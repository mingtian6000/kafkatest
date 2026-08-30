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

你这个观察非常关键，而且它揭示了一个本质问题：对于 Airflow 来说，加 Worker 节点并不等于加吞吐量，尤其是面对长耗时的、IO 密集型的原子任务时。

我先帮你定性，再说怎么办。

为什么新 Worker 加入后还是分不到任务

你描述的"死磕一张表"的 BigQuery 任务，在 Airflow 里属于典型的同步算子占用 worker slot 的场景：

⚠️ 经典的同步算子在执行期间会一直占用 worker slot，即使真正的重活在 BigQuery 那边跑，Airflow 的 worker 也得陪着等 。一个 90 分钟的 BigQuery 查询，意味着这个 worker slot 被占用 90 分钟。
这就导致了你看到的局面：3 台老 Worker 的 slot 被这些"死磕"任务占满 → 任务队列可能已经空了（因为 
"parallelism" 限制了全局并发）→ 新 Worker 起来后无任务可拉 → CPU 接近 0。

还有一个隐藏杀手：
"worker_prefetch_multiplier"。Celery 默认会让 Worker 预取任务，如果调得过高，老 Worker 会把 broker 里的任务"囤"在自己内部队列里，新 Worker 根本看不到活 。

你现在这个 AutoScaler 为什么是白加的

MIG 基于 CPU > 80% 扩容的逻辑在这里完全失效，原因有两层：

1. CPU 高 ≠ 任务队列深。CPU 高是因为老 Worker 在等 BigQuery 返回结果时空转忙等（或者 Web Server 在反序列化大量 DAG），但任务队列可能是空的——因为全局并发额度（
"parallelism"）早就被占满了
2. 即使扩出新 Worker，没有新任务产生，因为 
"parallelism" 这道闸门没打开

Airflow 官方 Helm Chart 的 KEDA 扩缩容逻辑就很说明问题，它是基于：

SELECT ceil(COUNT(*)::decimal / worker_concurrency) 
FROM task_instance 
WHERE state='running' OR state='queued'

也就是按 running + queued 的任务数来扩，而不是按 CPU 。这才是 Airflow Worker 扩容的正确信号。

怎么破：分三步走

第一步：先确认卡点在哪（10 分钟定位）

在 Airflow 机器上跑：

# 1. 看 Worker 是不是真的都注册了
celery -A airflow.executors.celery_executor inspect active

# 2. 看任务到底卡在哪种状态
# 在元数据库查：
SELECT state, COUNT(*) FROM task_instance 
WHERE queue = 'default' 
GROUP BY state;

# 3. 看 prefetch 配置
airflow config get-value celery worker_prefetch_multiplier

判断：

- 如果大量任务处于 
"running" 状态 → 说明 slot 被长任务占满，新 Worker 没活干（你的场景大概率就是这个）
- 如果大量任务处于 
"queued" 但没人接 → 那是 
"worker_concurrency" 或 broker 连接问题
- 如果 
"worker_prefetch_multiplier" > 1 → 调到 1，防止任务被囤

第二步：把"死磕 BigQuery"的任务隔离出来（关键）

这是解决你问题的核心。做两件事：

A. 为 BigQuery 重任务建专用 Queue + 专用 Worker 组

# DAG 里给重任务指定队列
bigquery_task = BigQueryInsertJobOperator(
    task_id="heavy_bq_task",
    configuration={...},
    queue="bq_heavy",  # 指定专用队列
)

然后启一类专门的 Worker 只监听这个队列：

# 这类 Worker 的 worker_concurrency 可以设低一点（比如 4-8）
# 因为它们注定要"长时间 holding"
airflow celery worker -Q bq_heavy -c 8

这样普通任务走 default 队列，被普通 Worker 快速消费；BigQuery 重任务走 bq_heavy 队列，由专用 Worker 慢慢磨。新扩容出来的 Worker 如果监听 
"default" 队列，至少能接住普通任务，不会完全空转。

B. 调整并发配置，让 slot 不被长任务锁死

[core]
parallelism = 256  # 提高到 worker数 × worker_concurrency
dag_concurrency = 64

