```python
@mcp.tool()
def clone_page_with_attachments(source_page_id: str, target_space: str, new_title: str):
    """
    克隆页面内容及其所有附件到新页面。
    """
    # 1. 获取原页面内容和附件
    source_page = conf.get_page_by_id(source_page_id, expand='body.storage')
    attachments = conf.get_attachments_from_content(source_page_id)['results']
    
    # 2. 创建新页面
    new_page = conf.create_page(space=target_space, title=new_title, 
                                body=source_page['body']['storage']['value'])
    new_id = new_page['id']
    
    # 3. 循环处理附件
    for att in attachments:
        file_name = att['title']
        # 下载附件内容
        file_content = conf.get_attachment_content(att['id'])
        # 上传到新页面
        conf.attach_file(file_content, name=file_name, page_id=new_id)
        
    return f"克隆完成！新页面 ID: {new_id}，已迁移 {len(attachments)} 个附件。"


```python
import requests
import json
from google.oauth2 import service_account
from google.auth.transport.requests import Request
from google.auth.transport.urllib3 import urllib3
from google.cloud import container_v1
from kubernetes import client, config
from kubernetes.client import Configuration
from kubernetes.client.rest import ApiException
import yaml
import os
import tempfile
from urllib3 import PoolManager, ProxyManager

