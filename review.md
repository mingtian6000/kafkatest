```
Kiro 本身没有一个开箱即用的"trigger Jenkins → 看 log → 改代码 → 自动重试"黑盒开关，但你要的这种 agent loop + 超时重试 + 读失败 log 自我迭代 完全可以用 Kiro 现有的几层机制拼出来。关键是分清：哪一层做"循环"，哪一层做"触发"，哪一层做"读 log 修代码"。

Kiro 原生有什么、没有什么

有的：

- Agent Loop 本体：Kiro 在 Autopilot / Spec 执行模式下，本来就是"规划→调工具→看结果→继续"的循环，不是一次性回答。
- Hooks（事件触发）：
"PostToolUse" / 
"PostTaskExec" / 
"PreTaskExec" 能在工具或 spec 任务跑完时触发 shell 或 agent prompt，每个 hook 自带 
"timeout"（秒）。
- Shell action 退出码：command 类型 hook 退出非 0，Kiro 会把 stderr 喂回 LLM；
"PreToolUse" 退出 2 可阻断。
- MCP / Powers：可以接 Jenkins MCP 或自己写脚本调 Jenkins REST API（
"/job/.../lastBuild/logText/progressiveText"）。
- Subagents / Delegate：可以把"分析 Jenkins log"丢给子 agent 在后台跑。

没有的（重要）：

- 没有内建的"retry N 次带退避"原语（不像 Jenkins 的 
"retry(3)"）。
- Hook 本身不是循环体，它是"事件→一次动作"。要不要再试一次，得由主 agent 的 loop 决定，或者你在 shell 脚本里自己写 
"for i in 1..N"。
- Hook 里 
"type: agent" 只是往上下文塞一段 prompt，不会自己开一个新循环。

所以"Pipeline fail → 读 log → 改代码 → 重跑"这个闭环，正确拼法是：主 agent（或 spec task）持有循环，Hook 做触发和反馈，shell 脚本做轮询/超时，Skill 封装修复经验。

推荐架构：三层拼一个自愈 loop

1）Skill 封装"怎么修 Config Two / 编译错误"

".kiro/skills/jenkins-self-heal/SKILL.md"：

---
name: jenkins-self-heal
description: >-
  When a Jenkins pipeline fails, fetch the failing log, classify the error
  (compile / test / config-two schema / timeout), patch the repo accordingly,
  and re-trigger the pipeline. Activates when user says "rerun CI", "fix build",
  or after a PostTaskExec hook reports Jenkins FAILURE.
---
## Steps
1. curl Jenkins API for lastBuild result + logText
2. Classify: grep for `BUILD FAILURE`, `ConfigTwoException`, `npm ERR`, `Timeout`
3. Apply known fix from references/fix-patterns.md
4. Edit file, git commit -m "ci: auto-fix <class>"
5. Trigger rebuild via `curl -X POST .../build`
6. Poll up to 10 min; if still red, loop max 3 times then stop and report

把"哪类错改哪个文件"写进 
"references/fix-patterns.md"，这就是你从 agent.md 转过来的核心知识。

2）Hook 做"任务结束自动查 CI"

".kiro/hooks/ci-watch.json"：

{
  "version": "v1",
  "hooks": [{
    "name": "jenkins-after-task",
    "trigger": "PostTaskExec",
    "action": {
      "type": "command",
      "command": "bash .kiro/scripts/jenkins_check.sh"
    },
    "timeout": 600,
    "enabled": true
  }]
}

"jenkins_check.sh" 里干这些事：

- 调 Jenkins API 拿最近一次 build 状态
- 如果 SUCCESS → exit 0，什么都不发生
- 如果 FAILURE → 把 log 落到 
".kiro/ci-last.log"，exit 0 但 stdout 打印 
""Jenkins FAILURE, log at .kiro/ci-last.log, invoke /jenkins-self-heal""（Kiro 会把 stdout 加进上下文，主 agent 看到后主动调 skill）
- 超时轮询写在脚本里（
"for i in {1..20}; sleep 30"），脚本级超时由 hook 的 
"timeout: 600" 兜住

为什么不让 hook 直接调 agent 修代码？因为 
"type: agent" 的 hook 只是注入 prompt，不保证主 agent 真的进入"改完再跑"的循环；让主 agent 在拿到 stdout 后自己走 skill 的 loop 更可控。
3）主 agent / Autopilot 持有 retry 循环

你在 Kiro 里开 Autopilot 或给 spec task 下指令：

"实现这个 task，每次 PostTaskExec 后如果 Jenkins 红，按 jenkins-self-heal 修，最多 3 轮，还红就停。"
Kiro 的 agent loop 会：

- 写代码 → 提交 → hook 触发 
"jenkins_check.sh" → 发现红 → stdout 提示
- 下一轮 agent 读到提示 → 调 
"jenkins-self-heal" skill → 改代码 → 再提交
- 直到绿或到 3 轮上限

这就是你要的"超时重试 + 看 log 迭代"，循环由 agent 对话轮次提供，超时由 shell+hook timeout 提供，修代码由 skill 提供。

几种可行度对比

方案 做法 自治程度 评价
纯 Hook 
"type:agent" 失败后塞 prompt 让 agent 修 中 简单但循环弱，agent 可能一轮就停
Hook+Shell 轮询+主 agent skill 上述三层 高 推荐，最接近你说的自愈 loop
把重试全写进 Jenkinsfile 
"retry(3)+timeout" 在 Jenkins 侧 低（只重试构建，不改代码） 适合 flaky 测试，不适合"改代码" 
外部 cron/python 跑 CI 自愈调 Kiro CLI 完全脱离 hook 高 重，但适合服务端无人值守

两个容易踩的坑

- Hook 无限递归：
"PostFileSave" 触发 agent 改文件 → 又存盘 → 再触发。Kiro 对同 hook 嵌套会跳过，但跨 hook 链（save→agent→save）可能转。建议 CI 检查挂 
"PostTaskExec" 而不是 
"PostFileSave"，或用 matcher 排除 CI 脚本自身。
- 超时单位：hook 的 
"timeout" 是秒，默认 60；你等 Jenkins 10 分钟要写 
"600"，脚本内部再自己 sleep 轮询，别指望 Kiro 自动退避。

一句话定性

Kiro 没有"内置 Jenkins 自愈 agent loop"按钮，但它给齐了积木：agent 主循环（Autopilot/Spec）+ Hook（事件/超时）+ Shell（轮询 Jenkins API）+ Skill（修代码知识）+ MCP（可选接 Jenkins）。你前面那个 IDEA 里"Config Two 保存时改键名"的需求，升级成"CI 红了自动修 Config Two 再跑"——就是把这个 Skill 从文件事件挂到 
"PostTaskExec" + 
"jenkins-self-heal" 上就行。

如果你愿意，我可以帮你把上面那个 
"jenkins_check.sh" 和 
"jenkins-self-heal/SKILL.md" 按"Config Two yaml 校验失败"这一具体场景填实，你直接丢进 
".kiro/" 就能跑。