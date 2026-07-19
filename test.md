```
你看到的 
"~/.copilot/" 是 Copilot CLI / Agent 的用户级全局配置根目录（类似 
".gitconfig" 之于 git），所有仓库都能继承，不会被 commit 进项目。里面那几个文件夹不是随便起的名，每类对应一种定制维度——你混着说的 skill / instruction / agent 其实是三种不同东西，先帮你拆清楚再给例子。

"~/.copilot/" 下常见结构

~/.copilot/
├── copilot-instructions.md      # 全局"总纲"，每次会话必加载
├── instructions/                # 模块化 instruction 碎片（*.instructions.md）
├── skills/                      # 个人级 Agent Skills（跨项目共享）
├── agents/                      # 个人级自定义 Agent（@xxx 调用）
├── mcp-config.json              # 全局 MCP 服务器
└── lsp-config.json              # 全局 LSP 服务器

下面分三类说你问的那几个。

📝 instructions/ —— "常开的规矩"

定位：编码规范、build/lint/test 命令、commit 风格这类"每次对话都想让 Copilot 知道"的基础上下文。分两种：

- 根文件 
"copilot-instructions.md" → 必加载，像系统 prompt 补丁，建议 <100 行
- 模块化 
"instructions/*.instructions.md" → 可按话题拆，CLI 会自动发现加载；VS Code 侧主要在 
".github/instructions/"，但 CLI 也认 
"~/.copilot/instructions/" 做用户级

该塞什么：项目无关的个人信息，比如你全局偏好的语言风格、常用 build 命令别名。

例子 
"~/.copilot/instructions/code-style.instructions.md"：

---
description: "My global TS/JS coding preferences"
---

# Personal Coding Style
- TypeScript strict mode 必开
- 变量用 camelCase，组件用 PascalCase
- 导出的 function 必须有 JSDoc
- import 顺序：std → 第三方 → 本地，本地用 `@/` 别名
- 提交信息 follow conventional commits（feat/fix/chore/docs）

💡 仓库里的 
".github/copilot-instructions.md" 会覆盖同名的全局规则，团队约定走仓库、个人怪癖走 
"~/.copilot/"。

🧰 skills/ —— "按需加载的能力包"

定位：和 instructions 最大区别是只在 Copilot 判定相关时才加载进 context，不占每次对话的 token。是个 open standard（agentskills.io），VS Code Copilot、Copilot CLI、Copilot Cloud Agent 都认。

个人级路径就是 
"~/.copilot/skills/<skill-name>/SKILL.md"（跨所有项目生效，不进 git）。

该塞什么：带步骤、可带脚本/模板的"专项能力"，比如"帮我按公司格式发 PR""跑某套测试流程""调内部 CLI 工具"。

例子 
"~/.copilot/skills/pr-generator/SKILL.md"：

---
name: pr-generator
description: 生成符合团队规范的 PR 描述。当用户说"提 PR"/"summarize changes"/涉及 git push 时使用。
---

# PR 生成技能

1. 先 `git diff main...HEAD` 看变更范围
2. 按以下模板输出 PR description：

markdown

Summary

（一句话）

Changes

- 

Test Plan

- 

Checklist

- [ ] lint 通过
- [ ] 自测过


3. 标题用 conventional commits 格式

还可以往 
"pr-generator/" 里扔 
"template.md"、
"scripts/validate-title.sh" 这类资源，SKILL.md 里用 
"./xxx" 引用就行。

🤖 agents/ —— "角色化代理"

定位：
"instructions" 是"规矩"，
"skills" 是"工具包"，
"agents" 是"角色"——每个 agent 有自己的 prompt + tool 权限 + 上下文，聊天里用 
"@agent-name" 唤出。

个人级放 
"~/.copilot/agents/<name>.agent.md"，所有会话可用。

该塞什么：你反复要的某个角色，比如"专职 code reviewer""专职写 commit msg""专职转 SQL"。

例子 
"~/.copilot/agents/reviewer.agent.md"：

# Reviewer Agent

你是一个资深前端 reviewer，专注 React + TypeScript 项目。

## 关注点（按优先级）
1. 类型安全：any 不许出现，props 必须显式 type/interface
2. 性能：useEffect 依赖数组、不必要的 re-render
3. 可访问性：表单要有 label、图片要有 alt
4. 命名：component 用 PascalCase、hook 用 useXxx

## 输出格式
每条意见给三栏：位置 / 问题 / 建议改法

之后在 Copilot Chat 里 
"@reviewer" 就能切到这个角色，它会带自己的指令和工具集，跟普通聊天隔离。

一张表帮你选该往哪塞

你想做的事 放哪 加载方式
全局编码风格、build 命令、commit 规范 
"~/.copilot/instructions/*.instructions.md" 每次必带
专项 workflow（带脚本/模板，按需） 
"~/.copilot/skills/<x>/SKILL.md" 相关才加载
固定角色（@xxx 唤出） 
"~/.copilot/agents/<x>.agent.md" 
"@" 显式调用
跨项目都要的"总纲" 
"~/.copilot/copilot-instructions.md" 每次必带

⚠️ 团队共享的版本请放仓库里：instructions → 
".github/instructions/"，skills → 
".github/skills/"，agents → 
".github/agents/"。
"~/.copilot/" 这套只给自己用，别 commit。
如果你那底下还有 
"prompts/" 或 
"hooks/" 之类，那是仓库级 
".github/" 的常见配套（
"prompts/*.prompt.md" 是 
"/slash" 调用的模板，
"hooks/*.json" 是 agent 工作流里插的 shell），个人级 
"~/.copilot/" 目前官方主推就是上面四个文件夹 + 两个 json。