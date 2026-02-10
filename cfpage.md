你提到的这套架构（Airflow + RabbitMQ + Celery）在 Google Cloud 的 VM 上，是数据工程领域最经典的“任务调度 + 消息队列 + 分布式执行”组合。它主要用来解决大规模、复杂、长时间运行的数据处理任务的编排与执行问题。

简单来说，这套架构的核心逻辑是：Airflow 负责“指挥”（编排任务），RabbitMQ 负责“传令”（传递指令），Celery 负责“干活”（执行任务）。

1. 各组件扮演的角色

组件 角色 比喻
Airflow 调度器 (Orchestrator) 导演。它不干活，只负责定义任务流程（DAG），决定什么时候该运行哪个任务，以及任务之间的依赖关系（谁先谁后）。
RabbitMQ 消息队列 (Message Broker) 传令兵。Airflow 把“要执行任务”的指令（消息）发给 RabbitMQ，Celery 从 RabbitMQ 这里领取任务。它解耦了调度器和执行器，避免它们直接通信。
Celery 执行器 (Executor) 工人。它负责实际执行 Airflow 定义的任务（比如跑 Python 脚本、执行 SQL、调用 API）。它通常部署在多个 VM 上，形成“工人集群”。

2. 这套架构解决的具体问题

在数据工程中，单台机器往往无法处理海量数据或复杂的计算逻辑。这套架构通过分布式和异步的方式解决了以下痛点：

* 解决单点瓶颈：如果只用 Airflow 的默认执行器，所有任务都在调度器所在的机器上运行。一旦任务量大或任务复杂，调度器会卡死。引入 Celery 后，任务被分发到多台 VM 上并行执行，大大提升了吞吐量。
* 实现高可用：RabbitMQ 和 Celery Worker 都可以部署多个节点。即使某个 Worker 宕机，任务消息还在队列里，其他 Worker 可以接手继续执行，保证了系统的稳定性。
* 处理长耗时任务：数据清洗、模型训练、ETL 任务往往需要运行几小时甚至几天。这种架构支持任务的异步执行，Airflow 发完指令后就不用管了，由 Celery Worker 在后台默默执行，不会阻塞调度器。
* 资源弹性伸缩：当任务队列积压很多时，你可以快速在 GCP 上启动新的 VM 加入 Celery 集群（Scale Out）；任务少的时候可以关掉 VM（Scale In），非常灵活。

3. 典型应用场景

* 大数据 ETL 流水线：每天定时从各个业务数据库抽取数据，经过清洗转换（Transform）后加载到数据仓库（如 BigQuery）。
* 机器学习流水线 (MLOps)：定期重新训练 AI 模型，包括数据预处理、特征工程、模型训练、模型评估和部署。
* 报表系统：生成复杂的业务报表，涉及多表关联和聚合计算。
* 数据质量监控：定时检查数据的一致性、完整性和准确性。

你提到的 Salary 其实是 Celery（发音类似“塞勒瑞”），这是一个非常流行的 Python 分布式任务队列框架。它和 Airflow 是搭档关系，不是部署在 Airflow 上的服务。

1. Celery 是什么？

你可以把它理解成 “干活的小工”。它的核心组件是 Worker（工人），专门负责执行具体的任务代码（比如跑 Python 脚本、处理数据、调用 API）。

2. 它在哪里？

在 Google Cloud 的 VM 架构中，Celery Worker 通常部署在独立的虚拟机上，或者与 Airflow 的 Scheduler/Webserver 部署在同一台机器上（但作为独立进程运行）。

* Airflow VM：负责“指挥”（调度任务）。
* RabbitMQ VM：负责“传令”（传递任务指令）。
* Celery Worker VM：负责“干活”（执行任务）。

3. 为什么需要它？

Airflow 本身只负责编排任务流程（DAG），但具体执行任务需要“执行器”。Celery Executor 是 Airflow 最常用的分布式执行器，它允许你将任务分发到多台机器（Worker）上并行执行，从而解决单机性能瓶颈，实现高并发处理。

总结：Celery 是 Airflow 集群中负责实际执行代码的组件，通常以独立进程或独立 VM 的形式存在。

这个问题拆解成两块：Airflow 怎么连 RabbitMQ 和 RabbitMQ 集群怎么配。

1. Airflow 连接 RabbitMQ 配置

Airflow 通过 
"CeleryExecutor" 与 RabbitMQ 通信。你只需要在 Airflow 的配置文件（
"airflow.cfg" 或环境变量）中修改以下两个核心参数：

* 执行器类型：
"executor = CeleryExecutor"
* 消息队列地址：
"broker_url = amqp://用户名:密码@RabbitMQ主机IP:5672/"

关键点：

* 协议：必须使用 
"amqp://" 协议。
* 端口：默认是 
"5672"（管理界面端口是 
"15672"，别搞混）。
* 权限：确保 RabbitMQ 里创建了对应的用户和 vhost，并授予读写权限。

2. RabbitMQ 集群配置（3节点）

在 GCP VM 上搭建 3 节点集群，核心是共享 Erlang Cookie 和节点发现。

第一步：共享 Cookie（集群通信凭证）

RabbitMQ 节点间通过 Erlang Cookie 认证。你必须让 3 台 VM 的 Cookie 文件内容完全一致。

* 文件路径：
"/var/lib/rabbitmq/.erlang.cookie"
* 操作：将其中一台 VM 的 Cookie 内容复制到另外两台，然后重启 RabbitMQ 服务。

第二步：节点发现（加入集群）

假设 3 台 VM 的主机名分别是 
"rabbit1"、
"rabbit2"、
"rabbit3"。

1. 启动第一个节点：在 
"rabbit1" 上正常启动服务。
2. 加入集群：在 
"rabbit2" 和 
"rabbit3" 上分别执行：
# 停止应用
rabbitmqctl stop_app
# 加入 rabbit1 的集群
rabbitmqctl join_cluster rabbit@rabbit1
# 启动应用
rabbitmqctl start_app
3. 验证：执行 
"rabbitmqctl cluster_status"，看到 3 个节点都是 
"running" 就成功了。

第三步：高可用策略（Mirrored Queue）

集群建好后，默认队列数据只存在一个节点上。为了容灾，你需要设置镜像队列，让消息在 3 个节点间同步：

rabbitmqctl set_policy ha-all "^" '{"ha-mode":"all"}'

这条命令会让所有队列在所有节点上创建副本，这样即使某个 VM 宕机，任务也不会丢失。