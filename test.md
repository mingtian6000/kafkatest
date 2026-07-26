```
好问题。先把一个关键认知摆正：instruction 文件是"软引导"，不是"硬拦截器"。它会自动并入每次 Copilot Chat 的提示里，指导模型的行为，但它没有强制力——模型在长上下文、用户显式要求等情况下可能不严格遵守。所以真正的安全边界要靠"instruction + 工具级权限 + 组织策略 + CI 门禁"四层一起。

下面我分两部分给你：先讲怎么写才有效，再给你一份直接可复制到 
".github/copilot-instructions.md" 的公司级模板。

一、有效 instruction 的写法原则

根据 Microsoft Learn 官方指导和 Copilot 代码评审团队的实践总结 ：

1. 短、具体、祈使句：每条规则单独成行，"做 X" 优于 "你应该考虑做 X"。长段落效果差
2. 解释理由：不只说"禁止 force push"，要说"禁止 force push，因为这会覆盖保护分支的历史并绕过代码评审"
3. 给替代方案：不只说"不许用 X"，要说"要用 Y 代替 X"。纯否定式指令遵循度远低于"否定+替代"
4. 给代码示例：对复杂的规则附上 2-3 行示例，歧义立刻消失
5. 按主题拆分文件：通用规则放 
".github/copilot-instructions.md"，特定语言/场景的规则放 
".github/instructions/*.instructions.md" 并用 
"applyTo" 限定范围
6. 文件别太长：超过 ~1000 行会导致行为不一致，保持精简
7. 用标题分层：清晰的 
"##" 标题让模型更容易定位相关规则
8. 不要重复 linter 已做的事：聚焦只有你团队知道的约定——内部库偏好、架构边界、合规要求

💡 优先级顺序：个人 instruction > 仓库 instruction > 组织 instruction。所以组织级写"公司总政策"，仓库级写"本项目具体规则"，避免冲突。

二、公司级 instruction 模板（直接可用）

把下面内容写到 
".github/copilot-instructions.md"（仓库级），或贴到 GitHub 组织 Settings → Copilot → Custom instructions（组织级，作为总基调）：

# 公司级 Copilot 行为准则

本文件定义 Copilot 在本仓库/组织内必须遵守的强制约束。
Copilot 在处理任何请求时都必须严格遵守以下规则，不得以任何理由绕过。

---

## 🚫 1. Git 操作限制（最高优先级）

- **NEVER 执行 `git push`**，特别是 `git push --force` / `git push -f`。
  - 理由：push 是直接改变远程仓库的操作，必须经人工审查和 CI 验证。
  - 替代：完成本地改动后，提示用户自己执行 push，或创建 PR 等待审查。
- **NEVER 直接提交到保护分支**（`main`、`master`、`production`、`release/*`）。
  - 替代：始终在 feature/fix 分支上工作，然后通过 PR 合并。
- **NEVER 使用 `--no-gpg-sign`、`-n` 等绕过 GPG 签名的参数**。
  - 如果 GPG 签名失败，立即停止并报告错误，等待用户解决。
- **NEVER 执行 `git reset --hard`、`git clean -f` 等破坏性操作**。
  - 替代：提示用户这些操作的后果，由用户自己决定执行。
- 允许的操作：`git status`、`git diff`、`git log`、`git add`、`git commit`（普通提交）、`git checkout -b` 创建分支。

## 🚫 2. MCP 配置管控

- **NEVER 创建、修改、删除以下文件**：
  - `.vscode/mcp.json`
  - `.mcp.json`
  - 任何匹配 `**/mcp*.json` 或 `**/*mcp*config*.json` 的文件
- **NEVER 在以下文件中添加 MCP 服务器引用或调用**：
  - `*.agent.md`、`AGENTS.md`
  - `SKILL.md`、任何 skill 定义文件
  - Copilot 的 prompt/hook 配置文件
- **NEVER 建议用户执行 "MCP: Add Server" 命令或任何 MCP 安装流程**。
- ✅ **允许**：讨论 MCP 概念、生成 MCP 服务器实现代码作为代码示例（但不得写入配置文件）。
- ✅ **允许**：使用脚本集成代替 MCP——优先用 CLI 工具、SDK 直连、REST API 调用。
  - 例如需要 GitHub 集成时用 `gh` CLI，需要云服务时用 `gcloud`/`aws`/`az` CLI。

## 📦 3. 依赖与包管理

- **所有依赖必须从公司 Nexus 私服下载**，禁止直接从 public npm/pip/Maven Central 下载。
- 安装前必须先配置私服源：

bash

npm

npm config set registry https://nexus.your-company.com/repository/npm-group/

pip

pip config set global.index-url https://nexus.your-company.com/repository/pypi/simple/

Maven (pom.xml 中)

<repository><url>https://nexus.your-company.com/repository/maven-group/</url></repository>

- **NEVER 在 `package.json` / `requirements.txt` / `pom.xml` 中硬编码 public registry URL**。
- 安装依赖时必须显式指定私服：

bash

npm install <pkg> --registry https://nexus.your-company.com/repository/npm-group/

- 新增依赖前，先检查 Nexus 上是否存在该包；若不存在，提示用户走私服代理申请流程。

## 🔒 4. 安全与合规

- **NEVER 在代码中硬编码密钥、密码、token、API key**。
- 替代：使用环境变量、Secret Manager、或公司的 Vault 服务。
- 变量命名规范：`*_API_KEY`、`*_SECRET`、`*_TOKEN`、`*_PASSWORD`
- **NEVER 提交 `.env`、`.key`、`.pem`、`credentials.json` 等敏感文件**。
- 确保这些已在 `.gitignore` 中。
- 所有用户输入在 API 边界必须验证；数据库访问必须使用参数化查询，禁止字符串拼接 SQL。
- 发现代码中存在硬编码密钥时，必须立即指出并要求改为环境变量引用。

## 🌐 5. 网络与外发限制

- **NEVER 建议直接访问公网 API 端点**，所有外部调用必须通过公司 API 网关。
- 禁止访问 `*.googleapis.com`、`*.amazonaws.com` 等公网端点（除非经 Gateway 代理）。
- 外部 HTTP 调用必须使用公司 SDK，该 SDK 自动注入认证头和审计日志。

## 🔀 6. 代码评审与 PR 流程

- 完成代码改动后，Copilot 必须：
1. 确保本地通过所有 lint 和 unit test
2. 创建 feature 分支并提交
3. 提示用户创建 PR，**NEVER 自行合并 PR**
4. PR 描述必须包含：变更目的、影响范围、测试情况、回滚方案
- 等待 CI/CD 检查通过和 required reviewers 批准后方可合并。

## 🛠 7. 工具使用权限（针对 Copilot CLI）

如果在 CLI 环境下使用，建议启动时附加权限限制：

bash

copilot --deny-tool='shell(git push)' \

--deny-tool='shell(git push --force)' \

--deny-tool=write \

--allow-tool='shell(git:status)' \

--allow-tool='shell(git:diff)' \

--allow-tool='shell(git:log)'

> 注：`--deny-tool` 优先级高于 `--allow-tool`，即使开了 `--allow-all-tools` 也会生效。

## 📋 8. 通用编码规范

- 语言：优先使用 TypeScript 而非 JavaScript；优先使用 Python 3.11+
- 命名：camelCase 变量/函数，PascalCase 类名，UPPER_SNAKE_CASE 常量
- 错误处理：不得吞掉异常；必须 logged 或显式 re-throw
- 注释：公共 API 必须有文档注释；复杂逻辑必须有 inline 注释说明"为什么"而非"是什么"

---

## 违反以上规则时的行为

当 Copilot 发现自己即将违反上述任何规则时：
1. **立即停止**该操作
2. 向用户明确报告："⚠️ 此操作违反组织策略：[具体规则]"
3. 提供符合政策的替代方案
4. 等待用户显式确认后才可继续执行非禁止类操作

三、为什么光有 instruction 不够——必须的兜底

instruction 是"软引导"，下面这些是"硬边界"，缺一不可：

1. Copilot CLI 工具级拒绝（最有效）

如果你用 Copilot CLI，启动时必须加拒绝参数 ：

copilot --deny-tool='shell(git push)' --deny-tool='shell(git push --force)' --deny-tool=write

"--deny-tool" 优先级高于 
"--allow-tool" 和 
"--allow-all-tools"，即使模型想作恶也会被物理拦截 。

2. 组织策略层（GitHub.com 管理后台）

- 禁用 Copilot CLI（如果业务允许）
- MCP 服务器白名单：只允许组织批准的 MCP server
- Coding Agent 不配置任何 MCP
- 分支规则集：保护分支禁止 force push、要求 PR 审查、要求状态码检查通过

3. Git 钩子兜底

在仓库里装 pre-push 钩子（就是我们之前聊的方案）：

#!/bin/bash
# .git/hooks/pre-push
echo "🔍 Pre-push policy check..."
# 检查即将推送的提交是否包含密钥
gitleaks protect --staged --redact
if [ $? -ne 0 ]; then
  echo "❌ Secrets detected! Push blocked by policy."
  exit 1
fi

4. CI 门禁

在 GitHub Actions / 公司 CI 里跑：

- 依赖审计（检查是否从私服下载）
- Secret 扫描（gitleaks）
- MCP 配置文件变更检测（有变更则 fail the build）

四、给你的落地建议