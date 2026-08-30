```
你这个现象的本质是：autoscaling 的触发指标选错了。MIG 看到 CPU > 80% 就扩出第 4 台 VM，但这台新 VM 上没有 Airflow Task 可跑——因为 Airflow 的 Task 不是按"CPU 空闲"分配的，而是由 Scheduler → Celery 队列 → Worker 拉取这条链决定的。新 VM 如果没有作为 Worker 正确加入、或者没有监听同一个 broker/队列，它就永远分不到 Task，CPU 自然是 0。

下面分两层帮你拆。

第一层：先搞清楚"CPU 80%"是谁的 CPU

MIG 整机组扩容会把 Web Server、Scheduler、Worker 混在一起看。但你提到"task 没有分到它上面"——这说明你扩出来的第 4 台机器要么是纯 Web Server、要么是 Worker 但没正确加入集群。

Airflow 各组件的扩容逻辑完全不同 ：

组件 真正的瓶颈信号 正确的扩容指标
Web Server UI/API 请求量、并发连接数 HTTP 请求率、活跃连接数
Worker Celery 队列里堆积的 task 数 Queued + Running task 数
Scheduler DAG 解析延迟、调度吞吐 DAG parsing time、scheduler heartbeat

⚠️ 用整机 CPU 80% 触发扩容，最大的问题就是：CPU 高可能是 Web Server 在反序列化大量 DAG 导致的，但扩出来的新 VM 如果不跑 Worker，就永远分不到 Task。
第二层：如果你扩的是 Worker 节点，为什么新 Worker 分不到 Task

即使第 4 台 VM 确实是 Worker，也可能因为以下几个原因空转：

1. 
"worker_concurrency" 设太高，老 Worker 把活全抢光了

这是最常见的原因。Managed Airflow / Composer 的扩容算法本质是 ：

目标 Worker 数 = ceil((Running Tasks + Queued Tasks) / worker_concurrency)

如果你 
"worker_concurrency=100"，3 台老 Worker 一下子就能吞掉 300 个 task，队列瞬间清空，autoscaler 看到队列空了就不继续扩，而且即使扩了第 4 台，也没 task 可分 。

改法：把 
"worker_concurrency" 降到合理值（1 CPU ≈ 12 并发 task ，你可以按 CPU 数算），让队列保持一定深度，新 Worker 才有活干。

2. 
"core.parallelism" 限制了全局并发 Task 数

"core.parallelism" 是整个 Airflow 安装中能同时运行的 Task 实例上限 。如果这个值偏小，老 Worker 就已经把并发额度用满了，新 Worker 起来也是干等。

改法：按 
"parallelism ≥ 期望并发 task 数" 来调，且要大于 
"worker_concurrency × worker 数"。

3. 新 VM 上的 Worker 没监听正确的队列

Celery 的 task 是按 queue 分发的。默认 queue 是 
"default_queue" 。如果你的 DAG 里 task 指定了特定 queue，而新 Worker 启动时没加 
"-Q queue_name" 参数，它就只监听 default，分不到那些 task。

改法：新 Worker 启动命令要带上正确的队列：

airflow celery worker -Q default,my_queue

4. Pool slots 耗尽

Admin → Pools 里如果 default_pool 的 slots 被占满，多余的 task 会卡在 queued 状态，新 Worker 即使空闲也接不到活 。

推荐的改造方案

方案 A：把 Web Server 和 Worker 拆开扩容（推荐）

MIG 整机扩容在这类场景下基本是无效的，因为 Airflow 是有状态、角色分离的系统。正确做法是：

1. Web Server 层：如果真的是 Web Server CPU 高（UI/API 压力大），用 GCP 的 Instance Group + HTTP Load Balancer，按 HTTP 请求率或 CPU 扩容 Web Server 副本，前面挂 LB
2. Worker 层：单独建一个 MIG 或 GKE node pool 专门跑 Worker，按 Celery 队列深度触发扩容，而不是按 CPU

方案 B：迁移到 GKE + KEDA（长期最优）

TRM Labs 的工程团队就是从 Composer 迁到 GKE 后，用 KEDA 基于 Celery 队列深度做 Worker 的 HPA ——这是目前社区验证过的最合理方案。KEDA scaler 直接读 Celery queue length，做到：

- 队列堆积 → 扩 Worker
- 队列清空 → 缩 Worker
- 新 Worker 起来必然有活干，不会空转

大致配置方向：

# KEDA ScaledObject 核心逻辑
triggers:
- type: celery
  metadata:
    queueLength: "30"  # 每 30 个待处理 task 扩 1 个 Worker
    queue: "default"

方案 C：如果坚持 MIG，至少改这两个地方

如果短期没法大改架构，至少把 MIG 的扩容策略调整一下：

1. 扩容指标改成基于 Celery 队列长度（如果 GCP 监控能拿到这个 metric），而不是 CPU 80%
2. 新 VM 启动脚本里强制 join 到 Worker 集群，并且：
   - 设置合理的 
"worker_concurrency"（别太高）
   - 监听所有需要的队列
   - 启动后主动 
"celery list-workers" 确认注册成功

一个快速自检清单

在你动手改之前，先跑这几个命令确认根因：

# 1. 看 Worker 有没有真的加入集群
airflow celery list-workers

# 2. 看队列里 task 情况
airflow tasks states-for-dag-run <dag_id> <execution_date>

# 3. 看并发配置
airflow config get-value celery worker_concurrency
airflow config get-value core parallelism

# 4. 看 pools 使用
airflow pools list

如果 
"celery list-workers" 里看不到第 4 台 VM，那就是 Worker 没注册成功，跟 CPU 指标无关，要先解决 join 问题。如果能看到但 task 不分给它，那就是 
"worker_concurrency" 或 
"parallelism" 的锅。

💡 一句话定性：你现在的 AutoScaler 基本是白加的——因为 Airflow Worker 的扩容不应该由 CPU 驱动，而应该由 Celery 队列深度 驱动。CPU 高只是"结果"，不是"原因"。
你方便透露一下两点吗：

1. 你这 4 台 VM 上分别跑哪些 Airflow 组件？（是 4 台都是 all-in-one，还是 Web/Scheduler/Worker 分开的）
2. Executor 用的是 Celery 还是其他的？

知道这两点我能给你更具体的配置改法。