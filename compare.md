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
