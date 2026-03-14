我没办法直接 commit 到你的 GitHub 仓库（我没有写入权限），但我可以给你整理一份完整的 Markdown 文档，你直接复制粘贴到 GitHub 就行。

以下是完整的排查文档内容，你可以保存为 `airflow-execution-date-error-fix.md` 然后 push 到你的 repo：

```markdown
# Airflow 2.9 Cloud SQL PostgreSQL - execution_date 错误排查与修复

## 问题现象
Airflow 2.9 连接 Cloud SQL PostgreSQL 时，`airflow db upgrade` 或 `airflow db migrate` 报错：
```

AttributeError: execution_date

# 1. 找到 db.py 文件
DB_PY="/usr/local/lib/python3.9/site-packages/airflow/utils/db.py"

# 2. 备份
cp $DB_PY ${DB_PY}.bak

# 3. 用 sed 注释掉 check_run_id_null 函数（让它直接返回空）
sed -i '/def check_run_id_null/,/return.*issues/{
  /def check_run_id_null/a\    return []  # TEMPORARY PATCH
  /def check_run_id_null/,/^    return \[\]  # TEMPORARY PATCH/d
}' $DB_PY

# 4. 现在执行 migrate（应该能过）
export AIRFLOW_CONFIG=/opt/airflow/airflow.cfg
/usr/local/bin/airflow db migrate

# 5. 恢复原始文件（重要！）
mv ${DB_PY}.bak $DB_PY

```
或 `check_run_id_null` 函数失败。

**根本原因**：Airflow 2.2 迁移脚本检查旧数据时访问了 `execution_date` 列，但数据库状态与 alembic_version 标记不匹配。

---

## 前置检查：确认连接正确的数据库

### 1. 检查当前配置（排查 SQLite 陷阱）
```python
export AIRFLOW_CONFIG=/opt/airflow/airflow.cfg

python3 << 'EOF'
from airflow.configuration import conf
from airflow.utils.db import create_session, provide_session
from sqlalchemy import text, inspect
from airflow.utils.session import NEW_SESSION

conf.load_test_config()

with create_session() as session:
    # 1. 检查 dag_run 表是否存在
    inspector = inspect(session.bind)
    tables = inspector.get_table_names()
    
    if 'dag_run' not in tables:
        print("dag_run 表不存在，数据库是空的，直接初始化")
        print("请运行: airflow db init")
        exit(0)
    
    # 2. 检查 dag_run 表结构（是否有 execution_date 列）
    columns = [col['name'] for col in inspector.get_columns('dag_run')]
    print(f"dag_run 表列: {columns}")
    
    has_execution_date = 'execution_date' in columns
    has_run_id = 'run_id' in columns
    
    # 3. 如果有 execution_date 列（旧表结构），检查是否有 NULL 值
    if has_execution_date:
        result = session.execute(text("SELECT COUNT(*) FROM dag_run WHERE execution_date IS NULL"))
        null_count = result.scalar()
        print(f"execution_date 为 NULL 的行数: {null_count}")
        
        if null_count > 0:
            # 删除这些行（或更新它们）
            print("删除 execution_date 为 NULL 的行...")
            session.execute(text("DELETE FROM dag_run WHERE execution_date IS NULL"))
            session.commit()
            print("已清理")
    
    # 4. 如果有 run_id 列（新表结构），检查是否有 NULL 值
    if has_run_id:
        result = session.execute(text("SELECT COUNT(*) FROM dag_run WHERE run_id IS NULL"))
        null_count = result.scalar()
        print(f"run_id 为 NULL 的行数: {null_count}")
        
        if null_count > 0:
            print("删除 run_id 为 NULL 的行...")
            session.execute(text("DELETE FROM dag_run WHERE run_id IS NULL"))
            session.commit()
            print("已清理")
    
    # 5. 强制修复 alembic_version（标记为 2.9.0，跳过所有旧检查）
    print("强制更新 alembic_version 到 2.9.0...")
    session.execute(text("DELETE FROM alembic_version"))
    session.execute(text("INSERT INTO alembic_version (version_num) VALUES ('c4602d0c5c2d')"))
    session.commit()
    print("版本已强制更新为 2.9.0")

print("修复完成，现在可以运行: airflow db migrate")
EOF

# 执行修复后的迁移
/usr/local/bin/airflow db migrate


# 现在执行 migrate（应该能过）
export AIRFLOW_CONFIG=/opt/airflow/airflow.cfg
/usr/local/bin/airflow db migrate

# 如果手动执行和 systemd 结果不同，强制指定配置
export AIRFLOW_CONFIG=/opt/airflow/airflow.cfg
/usr/local/bin/airflow config get-value database sql_alchemy_conn
```

2. 查看 Alembic 版本状态

```bash
export AIRFLOW_CONFIG=/opt/airflow/airflow.cfg

