```
啊对，是我刚才想岔了——15 个项目 = 15 个 GKE 集群，每套自己装 Prometheus + Grafana，那确实没法统一看，也不该强求统一（网络打通、跨项目 IAM、数据留存策略都不一样，硬统一反而维护炸）。

那回到你的问题：单套 Grafana 在自己集群里，应该做哪些 Dashboard 才能比 GCM 多拿到 insight？ 这才是正经方向。

先给结论：GCM 有但 Grafana 不该重复的，直接跳过

GCM 开箱就有的这些，Grafana 里别再花时间做，做了就是重复劳动：

- 集群 / 节点级 CPU、内存、磁盘、网络（GCM 的 GKE 内置看板比你还细，还带 GCP 项目级聚合）
- GKE Control Plane 可用性、API Server 延迟那些（GCM 原生采，Prometheus 采不到这么干净）

Grafana 这边要做的，是GCM 拿不到 / 拿不全 / 拿不灵活的那几层。

🟥 第一层：KSM（kube-state-metrics）—— 这是 GCM 最大的盲区

GCM 看 GKE 是"资源视角"（CPU 用了多少），KSM 是"K8s 对象视角"（Deployment 现在几个 ready、Pod 为啥 Pending）。这两套语义完全不同，GCM 几乎不暴露 KSM 指标。

每套 Grafana 必做一张 K8s Workload Health Dashboard，核心面板：

面板 PromQL 思路
Deployment ready 比例 
"kube_deployment_status_replicas_ready / kube_deployment_spec_replicas" by (namespace, deployment)
Pod Restart 突增（比 CPU 报警早） 
"increase(kube_pod_container_status_restarts_total[15m]) > 0"
OOMKilled 计数 
"kube_pod_container_status_terminated_reason{reason="OOMKilled"}"
Pending Pod + 原因 
"kube_pod_status_scheduled{condition="false"}" + 
"kube_pod_status_scheduled_reason"
HPA 当前 replicas vs target 
"kube_hpa_status_current_replicas" vs 
"kube_hpa_spec_max_replicas"（看是不是卡 ceiling）
镜像拉取失败 `kube_pod_container_status_waiting_reason{reason="ImagePullBackOff

💡 这类 Dashboard 的价值：早上线 5 分钟发现"哎这个 Deployment 的 ready 一直是 0/3，但 CPU 没报警"——GCM 不会告诉你这个。
现成 ID 直接 import：
"15757"（Kubernetes Views）或 
"6417"（Deployment 细粒度），改改 namespace 过滤就行。

🟥 第二层：cAdvisor 容器级 —— GCM 只到 Pod 级

GCM 的 Pod 指标是 aggregate 过的，但 cAdvisor 能到 container 级，同一个 Pod 里 sidecar vs 业务容器的 CPU/内存可以分开看。

重点面板：

- 每个容器 
"container_memory_working_set_bytes"（比 RSS 准，OOM 判据）
- 
"container_cpu_cfs_throttled_seconds_total"（CPU throttle，GCM 完全没有，但业务延迟的根因经常在这）
- 容器内 
"/sys/fs/cgroup" 的 limit 命中情况

这块 GCM 是真没有等效，Prometheus 捡漏。

🟥 第三层：应用 RED（如果你的 Deployment 暴露 /metrics）

这一层 GCM 除非你接了 Cloud Trace + 自己吐 Custom Metrics，否则完全没有。而 Prometheus 这边只要业务方加几行 
"prometheus-client" 库就能出。

每张 RED Dashboard 按 namespace / Deployment 拆：

- Rate：
"rate(http_requests_total[5m])" by (svc, route)
- Errors：
"rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m])"
- Duration：P95 
"histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))"

如果你 15 个项目里有些还没暴露 /metrics，可以优先推核心业务那几个先上，Grafana 这边 RED 面板一挂，比 GCM 的"Pod CPU 80%"有用 10 倍——CPU 80% 不代表用户慢，P95 涨了才是。

🟥 第四层：资源利用率（裁成本的视角）

GCM 的 GKE 看板能看"用了多少"，但看不出"request 给多了多少"，这块 PromQL 很香：

# CPU 利用率 = usage / request
sum(rate(container_cpu_usage_seconds_total{container!="POD"}[5m])) by (namespace)
/
sum(kube_pod_container_resource_requests{resource="cpu"}) by (namespace)

内存同理。出来一张表，哪个 namespace request 给超了一眼看到，反推裁 request / limit。15 套 Grafana 每套自己看自己的，贴合各项目预算归属。

🟥 第五层：按你的 market / stream / env 标签做过滤

你之前提过项目按 market、stream、env（SIT/UAT/Prod...）分类。Prometheus 这边 kube_pod_labels 能把 Pod 的 label 全抓下来，所以可以在 Grafana Dashboard 顶部加 variable 下拉：

- 
"market" ← 从 
"kube_pod_labels" 里正则提（如果你 Pod / Namespace 打了 
"market=APAC" 这类 label）
- 
"stream" ← 同理
- 
"env" ← namespace 名正则或 label 提

配完之后，一张 Dashboard 在单集群内也能按你的业务维度切，比 GCM 硬按 GKE 集群 / namespace 筛要顺。

⚠️ 前提：你 15 个集群的 label 规范要统一，不然这套 variable 每边改起来烦。建议 label 命名 
"market"、
"stream"、
"env" 全局约定死。

🟦 15 套 Grafana 的维护痛点（顺便提一句）

虽然你不统一看，但15 套 Dashboard JSON 手动维护还是会疯的，建议两点：

1. Dashboard 走 Git + Provisioning：每套 Grafana 起 
"provisioning/dashboards" 目录挂 Git，Dashboard JSON 版本化管理，改一版 push，15 套 pull 更新。比 UI 里手改稳。
2. Datasource 统一叫 
"Prometheus"（默认名），这样同一份 JSON 在 15 套都能 import 不报错，不用改 UID。

如果你哪天嫌 15 套 Grafana 版本漂移（A 集群 Dashboard v3、B 集群还是 v1），可以上一套 Grafana Operator（跑在随便一个管控集群里，CRD 定义 Dashboard，sync 到 15 套）——不过你目前规模手动 Git + Provisioning 够用了。

要不要我挑一张具体的 Dashboard ID（比如 
"7249" 或 
"15757"），按你"market / stream / env"的 label 结构告诉你 variable 怎么配成三级下拉？单集群内按业务维度切，这个配完比 GCM 原生看板顺很多。