class DualProxyGKEClient:
    """
    处理双重代理场景的GKE客户端：
    1. 通过GCP代理访问Google Cloud API
    2. 通过GKE代理访问Kubernetes API
    """
    
    def __init__(self, 
                 gcp_proxy_url,      # 如: http://gcp-proxy:3128
                 gke_proxy_url,      # 如: http://gke-proxy:8080
                 sa_key_path,        # 服务账号JSON路径
                 project_id,
                 location,
                 cluster_name):
        
        self.gcp_proxy_url = gcp_proxy_url
        self.gke_proxy_url = gke_proxy_url
        self.project_id = project_id
        self.location = location
        self.cluster_name = cluster_name
        
        # 第一步：配置GCP SDK使用代理访问Google API
        self._setup_gcp_client(sa_key_path)
        
        # 第二步：获取集群信息
        self.cluster_info = self._get_cluster_info()
        
        # 第三步：配置K8s客户端使用GKE代理
        self.k8s_client = self._setup_k8s_client()
    
    def _setup_gcp_client(self, sa_key_path):
        """配置GCP客户端通过代理访问Google API"""
        print(f"配置GCP客户端，使用代理: {self.gcp_proxy_url}")
        
        # 创建带代理的HTTP适配器
        self.gcp_proxies = {
            'http': self.gcp_proxy_url,
            'https': self.gcp_proxy_url,
        }
        
        # 1. 为google-auth配置代理
        import google.auth.transport.requests
        
        # 创建自定义session
        self.gcp_session = requests.Session()
        self.gcp_session.proxies.update(self.gcp_proxies)
        
        # 2. 加载服务账号凭证
        self.credentials = service_account.Credentials.from_service_account_file(
            sa_key_path,
            scopes=['https://www.googleapis.com/auth/cloud-platform']
        )
        
        # 3. 创建带代理的GCP客户端
        # 为container_v1客户端配置HTTP
        from google.api_core.client_options import ClientOptions
        
        client_options = ClientOptions(
            api_endpoint=f"https://container.googleapis.com",
            # 可以添加代理配置
        )
        
        # 手动创建客户端，传入我们配置的session
        self.gcp_client = container_v1.ClusterManagerClient(
            credentials=self.credentials,
            client_options=client_options
        )
        
        # 覆盖内部的HTTP适配器
        import google.auth.transport.requests
        
        # 刷新token
        request_adapter = google.auth.transport.requests.Request(session=self.gcp_session)
        self.credentials.refresh(request_adapter)
        
    def _get_cluster_info(self):
        """通过GCP代理获取集群信息"""
        print("通过GCP代理获取集群信息...")
        
        cluster_path = f"projects/{self.project_id}/locations/{self.location}/clusters/{self.cluster_name}"
        
        try:
            # 这个调用会通过GCP代理
            cluster = self.gcp_client.get_cluster(name=cluster_path)
            
            return {
                'endpoint': cluster.endpoint,  # K8s API Server地址
                'ca_cert': cluster.master_auth.cluster_ca_certificate,
                'name': cluster.name
            }
        except Exception as e:
            print(f"获取集群信息失败: {e}")
            # 如果通过SDK失败，尝试直接调用REST API
            return self._get_cluster_info_via_rest()
    
    def _get_cluster_info_via_rest(self):
        """通过REST API获取集群信息（备选方案）"""
        print("尝试通过REST API获取集群信息...")
        
        # 构造REST API URL
        url = (f"https://container.googleapis.com/v1/projects/{self.project_id}"
               f"/locations/{self.location}/clusters/{self.cluster_name}")
        
        # 获取访问令牌
        from google.auth.transport.requests import Request
        self.credentials.refresh(Request())
        access_token = self.credentials.token
        
        headers = {
            'Authorization': f'Bearer {access_token}',
            'Content-Type': 'application/json'
        }
        
        response = self.gcp_session.get(url, headers=headers)
        
        if response.status_code == 200:
            data = response.json()
            return {
                'endpoint': data['endpoint'],
                'ca_cert': data['masterAuth']['clusterCaCertificate'],
                'name': data['name']
            }
        else:
            raise Exception(f"REST API调用失败: {response.status_code}, {response.text}")
    
    def _setup_k8s_client(self):
        """配置K8s客户端通过GKE代理访问"""
        print(f"配置K8s客户端，使用GKE代理: {self.gke_proxy_url}")
        
        # 1. 获取访问K8s的token
        self.credentials.refresh(Request())
        k8s_token = self.credentials.token
        
        # 2. 创建kubeconfig配置
        kubeconfig = {
            'apiVersion': 'v1',
            'kind': 'Config',
            'clusters': [{
                'name': 'gke-cluster',
                'cluster': {
                    'server': f"https://{self.cluster_info['endpoint']}",
                    'certificate-authority-data': self.cluster_info['ca_cert'],
                }
            }],
            'contexts': [{
                'name': 'gke-context',
                'context': {
                    'cluster': 'gke-cluster',
                    'user': 'gke-user',
                }
            }],
            'current-context': 'gke-context',
            'users': [{
                'name': 'gke-user',
                'user': {
                    'token': k8s_token
                }
            }]
        }
        
        # 3. 将kubeconfig写入临时文件
        with tempfile.NamedTemporaryFile(mode='w', suffix='.yaml', delete=False) as f:
            yaml.dump(kubeconfig, f)
            kubeconfig_path = f.name
        
        # 4. 配置K8s客户端使用代理
        config.load_kube_config(config_file=kubeconfig_path)
        
        # 5. 获取K8s配置对象
        k8s_config = client.Configuration.get_default_copy()
        
        # 6. 关键步骤：为K8s客户端配置代理
        # 方法A：通过环境变量（最简单）
        os.environ['HTTP_PROXY'] = self.gke_proxy_url
        os.environ['HTTPS_PROXY'] = self.gke_proxy_url
        os.environ['NO_PROXY'] = 'localhost,127.0.0.1'  # 排除本地
        
        # 方法B：直接配置REST客户端（更可控）
        from kubernetes.client.rest import RESTClientObject
        import urllib3
        
        # 创建带代理的HTTP连接池
        http_pool = urllib3.ProxyManager(
            self.gke_proxy_url,
            cert_reqs='CERT_NONE',  # 如果代理是自签证书
            assert_hostname=False,
            retries=urllib3.Retry(3, redirect=2)
        )
        
        # 替换REST客户端的连接池
        k8s_config.proxy = self.gke_proxy_url
        
        # 7. 创建API客户端
        api_client = client.ApiClient(configuration=k8s_config)
        
        # 清理临时文件
        os.unlink(kubeconfig_path)
        
        return {
            'core_v1': client.CoreV1Api(api_client),
            'apps_v1': client.AppsV1Api(api_client),
            'config': k8s_config
        }
    
    def list_namespaces(self):
        """列出所有namespace"""
        print("通过GKE代理列出namespaces...")
        
        try:
            api_instance = self.k8s_client['core_v1']
            namespaces = api_instance.list_namespace()
            
            result = []
            for ns in namespaces.items:
                result.append({
                    'name': ns.metadata.name,
                    'status': ns.status.phase,
                    'creation_timestamp': ns.metadata.creation_timestamp.isoformat() if ns.metadata.creation_timestamp else None
                })
            
            return result
        except ApiException as e:
            print(f"K8s API异常: {e}")
            # 如果直接调用失败，尝试通过requests手动调用
            return self._list_namespaces_via_requests()
    
    def _list_namespaces_via_requests(self):
        """备选方案：通过requests直接调用K8s API"""
        print("通过requests直接调用K8s API...")
        
        # 获取token
        self.credentials.refresh(Request())
        token = self.credentials.token
        
        # 构造API URL
        api_url = f"{self.gke_proxy_url}/api/v1/namespaces"
        
        headers = {
            'Authorization': f'Bearer {token}',
            'Accept': 'application/json'
        }
        
        # 注意：这里通过GKE代理
        response = requests.get(
            api_url,
            headers=headers,
            verify=False,  # 如果代理是自签名证书
            proxies={'http': self.gke_proxy_url, 'https': self.gke_proxy_url}  # 明确指定代理
        )
        
        if response.status_code == 200:
            return response.json()['items']
        else:
            raise Exception(f"请求失败: {response.status_code}, {response.text}")
    
    def list_pods(self, namespace="default"):
        """列出指定namespace中的pods"""
        print(f"列出namespace '{namespace}'中的pods...")
        
        try:
            api_instance = self.k8s_client['core_v1']
            pods = api_instance.list_namespaced_pod(namespace=namespace)
            
            result = []
            for pod in pods.items:
                result.append({
                    'name': pod.metadata.name,
                    'namespace': pod.metadata.namespace,
                    'status': pod.status.phase,
                    'node': pod.spec.node_name,
                    'ip': pod.status.pod_ip,
                    'creation_timestamp': pod.metadata.creation_timestamp.isoformat() if pod.metadata.creation_timestamp else None
                })
            
            return result
        except ApiException as e:
            print(f"获取pods失败: {e}")
            return []
    
    def get_cluster_health(self):
        """检查集群健康状态"""
        print("检查集群健康状态...")
        
        try:
            # 尝试访问几个关键API端点
            api_instance = self.k8s_client['core_v1']
            
            # 1. 检查API Server
            api_versions = api_instance.get_api_versions()
            
            # 2. 检查节点状态
            nodes = api_instance.list_node()
            node_count = len(nodes.items)
            
            # 3. 检查所有namespace
            namespaces = api_instance.list_namespace()
            ns_count = len(namespaces.items)
            
            return {
                'status': 'healthy',
                'api_server': 'accessible',
                'node_count': node_count,
                'namespace_count': ns_count,
                'proxy_used': {
                    'gcp_proxy': self.gcp_proxy_url,
                    'gke_proxy': self.gke_proxy_url
                }
            }
        except Exception as e:
            return {
                'status': 'unhealthy',
                'error': str(e),
                'proxy_used': {
                    'gcp_proxy': self.gcp_proxy_url,
                    'gke_proxy': self.gke_proxy_url
                }
            }


