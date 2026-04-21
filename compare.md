```
import os
import logging
from google.cloud.sql.connector import Connector, IPTypes
import pg8000

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def execute_sql_files():
    """使用IAM认证顺序执行SQL文件"""
    
    # 从环境变量获取配置
    instance_connection_name = os.environ.get("CLOUD_SQL_INSTANCE")
    db_iam_user = os.environ.get("DB_IAM_USER")  # IAM数据库用户格式：sa-name@project-id.iam
    db_name = os.environ.get("DB_NAME", "postgres")
    
    # 验证必要参数
    if not instance_connection_name:
        raise ValueError("环境变量 CLOUD_SQL_INSTANCE 未设置")
    if not db_iam_user:
        raise ValueError("环境变量 DB_IAM_USER 未设置，格式应为：sa-name@project-id.iam")
    
    logger.info(f"连接到 Cloud SQL 实例: {instance_connection_name}")
    logger.info(f"使用 IAM 用户: {db_iam_user}")
    
    # 初始化连接器
    connector = Connector()
    
    def get_connection():
        """获取数据库连接（使用IAM认证）"""
        return connector.connect(
            instance_connection_name,
            "pg8000",
            user=db_iam_user,
            db=db_name,
            enable_iam_auth=True,  # 启用IAM认证
            ip_type=IPTypes.PUBLIC,  # 或 IPTypes.PRIVATE 如果使用私有IP
        )
    
    # 获取SQL文件列表（按文件名排序）
    sql_dir = "sql_scripts"
    if not os.path.exists(sql_dir):
        raise FileNotFoundError(f"SQL脚本目录不存在: {sql_dir}")
    
    sql_files = sorted([f for f in os.listdir(sql_dir) if f.endswith('.sql')])
    
    if not sql_files:
        logger.warning(f"在 {sql_dir} 目录中未找到SQL文件")
        return
    
    logger.info(f"找到 {len(sql_files)} 个SQL文件需要执行: {sql_files}")
    
    # 顺序执行每个SQL文件
    for sql_file in sql_files:
        file_path = os.path.join(sql_dir, sql_file)
        logger.info(f"开始执行: {sql_file}")
        
        try:
            # 读取SQL文件内容
            with open(file_path, 'r', encoding='utf-8') as f:
                sql_content = f.read()
            
            # 分割SQL语句（按分号）
            sql_statements = [stmt.strip() for stmt in sql_content.split(';') if stmt.strip()]
            
            if not sql_statements:
                logger.warning(f"文件 {sql_file} 为空或没有有效的SQL语句")
                continue
            
            # 执行每个语句
            with get_connection() as conn:
                with conn.cursor() as cursor:
                    for i, stmt in enumerate(sql_statements, 1):
                        if stmt:
                            try:
                                cursor.execute(stmt)
                                logger.debug(f"成功执行第 {i} 条语句")
                            except Exception as stmt_error:
                                logger.error(f"执行第 {i} 条语句时出错: {stmt_error}")
                                logger.error(f"问题语句: {stmt[:100]}...")  # 只显示前100字符
                                raise
                    conn.commit()
            
            logger.info(f"成功执行: {sql_file}")
            
        except Exception as e:
            logger.error(f"执行 {sql_file} 时出错: {str(e)}")
            # 可以根据需要决定是否继续执行后续文件
            # raise  # 如果出错就停止
            # 或者记录错误但继续执行
            logger.error(f"跳过文件 {sql_file}，继续执行下一个")
            continue
    
    # 关闭连接器
    connector.close()
    logger.info("所有SQL文件执行完成")

def test_connection():
    """测试IAM认证连接是否正常工作"""
    instance_connection_name = os.environ.get("CLOUD_SQL_INSTANCE")
    db_iam_user = os.environ.get("DB_IAM_USER")
    db_name = os.environ.get("DB_NAME", "postgres")
    
    connector = Connector()
    
    try:
        conn = connector.connect(
            instance_connection_name,
            "pg8000",
            user=db_iam_user,
            db=db_name,
            enable_iam_auth=True,
            ip_type=IPTypes.PUBLIC,
        )
        
        with conn.cursor() as cursor:
            cursor.execute("SELECT version();")
            result = cursor.fetchone()
            logger.info(f"数据库版本: {result[0]}")
        
        conn.close()
        logger.info("IAM认证连接测试成功")
        return True
        
    except Exception as e:
        logger.error(f"连接测试失败: {str(e)}")
        return False
    finally:
        connector.close()

if __name__ == "__main__":
    # 可选：先测试连接
    if test_connection():
        execute_sql_files()
    else:
        logger.error("连接测试失败，停止执行")
