```
你说的就是 Apache Airflow 2.x → 3.x 的 breaking change，这块官方升级指南和 3.0 release notes 已经写得比较明确。下面按“升级前最容易踩坑”的层级给你梳理，主要覆盖 3.0，也顺带提了 3.1/3.2 里继续收紧的点。

前置条件先卡住

- 升级起点必须是 Airflow ≥ 2.7，官方建议先升到最新的 2.x，把 deprecation warning 清完再跳 3.x。
- Python 只支持 3.9–3.12（3.1+ 又去掉了 3.9）。
- 官方给的两个扫描工具一定要跑：
   - 
"ruff check dags/ --select AIR301,AIR302 --preview"（AIR30x 系列抓 DAG 层 breaking change）
   - 
"airflow config update" / 
"airflow config lint"（扫配置项）

最影响存量代码的：架构级变化

Airflow 3 引入 AIP-72（Task Execution API + Task SDK）：

- Worker / task / DAG processor / triggerer 不再直连 metadata database，统一走新的 API Server。
- 你如果在自定义 Operator、Sensor、trigger 里写过 
"provide_session"、
"create_session"、
"session.query(...)"、直接 import 
"airflow.models.*" 去查库——3.x 会直接报错，要改成走 REST API / Task SDK / 
"airflow.sdk"。
- 这对“老项目里偷偷拿 session 查别的 dag_run / xcom / variable”的写法杀伤最大，基本是第一批要改的东西。

DAG 编写层的 breaking change

- 
"DAG(..., schedule_interval=...)" / 
"timetable=" 被移除，统一用 
"schedule="；
"DAG.schedule_interval" 属性也没了。
- 上下文变量里大量移除，模板/
"{{ }}" 里用到会直接坏：
   - 
"execution_date" → 用 
"dag_run.logical_date"
   - 
"prev_ds / next_ds / yesterday_ds / tomorrow_ds / prev_execution_date / next_execution_date / prev_execution_date_success" 等全部移除
- 
"logical_date" 语义变了：2.x 里约等于 
"data_interval_start"，3.x 里约等于 run-after（排队/触发时间点）；另外现在允许 
"logical_date=None"（用于推理、事件驱动、ML 流水线）。
- 裸 cron 字符串默认从 
"CronDataIntervalTimetable" 变成 
"CronTriggerTimetable"，如果你依赖“data interval 左闭右开”的老行为，要开 
"[scheduler].create_cron_data_intervals=True"。
- 
"catchup_by_default" 默认改成 False。
- 
"allow_illegal_arguments" 类容忍没了：Operator 传错参（比如拼错 kwarg）2.x 默默忽略，3.x 直接 import 失败。

被彻底移除的功能 / 替代物

- SubDagOperator / SubDAG → TaskGroup
- SLA / sla_callbacks → 后续用 Deadline Alerts（3.2+ 开始有）
- SequentialExecutor / DebugExecutor → 本地开发用 LocalExecutor；DebugExecutor 在 3.x 早期也被移走/重构
- CeleryKubernetesExecutor、LocalKubernetesExecutor → 多 Executor 配置（Multiple Executor Config）
- DAG pickling、XCom pickling（默认后端不再允许 pickle；老 XCom 进归档表，要传复杂对象得自定义 XCom backend）
- Experimental API 没了，全走 REST API
- CLI 的 
"--subdir / -S" 被 DAG Bundles 概念替代

Provider / import 拆分（也是大坑）

原来在 
"airflow-core" 里的常用算子被拆到独立包 apache-airflow-providers-standard：

- 
"BashOperator"、
"PythonOperator"、
"EmailOperator"、
"SimpleHttpOperator"、
"ShortCircuitOperator"、
"DummyOperator→EmptyOperator" 等都挪出去了
- 旧 import 路径 
"airflow.operators.* / airflow.sensors.* / airflow.hooks.* / airflow.macros.*" 在 3.0 起陆续移除，长期稳定写法是：
   - 通用 DAG 构件：
"from airflow.sdk import DAG, @task, TaskGroup, Asset, Connection, Variable"
   - 标准算子：
"from airflow.providers.standard.operators.python import PythonOperator"

部署 / API / 配置侧

- REST API：
"/api/v1" → FastAPI 的 
"/api/v2"，校验更严格（422 代替 400），
"execution_date" 参数换成 
"logical_date"；POST 触发 DAG run 不传 
"logical_date" 时默认 
"None"。
- CLI 拆分：
"airflow" 管本地，
"airflowctl" 管远程/服务化部署（AIP-81）。
- 
"airflow db init / db upgrade" 合并成 
"airflow db migrate"。
- UI 从 Flask-AppBuilder 换成 React + FastAPI；FAB 不再是默认 auth manager，老 
"webserver_config.py" 里的 
"AirflowSecurityManager" 要改成 
"FabAirflowSecurityManagerOverride"（装 
"apache-airflow-providers-fab"）。
- 老 
"appbuilder_views / appbuilder_menu_items / flask_blueprints" 插件要迁 FastAPI，或靠 FAB provider 的兼容层兜底。

如果你愿意，我可以按你现在的代码形态（比如“主要是 Bash/Python DAG + 自研 Operator + 调 REST API / 用 XCom 传对象”）帮你筛出一张只针对你项目的 2→3 迁移 checklist。