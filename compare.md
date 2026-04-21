```
# main.py - Cloud Functions代码
import os
import sqlalchemy
from google.cloud.sql.connector import Connector

def execute_sql(request):
    """HTTP触发的Cloud Function，执行SQL脚本"""
    
    # 配置数据库连接
    INSTANCE_CONNECTION_NAME = os.environ.get("INSTANCE_CONNECTION_NAME")
    DB_USER = os.environ.get("DB_USER")
    DB_PASS = os.environ.get("DB_PASS")
    DB_NAME = os.environ.get("DB_NAME")
    
    # SQL文件列表（可以从Cloud Storage读取）
    sql_files = [
        "01_init.sql",
        "02_tables.sql",
        "03_data.sql"
    ]
    
    # 使用Cloud SQL Connector连接
    connector = Connector()
    
    def getconn():
        conn = connector.connect(
            INSTANCE_CONNECTION_NAME,
            "pg8000",
            user=DB_USER,
            password=DB_PASS,
            db=DB_NAME
        )
        return conn
    
    # 创建连接池
    pool = sqlalchemy.create_engine(
        "postgresql+pg8000://",
        creator=getconn,
    )
    
    # 顺序执行SQL文件
    with pool.connect() as db_conn:
        for sql_file in sql_files:
            # 从Cloud Storage读取SQL文件
            sql_content = read_sql_from_gcs(sql_file)
            db_conn.execute(sqlalchemy.text(sql_content))
    
    return "SQL执行完成"


# Dockerfile
FROM python:3.9-slim

# 安装依赖
RUN pip install sqlalchemy pg8000 google-cloud-sql-connector

# 复制执行脚本
COPY execute_sql.py /app/execute_sql.py
COPY sql_scripts/ /app/sql_scripts/

WORKDIR /app
CMD ["python", "execute_sql.py"]