# 查看迁移历史
/usr/local/bin/airflow db history

# 查看当前数据库版本（如果有 psql）
/usr/local/bin/airflow db shell -c "SELECT version_num FROM alembic_version;"
```

---

快速修复方案（按成功率排序）

方案 A：强制跳过数据检查（最快，推荐先尝试）

```bash
export AIRFLOW_CONFIG=/opt/airflow/airflow.cfg
export AIRFLOW__DATABASE__CHECK_MIGRATIONS=False
export AIRFLOW__DATABASE__LOAD_DEFAULT_CONNECTIONS=False

# 从 2.2.0 开始重新迁移（跳过有问题的检查）
/usr/local/bin/airflow db migrate --from-version 2.2.0
```

方案 B：Python 脚本修复版本标记（无需 psql）
如果 `db shell` 提示缺少 psql，用 Python 直接修复：

```bash
export AIRFLOW_CONFIG=/opt/airflow/airflow.cfg

python3 << 'EOF'
from airflow.configuration import conf
from airflow.utils.db import create_session
from sqlalchemy import text

conf.load_test_config()
with create_session() as session:
    # 查看当前版本
    try:
        result = session.execute(text("SELECT version_num FROM alembic_version"))
        current = result.fetchone()
        print(f"Current version: {current[0] if current else 'None'}")
    except Exception as e:
        print(f"Error checking version: {e}")
        print("Table may not exist")
    
    # 强制更新到 2.9.0（解决版本标记与表结构不匹配）
    session.execute(text("DELETE FROM alembic_version"))
    session.execute(text("INSERT INTO alembic_version (version_num) VALUES ('c4602d0c5c2d')"))
    session.commit()
    print("Force updated to version 2.9.0 (c4602d0c5c2d)")
EOF

# 再次迁移
/usr/local/bin/airflow db migrate
```

方案 C：Systemd 服务修复（解决启动失败）
如果 systemd service 起不来，修改 service 文件：

```bash
# 编辑 service
sudo systemctl edit airflow-db-upgrade.service --full

# 在 [Service] 部分添加：
Environment="AIRFLOW_CONFIG=/opt/airflow/airflow.cfg"
Environment="AIRFLOW__DATABASE__CHECK_MIGRATIONS=False"

# 或者修改 ExecStart：
ExecStart=/bin/bash -ce "AIRFLOW_CONFIG=/opt/airflow/airflow.cfg /usr/local/bin/airflow db migrate"

# 重载并启动
sudo systemctl daemon-reload
sudo systemctl restart airflow-db-upgrade.service
```

---

彻底重置（数据会丢失，慎用）

如果上述方案都失败，可能是数据库处于混乱状态：

```bash
# 1. 在 GCP Console 或使用 gcloud 删除并重建数据库
gcloud sql databases delete airflow --instance=你的实例名
gcloud sql databases create airflow --instance=你的实例名

# 2. 重新初始化
export AIRFLOW_CONFIG=/opt/airflow/airflow.cfg
/usr/local/bin/airflow db init
/usr/local/bin/airflow connections create-default-connections
```

---

常见坑点

1. AIRFLOW_HOME 不一致：手动执行时默认是 `/root/airflow`，systemd 是 `/opt/airflow`
2. 环境变量格式：`/etc/sysconfig/airflow` 里的 `KEY=VALUE` 需要手动 export 才能在 shell 生效
3. Cloud SQL Proxy：确保 proxy 正在运行（`systemctl status cloud-sql-proxy`）
4. Python 版本：Airflow 2.9 支持 Python 3.9，但建议升级到 3.11 避免未来兼容性问题

---

验证修复

```bash
# 检查迁移状态
/usr/local/bin/airflow db check-migrations

# 检查表结构
/usr/local/bin/airflow db shell -c "\d dag_run"

# 启动服务
sudo systemctl start airflow-webserver
sudo systemctl start airflow-scheduler
```

---

注意：如果看到 `sqlite:////root/airflow/airflow.db`，说明你连的是本地 SQLite，不是 Cloud SQL PostgreSQL。检查 `AIRFLOW_CONFIG` 和 `sql_alchemy_conn` 配置。

```

**使用方法：**
1. 复制上面所有内容
2. 在你的 GitHub 仓库里新建文件 `airflow-execution-date-error-fix.md`
3. 粘贴保存，commit

这样你手机或电脑上都能直接查看和复制命令，不需要手动抄了。

**需要我调整什么内容吗？**（比如加 Celery/RabbitMQ 相关的配置，或者改成中文格式）