# 使用示例
if __name__ == "__main__":
    # 配置信息
    CONFIG = {
        'gcp_proxy': 'http://your-gcp-proxy-ip:3128',  # 访问Google API的代理
        'gke_proxy': 'http://your-gke-proxy-ip:8080',  # 访问GKE集群的代理
        'service_account_key': '/path/to/service-account-key.json',
        'project_id': 'your-project-id',
        'location': 'us-central1',  # 或 'us-central1-a'
        'cluster_name': 'your-cluster-name'
    }
    
    try:
        # 初始化客户端
        print("初始化双重代理GKE客户端...")
        client = DualProxyGKEClient(
            gcp_proxy_url=CONFIG['gcp_proxy'],
            gke_proxy_url=CONFIG['gke_proxy'],
            sa_key_path=CONFIG['service_account_key'],
            project_id=CONFIG['project_id'],
            location=CONFIG['location'],
            cluster_name=CONFIG['cluster_name']
        )
        
        # 测试连接
        print("\n1. 检查集群健康状态:")
        health = client.get_cluster_health()
        print(f"   状态: {health['status']}")
        print(f"   节点数: {health.get('node_count', 'N/A')}")
        
        # 列出所有namespace
        print("\n2. 列出所有namespace:")
        namespaces = client.list_namespaces()
        for ns in namespaces:
            print(f"   - {ns['name']} (状态: {ns['status']})")
        
        # 列出每个namespace中的pods
        print("\n3. 列出pods:")
        for ns in namespaces[:3]:  # 只查看前3个namespace
            ns_name = ns['name']
            pods = client.list_pods(namespace=ns_name)
            print(f"\n   Namespace: {ns_name}")
            for pod in pods:
                print(f"     Pod: {pod['name']}, 状态: {pod['status']}, IP: {pod['ip']}")
                
    except Exception as e:
        print(f"错误: {e}")
        import traceback
        traceback.print_exc()