[celery]
worker_concurrency = 32  # 单机 8 核的话设 32-64 都行
worker_prefetch_multiplier = 1  # 关键！防止任务囤积

💡 有个真实案例：某团队从 2.9.2 升级后遇到"任务 queued 但不执行"，根因就是 
"parallelism=128" 但 
"worker_concurrency" 还是默认的 16，3 个 Worker 总共只能处理 48 个并发，远低于调度器想排进去的 128 个，导致大量任务积压 。把 
"worker_concurrency" 提到 64 问题解决。
第三步：根本解决方案——把长 BigQuery 任务改成 Deferrable

这一步是治本。Airflow 2.x 开始支持的 Deferrable Operator，对长耗时外部任务（BigQuery、Snowflake、Spark 等）是官方推荐方案 ：

bigquery_task = BigQueryInsertJobOperator(
    task_id="heavy_bq_task",
    configuration={
        "query": {
            "query": "SELECT * FROM `project.dataset.table`",
            "useLegacySql": False,
        }
    },
    deferrable=True,  # ← 关键开关
    location="US",
)

Deferrable 模式的工作机制：

1. Worker 提交 BigQuery 作业（约 1-2 秒）
2. Worker slot 立即释放，可以接新任务
3. 由一个叫做 Triggerer 的独立组件异步轮询 BigQuery 作业状态
4. 一个 Triggerer 进程可以同时 watch 上千个作业
5. BigQuery 作业完成后，Triggerer 通知 Worker 执行 
"execute_complete" 收尾

收益有多大：一个 90 分钟的 BigQuery 任务，经典模式占用 1.5 个 worker-slot-hour；deferrable 模式只占约 2 秒的 worker-slot-time，相当于 资源效率提升 99% 。

💡 TRM Labs 的工程团队从 Composer 迁到 GKE 后用 KEDA 基于队列深度扩缩 Worker，但他们也明确指出：对于长耗时查询，deferrable 化比单纯加 Worker 重要得多 。
启用 Deferrable 的前提：

- 你的 Airflow 环境要运行 Triggerer 组件：
"airflow triggerer"
- 至少 1 个 Triggerer 实例（生产环境建议 2 个做高可用）

第四步：AutoScaler 的触发指标要换

既然你在 GCE 上自管，建议把 MIG 的扩容逻辑从"CPU > 80%"改成基于任务队列深度：

# 自定义 MIG autoscaling 的 signal：
# 用 Cloud Monitoring 抓取 Airflow 元数据库里的 queued task 数
# 当 queued_tasks > N 时扩容 Worker

或者更彻底的方案——迁移到 GKE + KEDA。KEDA 直接读 Airflow 元数据库，按 
"running + queued" 任务数自动扩缩 Worker 副本数，这是目前社区验证过的 Airflow Worker 扩容最佳实践 ：

# KEDA ScaledObject 核心
triggers:
- type: postgresql
  metadata:
    query: >
      SELECT ceil(COUNT(*)::decimal / 32) 
      FROM task_instance 
      WHERE state IN ('running', 'queued')
    targetQueryValue: "1"

给你的行动优先级

按 ROI 排序：

1. 🔴 立即做：把 
"worker_prefetch_multiplier" 设为 1，防止任务囤积
2. 🟡 本周做：为 BigQuery 重任务建立独立 Queue，扩容出来的 Worker 专门监听这个队列
3. 🟢 本月做：把长耗时 BigQuery 任务逐步改成 
"deferrable=True"，并部署 Triggerer 组件
4. 🔵 长期：评估迁移到 GKE + KEDA，基于队列深度而非 CPU 做 Worker 扩缩

💡 一句话总结你的问题本质：Airflow 的 Worker 扩容不该由 CPU 驱动，而应由任务队列深度驱动；而长耗时 BigQuery 任务的根本解法不是加 Worker，而是用 Deferrable Operator 把 worker slot 释放出来。
你方便告诉我两个信息吗：

1. 你这些"死磕一张表"的任务，单次大概要跑多久？是几分钟级别还是小时级别？
2. 你现在的 Airflow 环境里有没有跑 Triggerer 组件？

这两个信息能帮我判断你更适合"Queue 隔离"的短期方案，还是可以直接上 Deferrable 的长期方案。