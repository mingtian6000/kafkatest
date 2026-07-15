```
你这个体感其实半对半错——得先把"cmd 多开"和"gcloud configuration"这两层拆开看，不然容易被绕进去。

🔍 先纠一个误区：Windows 的 cmd / WT 多开，不是共用 session

Windows 下不管是老 cmd、
"powershell"、
"pwsh"，还是 Windows Terminal 新开一个 tab，每个窗口/标签页都是独立进程，环境变量各自一份，互相不串。你在 Tab A 里 
"set MYVAR=foo"，Tab B 里 
"echo %MYVAR%" 是空的。

所以"共用一个 configuration"的锅不在 terminal，在 gcloud 那头。

🎯 那"共用"感从哪来？——
"gcloud config configurations activate" 是全局的

gcloud 的两套切换姿势行为完全不同：

方式 作用域 会不会串

"gcloud config configurations activate proj-a" 写 
"%APPDATA%\gcloud\active_config" 文件（全局共享） ❌ 会串——Tab A activate 了，Tab B 跟着变

"set CLOUDSDK_ACTIVE_CONFIG_NAME=proj-a"（cmd）/ 
"$env:CLOUDSDK_ACTIVE_CONFIG_NAME="proj-a""（PS） 当前进程的环境变量 ✅ 不串——每个 tab 独立

你之前觉得"共用"，大概率是因为用的是 
"activate" 那条命令——它改的是 gcloud 全局状态文件，所有 terminal 都读同一份，所以才会"一个 activate 全机跟着变"。

正确姿势是用 env var，不是用 activate。

✅ Windows 下落地方案（不开 Docker、不碰 WSL）

方案一：
"CLOUDSDK_ACTIVE_CONFIG_NAME"（轻量，够用）

:: Tab 1（cmd）
gcloud config configurations create proj-a
gcloud config configurations create proj-b
:: 先建好，下面 per-tab 绑

set CLOUDSDK_ACTIVE_CONFIG_NAME=proj-a
gcloud auth login
gcloud config set project my-proj-a

:: Tab 2（另一个 cmd）
set CLOUDSDK_ACTIVE_CONFIG_NAME=proj-b
gcloud auth login
gcloud config set project my-proj-b

PowerShell 写法：

$env:CLOUDSDK_ACTIVE_CONFIG_NAME="proj-a"

两个 tab 各跑各的 project / account，互不串，因为 configurations 的账号列表是全机共享的，但"哪个是 active"由 env var 控制，per-process。

⚠️ 局限：ADC 文件（
"application_default_credentials.json"）还在 
"%APPDATA%\gcloud" 里共享，如果 proj-a 和 proj-b 用不同 ADC，还得往下看方案二。

方案二：
"CLOUDSDK_CONFIG" 暴力隔离（推荐，100% 不串）

每个 terminal 指一个独立 config 目录，credentials / configurations / ADC / access token 全隔离：

:: Tab 1
set CLOUDSDK_CONFIG=%APPDATA%\gcloud-proj-a
gcloud auth login
gcloud auth application-default login
gcloud config set project my-proj-a

:: Tab 2
set CLOUDSDK_CONFIG=%APPDATA%\gcloud-proj-b
gcloud auth login
gcloud auth application-default login
gcloud config set project my-proj-b

Windows 下 gcloud 默认目录是 
"%APPDATA%\gcloud"（Linux/Mac 是 
"~/.config/gcloud"），
"CLOUDSDK_CONFIG" 一改，连 ADC 都隔离了，两个 tab 完全看不见对方。

💡 第一次进每个目录要重新 
"gcloud auth login" + 
"gcloud auth application-default login"，之后就持久化了。

方案三：顺手自动化（不用每次手打 set）

Windows Terminal 的 profile 预设 env（最省）：

WT 的 
"settings.json" 里给不同项目建不同 profile，每个 profile 加：

{
  "name": "gcp-proj-a",
  "commandline": "cmd.exe",
  "environment": [
    "CLOUDSDK_CONFIG=%APPDATA%\\gcloud-proj-a"
  ]
}

以后开就是绑定好的，点一下就行。

PowerShell 
"$PROFILE" 按目录切：

如果你习惯 cd 到项目目录再干活，可以在 
"$PROFILE" 里加个钩子，按当前目录自动设：

switch ($PWD.Path) {
  "C:\code\proj-a" { $env:CLOUDSDK_CONFIG = "$env:APPDATA\gcloud-proj-a" }
  "C:\code\proj-b" { $env:CLOUDSDK_CONFIG = "$env:APPDATA\gcloud-proj-b" }
}

direnv-windows 也能用，但 Windows 上装起来比 Linux 麻烦点，不强制推荐。

🛡 真要"进程级隔离"但不开 Docker/WSL？

如果连"同一台机器上 gcloud 目录隔离"都满足不了（比如怕别的环境变量也串），Windows 原生还有两招，但都偏重：

- 
"runas" 不同 Windows 用户：每个用户 
"%APPDATA%" 天然分开，gcloud 配置自然隔离，但切换用户很烦
- Windows Sandbox：临时轻量 VM，用完即扔，适合跑一次脏活，不适合日常开发

一般 
"CLOUDSDK_CONFIG" 就够用了，不用走到这层。

✅ 给你的推荐组合

Windows + cmd/WT + 不开 Docker 不碰 WSL：
提前 
"gcloud config configurations create proj-a / proj-b" 建好，然后每个 WT tab 里 
"set CLOUDSDK_ACTIVE_CONFIG_NAME=proj-a"（轻度）或 
"set CLOUDSDK_CONFIG=%APPDATA%\gcloud-proj-a"（重度，ADC 也要隔离）。WT 的 profile 里把 env 写死，开 tab 即就绪。
要不要我帮你写一份 WT 
"settings.json" 的模板，proj-a / proj-b 两个 profile 直接抄？或者你现在是 cmd 原生多开不是 WT，那走 
"$PROFILE" 或 
".bat" 启动脚本更合适。