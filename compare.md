```
import os
from sqlalchemy import create_engine

# 获取 Cloud SQL 连接信息
# 格式：/cloudsql/{项目}:{区域}:{实例名}
CLOUD_SQL_CONNECTION_NAME = os.environ.get("CLOUD_SQL_CONNECTION_NAME")

# 对于 PostgreSQL
DATABASE_USER = os.environ.get("DB_USER", "postgres")
DATABASE_NAME = os.environ.get("DB_NAME", "mydb")

# IAM 认证的连接字符串格式
# 注意：不需要密码，因为认证通过服务账号的 IAM 进行
engine = create_engine(
    f"postgresql+psycopg2://{DATABASE_USER}@/cloudsql/{CLOUD_SQL_CONNECTION_NAME}/{DATABASE_NAME}",
    # 如果需要额外参数
    pool_pre_ping=True,
    pool_size=5,
    max_overflow=10
)

# 使用示例
with engine.connect() as conn:
    result = conn.execute("SELECT 1")
    print(result.fetchone())

import os
import psycopg2
from google.auth import default
from google.auth.transport.requests import Request

def get_db_connection():
    """通过 Unix socket 连接 Cloud SQL，使用 IAM 认证"""
    
    # 1. 获取 IAM 令牌
    credentials, project = default()
    credentials.refresh(Request())
    iam_token = credentials.token
    
    # 2. 构建连接参数
    instance_connection_name = os.environ.get('CLOUD_SQL_INSTANCE')  # 格式: project:region:instance
    db_name = os.environ.get('DB_NAME')
    db_user = os.environ.get('DB_IAM_USER')  # 格式: sa-name@project-id.iam
    
    # 3. Unix socket 路径
    unix_socket_path = f"/cloudsql/{instance_connection_name}/.s.PGSQL.5432"
    
    # 4. 建立连接
    conn = psycopg2.connect(
        host=unix_socket_path,  # 关键：使用 Unix socket 路径作为 host
        database=db_name,
        user=db_user,
        password=iam_token,  # 使用 IAM 令牌作为密码
        sslmode='disable',  # 通过 Unix socket 连接时不需要 SSL
    )
    
    return conn


