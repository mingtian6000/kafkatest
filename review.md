```
好的，Agent Hooks 是目前 Copilot 提供的最硬的控制层——它是在 Agent 执行生命周期中插入自定义脚本的机制，具有确定性执行的特点，不像 instruction 那样只是"软引导"。下面我手把手教你怎么配置和使用，针对你的场景（阻止 Agent 写入 MCP JSON 文件）。

一、Agent Hooks 是什么

Agent Hooks 允许你在 Agent 的关键决策点（如即将执行工具前、工具执行后、对话结束时）运行自定义脚本，脚本可以根据返回值决定是否允许该操作继续。支持的 hook 点：

Hook 名称 触发时机 返回值影响

"PreToolUse" Agent 将要调用某个工具前 返回 
"true" 允许继续，
"false" 拒绝该次工具调用

"PostToolUse" 工具调用完成后 可用于记录日志或执行后处理

"OnSessionEnd" 会话结束时 清理资源等

对于你的需求，最合适的是 
"PreToolUse" ——在 Agent 即将执行文件写入操作时，检查目标路径是否为 MCP JSON 文件，若是则拒绝。

二、配置 Agent Hooks 的方法

前提条件

- 你需要 Copilot Business 或 Enterprise 订阅（个人版不支持）
- 当前 Copilot Agent Hooks 仍处于预览阶段，可能需要手动启用
- 支持的 IDE：VS Code 1.96+、Cursor、JetBrains（部分版本）

配置方式有两种

方式 A：通过 VS Code 设置（推荐，适用于个人/项目级别）

在 VS Code 的 
"settings.json" 中添加：

{
  "github.copilot.chat.agent.hooks": {
    "preToolUse": "/path/to/your/hook-script.sh"
  }
}

注意：路径可以是绝对路径，也可以是相对于项目根目录的相对路径（如 
".github/hooks/pre-tool-use.sh"）。
方式 B：通过项目级配置文件（适合团队共享）

在项目根目录创建 
".github/copilot-hooks.yml"：

version: 1
hooks:
  preToolUse:
    - command: bash
      args: [".github/hooks/pre-tool-use.sh"]

三、编写 Hook 脚本：拦截 MCP JSON 写入

下面是一个 Bash 脚本示例，它会在 Agent 每次调用工具前检查：如果工具是 
"writeFile" 或 
"editFile"，且目标文件匹配 MCP JSON 模式，则拒绝。

#!/bin/bash
# pre-tool-use.sh - 拒绝 Agent 写入 MCP JSON 配置文件

# 从环境变量中获取本次工具调用的详细信息
# Copilot 会注入以下环境变量：
#   COPILOT_TOOL_NAME: 工具名称（如 writeFile, editFile, searchFile 等）
#   COPILOT_TOOL_INPUT: 工具参数的 JSON 字符串（包含文件路径等）
#   COPILOT_SESSION_ID: 当前会话 ID

TOOL_NAME="${COPILOT_TOOL_NAME}"
TOOL_INPUT="${COPILOT_TOOL_INPUT}"

# 只拦截文件写入类工具
if [[ "$TOOL_NAME" == "writeFile" || "$TOOL_NAME" == "editFile" ]]; then
    # 提取文件路径（假设 TOOL_INPUT 是 JSON，可以用 jq 解析，但为了兼容性，用 grep 简单匹配）
    FILE_PATH=$(echo "$TOOL_INPUT" | grep -o '"filePath":"[^"]*"' | cut -d'"' -f4)
    
    # 检查是否匹配 MCP JSON 模式
    if echo "$FILE_PATH" | grep -qiE '(mcp\.json|\.vscode/mcp\.json|\.mcp\.json|mcp-config\.json)'; then
        echo "BLOCKED: Agent attempted to write to MCP configuration file ($FILE_PATH)"
        exit 1   # 非零退出表示拒绝该工具调用
    fi
fi

# 其他情况允许继续
exit 0

脚本放置位置

将上述脚本保存为 
".github/hooks/pre-tool-use.sh"，并赋予执行权限：

chmod +x .github/hooks/pre-tool-use.sh

如果不想用 Bash，也可以用 Python

#!/usr/bin/env python3
import os, json, sys

tool_name = os.environ.get("COPILOT_TOOL_NAME", "")
tool_input_str = os.environ.get("COPILOT_TOOL_INPUT", "")

if tool_name in ("writeFile", "editFile"):
    try:
        params = json.loads(tool_input_str)
        file_path = params.get("filePath", "")
        # 检查是否匹配 MCP JSON 模式
        if any(pattern in file_path.lower() for pattern in ["mcp.json", ".mcp.json", "mcp-config.json"]):
            print(f"BLOCKED: Attempted to write MCP config: {file_path}")
            sys.exit(1)
    except:
        pass

sys.exit(0)

四、验证 Hook 是否生效

1. 在 VS Code 中打开一个项目，确保 Copilot 已连接
2. 打开 Copilot Chat，切换到 Agent 模式
3. 向 Agent 提问："帮我创建一个 MCP 服务器配置文件"
4. 观察：Agent 可能会尝试写入 
".vscode/mcp.json"，此时 Hook 脚本会被触发，Agent 会收到错误，并在聊天窗口中显示类似以下信息：
⛔ Tool call rejected by pre-tool-use hook: writeFile on .vscode/mcp.json
5. 同时，VS Code 的输出面板（
"View → Output"，选择 "Copilot Agent Hooks"）会看到详细的日志

五、重要注意事项

1. Hook 脚本的权限：必须要有可执行权限（
"chmod +x"），否则 VS Code 会静默忽略
2. 环境变量：Copilot 注入的环境变量名称可能随版本变化，建议查看官方文档或输出面板的日志来确认实际变量名
3. 性能影响：Hook 脚本每次工具调用都会执行，尽量保持轻量（避免网络请求或复杂计算）
4. 仅阻止 Agent 自身的操作：用户手动创建文件、其他扩展写入的文件不受 Hook 控制
5. 跨平台兼容：Bash 脚本在 Windows 上需要 Git Bash 环境；如果团队有 Windows 用户，建议用 Python 或 Node.js 脚本
6. Hook 不是安全边界：恶意用户仍然可以绕过（比如直接修改 hook 脚本），但它能有效阻止 Agent 的意外行为

六、如果你还想更进一步：组合 instruction + hook

最佳实践是双层防护：

- Instruction 文件：告诉模型"不要写 MCP JSON"，减少它主动尝试的概率
- Agent Hooks：作为兜底，即使模型尝试了也会被物理拦截

这样既减少了 Agent 做无用功（浪费 token），又保证了策略强制执行。

七、如果 Hook 不生效怎么办？

- 检查 VS Code 版本是否 ≥ 1.96
- 检查 Copilot 订阅类型（Business/Enterprise）
- 在 VS Code 设置中搜索 
"github.copilot.chat.agent.hooks.enabled"，确保为 
"true"
- 查看输出面板（
"Copilot Agent Hooks"）是否有错误信息
- 尝试重启 VS Code

如果还是不行，可能你的 Copilot 版本尚未支持 Agent Hooks（仍在灰度中），此时可以暂时用 instruction + 组织策略的组合，等待功能全面开放。
