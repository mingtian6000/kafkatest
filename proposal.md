```python
import os
from fastmcp import FastMCP
from kubernetes import client, config

# 初始化 MCP
mcp = FastMCP("GKE-Manager")

def get_k8s_api_client(proxy_url=None):
    """
    手动配置带代理的 K8s 客户端
    """
    # 1. 加载本地 kubeconfig (通常由 gcloud container clusters get-credentials 生成)
    config.load_kube_config()
    
    # 2. 获取当前上下文的配置
    k8s_config = client.Configuration.get_default_copy()
    
    # 3. 核心：注入代理
    # 如果你在命令行需要配代理，这里就填那个代理地址
    if proxy_url:
        k8s_config.proxy = proxy_url
        # 如果是私有集群自签证书，有时需要跳过校验（生产环境慎用）
        # k8s_config.verify_ssl = False 
    
    return client.CoreV1Api(client.ApiClient(k8s_config))

@mcp.tool()
def list_gke_pods(namespace: str = "default", proxy: str = None):
    """
    列出 GKE 集群中的 Pod。
    :param namespace: 命名空间
    :param proxy: 可选，例如 'http://127.0.0.1:8888'。如果不传则尝试从环境变量获取。
    """
    # 优先使用传入的 proxy，其次选环境变量，最后 None
    proxy_to_use = proxy or os.getenv("HTTPS_PROXY") or os.getenv("http_proxy")
    
    try:
        v1 = get_k8s_api_client(proxy_to_use)
        pods = v1.list_namespaced_pod(namespace)
        
        result = []
        for pod in pods.items:
            result.append({
                "name": pod.metadata.name,
                "status": pod.status.phase,
                "pod_ip": pod.status.pod_ip
            })
        return result
    except Exception as e:
        return f"无法连接到集群: {str(e)}"

if __name__ == "__main__":
    mcp.run()
