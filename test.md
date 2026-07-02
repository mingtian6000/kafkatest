```
GKE 1.35 这块你点名的 Liveness / Readiness Probe 正好踩中了一个关键行为变更，而且是个 breaking change——这个得先讲清楚，因为升级上去很可能 Pod 会莫名重启或掉流量。

🎯 GKE 1.35 在 Probe 这块的具体变化

核心点：
"exec" probe 的 
"timeoutSeconds" 在 1.35 之前是被忽略的，1.35 起开始强制生效。

上游 Kubernetes 其实早就规定 
"timeoutSeconds" 默认是 1 秒，但 GKE 在 1.34 及更早版本里，对 
"exec" probe（
"livenessProbe.exec"、
"readinessProbe.exec"、
"startupProbe.exec"）一直无视这个字段——哪怕你设了 
"timeoutSeconds: 5"，command 跑 10 秒也不会被判超时，probe 照样报成功。

1.35 开始对齐上游行为：

- 没写 
"timeoutSeconds" → 默认 1 秒，command 跑不完就判失败
- 写了但太短（比如 command 实际要 3 秒却只给 1 秒）→ 1.35 起会正确报失败
- 以前 1.34 即使出 error 也只是 warn event，command 本身还能跑完；1.35 起直接按失败走后续动作

失败后的后果差别：

Probe 类型 1.34 及之前 1.35 起
Liveness timeout 只出 warn，不重启 判失败 → 重启容器，可能 CrashLoopBackOff
Readiness timeout 只出 warn，Pod 仍在 Service endpoints 判失败 → 
"Ready=False"，Pod 从 endpoints 摘除
Startup 同上 同上

所以如果你现有 YAML 里用了 
"exec" probe 但没仔细配 
"timeoutSeconds"，或者 command 本身比较重（比如 
"pg_isready"、curl 外部、跑个脚本），升 1.35 前一定要先 audit 一遍，把 
"timeoutSeconds" 调到比 command 实际耗时宽松一点，否则半夜 Pod 循环重启找半天原因。

💡 顺带一提：这个"修复"本质是 GKE 追上游——上游 K8s 这边 
"exec" probe timeout 早就是生效的，GKE 之前是自己 patch 过放宽的，1.35 把这个 patch 撤掉了。所以严格说不算 GKE"增强"，而是"回归标准行为"。

更广一点：GKE 相对"标准上游 K8s"的增强在哪些方向

除了 probe 这块是追上游，GKE 大部分"增强"其实是托管 + 谷歌基础设施集成，分几个维度：

1. 节点 / 集群运维层（Standard K8s 没有或要自己搭）

- Autopilot 模式：节点完全由 Google 管，按 Pod 资源计费，自动调优规模，不用自己搞 node pool、patching、capacity planning
- Node Auto-Repair：节点不健康自动替换
- Node Auto-Provisioning：根据 Pending Pod 自动建/删 node pool
- Release channels + 自动升级，比上游 
"kubeadm upgrade" 省事

2. 网络

- GCP Cloud Load Balancer 跟 
"Service"/
"Ingress"/
"Gateway" 原生集成，Global LB、HTTPS/HTTP2/gRPC 都直接有
- VPC-native 集群（别名 IP），Pod IP 直接路由，不用 overlay
- Private Cluster、Private Service Connect

3. 安全（这块是 GKE 跟上游差距最大的）

- Workload Identity：Pod 直接绑 GCP Service Account，不用挂 key
- Shielded Nodes：UEFI Secure Boot、vTPM
- Binary Authorization：部署时镜像签名校验（上游要靠 Connaisseur 之类自己拼）
- GKE Sandbox（gVisor）：强隔离，跑不受信代码
- Policy Controller（OPA Gatekeeper 托管版）

4. AI / 大规模

- 单集群 65,000 节点（上游理论上限 5,000，实际多数发行版卡在几千）
- GPU / TPU 原生调度，跟 AI Hypercomputer 集成
- GKE Inference 那套：推理成本降 30%、尾延迟降 60%、吞吐 +40%（谷歌自己 benchmark）

5. 多集群

- Fleet（机群）+ Team 多租户管理
- Config Sync（GitOps 托管）
- Anthos 把本地/多云 K8s 也挂进同一个控制台

回到你的问题

如果你问 1.35 的动机是正在规划升级，那 probe 这块是最高优要自查的——搜一遍集群里所有带 
"exec" probe 的 workload，
"timeoutSeconds" 没设或设太短的先改掉再升。其余 GKE 的"增强"对你来说要不要关心，取决于你现在用的是 Autopilot 还是 Standard、有没有用到 GCP 特有的那些集成（Workload Identity、Cloud LB、Binary Auth 这类）。

你现在是 Standard 还是 Autopilot？如果是 Standard，node 层那堆 auto-repair / auto-provisioning 值得打开；Autopilot 的话基本不用管，主要看 probe 和安全那块就行。