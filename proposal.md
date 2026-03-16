我来帮你查 Airflow 2.9.0 的最后一个 migration hash，以及 3.0.0 到 2.9.0 之间需要处理的 schema 差异。
让我查一下 Airflow 2.9.0 的具体 alembic revision 和 3.0.0 与 2.9.0 之间的 schema 差异：
让我再查一下 Airflow 2.9.0 的具体 alembic hash 和从 3.0 降级到 2.9 需要处理的表结构差异：
让我再查一下 2.9.0 具体的 alembic hash：
根据搜索结果，我找到了 Airflow 2.9.0 的最后一个 alembic hash 以及需要修改的表结构。

目标 Hash

Airflow 2.9.0 的最后一个 revision hash 是：`959e216a3abb`

这个 hash 是 3.0.0 第一个 migration (`29ce7909c52b`) 的前一个版本（down_revision），对应 2.9.0 的最终状态 。

需要执行的 SQL 操作

1. 首先修改 alembic_version 表

```sql
UPDATE alembic_version SET version_num = '959e216a3abb';
```

2. 需要手动回滚的 Schema 变更（3.0.0 → 2.9.0）

3.0.0 相比 2.9.0 有多个重大 schema 变更 ，你需要手动回滚这些变更：

TaskInstance 表（关键）
3.0.0 修改了 TI 表，为每次尝试使用唯一的 UUID 作为 ID/主键：

```sql
-- 检查是否有 ti_id 字段（3.0.0 新增的）
-- 如果有，需要保留或处理，但 2.9.0 可能不认识这个字段
-- 可能需要删除 3.0.0 新增的外键和字段

-- 删除 TaskReschedule 表中可能新增的 ti_id 外键（如果有）
-- 恢复使用 task_id, dag_id, execution_date 作为外键
```

DagRun 表（关键）
3.0.0 添加了 `run_after` 字段，移除了 `execution_date` 作为主键的概念：

```sql
-- 检查 dag_run 表结构
-- 3.0.0 将 execution_date 改为 nullable 并添加了 run_after
-- 2.9.0 需要 execution_date 作为非空字段

-- 如果 run_after 存在，需要把数据复制回 execution_date
UPDATE dag_run SET execution_date = run_after WHERE execution_date IS NULL;

-- 删除 3.0.0 新增的字段（如果存在）
ALTER TABLE dag_run DROP COLUMN IF EXISTS run_after;
```

Dag 表

```sql
-- 3.0.0 将 is_active 重命名为 is_stale
-- 回滚：如果存在 is_stale，需要改回 is_active
ALTER TABLE dag RENAME COLUMN is_stale TO is_active;

-- 3.0.0 删除了 default_view 列，如果需要可以添加回来
ALTER TABLE dag ADD COLUMN IF NOT EXISTS default_view VARCHAR(25) DEFAULT 'grid';
```

TaskInstance 表的其他变更

```sql
-- 3.0.0 添加了 try_id 和 scheduled_dttm 字段
-- 2.9.0 不认识这些字段，可能需要删除或保留（如果不影响运行）

-- 如果 2.9.0 报错，可能需要：
ALTER TABLE task_instance DROP COLUMN IF EXISTS try_id;
ALTER TABLE task_instance DROP COLUMN IF EXISTS scheduled_dttm;
```

XCom 表

```sql
-- 3.0.0 修改了 XCom 的主键结构（使用 TI.id 作为外键）
-- 可能需要确保 XCom 表有 dag_id, task_id, execution_date 字段
```

3. 简化方案（如果上述太复杂）

如果手动修改表结构太复杂，建议直接清空相关表（会丢失历史数据，但能让系统启动）：

```sql
-- 先备份
CREATE TABLE dag_run_backup AS SELECT * FROM dag_run;
CREATE TABLE task_instance_backup AS SELECT * FROM task_instance;

-- 清空可能不兼容的数据
TRUNCATE TABLE dag_run;
TRUNCATE TABLE task_instance;
TRUNCATE TABLE xcom;
TRUNCATE TABLE task_reschedule;
TRUNCATE TABLE log;
```

然后更新 alembic_version：

```sql
UPDATE alembic_version SET version_num = '959e216a3abb';
```

操作步骤总结

1. 备份数据库（必须）
2. 更新 alembic_version：`UPDATE alembic_version SET version_num = '959e216a3abb';`
3. 尝试启动 Airflow 2.9.0
4. 如果报 execution_date 错误，检查 `dag_run` 和 `task_instance` 表的 `execution_date` 字段是否非空，必要时填充数据或清空表
5. 如果报 column 不存在错误，可能需要删除 3.0.0 新增的列（如 `try_id`, `scheduled_dttm`, `run_after` 等）

注意：从 3.0.0 降级到 2.9.0 涉及核心调度逻辑变更（execution_date 机制改变），最安全的做法是清空 dag_run 和 task_instance 表后重新启动，这样可以确保 schema 兼容但会丢失历史运行记录。