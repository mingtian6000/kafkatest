```
你是一个智能日志分析师。以下是用户在过去两个月内与 AI 编程助手（如 GitHub Copilot 或 IntelliJ IDEA 内置 AI）的对话记录。

请执行以下任务：
1. 列出用户提出的所有问题或请求（每条一行）。
2. 将每个问题标记为以下两类之一：
   - 【操作记录】：描述用户正在做什么、遇到了什么问题、需要什么功能（例如：“如何合并分支”、“怎样配置 Cloud Run”）。
   - 【知识要点】：包含具体可执行的命令、API 调用、配置代码、快捷键等可以直接复用的技术细节（例如：“git merge --squash”、“gcloud run deploy --image=...”）。

输出格式：
- 每行一个条目，格式：[类别] 问题原文（如果涉及命令，请将命令用 `` 括起来）
- 最后统计【操作记录】和【知识要点】各自的数量。
 

以下是上一步提取出的【知识要点】列表。请将它们整理成一份结构化的 “Know-How 速查手册”。

要求：
- 按主题分组（例如 Git 命令、Cloud Run、Docker、快捷键等）。
- 每个条目包含：
  - 场景描述（一句话说明什么时候用这个命令）
  - 命令/代码块（精确可复制）
  - 关键参数说明（如有）
- 去除重复项，保留最完整版本。
- 如果某个知识点有常见坑点，用 ⚠️ 注明。

输出格式示例：
## Git
### 合并 squash 提交
- 场景：将多个 commit 压缩成一个后再合并到主分支。
- 命令：`git merge --squash feature-branch`
- 注意：⚠️ 之后需手动 commit。

## Cloud Run
### 部署服务
- 场景：部署容器化应用到 Google Cloud Run。
- 命令：`gcloud run deploy my-service --image gcr.io/my-project/my-image:tag --region us-central1`
- 参数说明：`--platform managed`（默认托管平台），`--allow-unauthenticated`（允许公开访问）。
