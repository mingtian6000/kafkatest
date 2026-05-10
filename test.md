```
看到你的描述，我理解问题所在了。你的情况比较特殊——你有一个基础 Jenkins 实例，然后在这个实例下创建了多个 workspace/context（如 production/1234），这在容器化或多租户的 Jenkins 部署中确实存在。

问题定位

401 错误通常表示认证失败。你的配置很可能有以下问题：

1. URL 拼接错误：
"base_url + "/" + env_name" 这种方式在特定 Jenkins 部署中可能不对
2. Token 权限问题：Token 可能被限制在特定的 workspace/context
3. 用户权限不足：用户可能没有访问这个特定 context 的权限

解决方案

第一步：验证正确的 URL 格式

用 curl 验证不同的 URL 格式，确定正确的访问方式：

# 尝试不同的 URL 格式
curl -v -u 用户名:Token "http://jenkins-host:8080/jenkins/api/json"  # 默认 Jenkins
curl -v -u 用户名:Token "http://jenkins-host:8080/jenkins/1234/api/json"  # 带 context
curl -v -u 用户名:Token "http://jenkins-host:8080/jenkins/production/1234/api/json"  # 完整路径
curl -v -u 用户名:Token "http://jenkins-host:8080/production/1234/api/json"  # 无 jenkins 前缀

查看哪个能返回 200 状态码和 Jenkins 信息。

第二步：修改代码，使用正确的 URL 格式

根据 curl 测试结果，调整你的代码：

如果完整 URL 是 
"http://jenkins-host:8080/jenkins/production/1234"：

import os
import jenkins

class JenkinsClient:
    def __init__(self, env_name="1234"):
        # 基础 Jenkins URL
        base_url = "http://jenkins-host:8080"
        
        # 根据你的实际部署情况选择正确的 URL 格式
        # 选项1: 如果你的 Jenkins 在子路径下
        # self.url = f"{base_url}/jenkins/{env_name}"
        
        # 选项2: 如果 production 是固定的父路径
        # self.url = f"{base_url}/jenkins/production/{env_name}"
        
        # 选项3: 如果 Jenkins 部署在根路径
        # self.url = f"{base_url}/{env_name}"
        
        # 先使用最简单的方式
        self.url = f"{base_url}/{env_name}"
        
        # 获取对应用户名和 Token
        self.user = os.environ.get(f"JENKINS_USER_{env_name}", "")
        self.password = os.environ.get(f"JENKINS_TOKEN_{env_name}", "")
        
        print(f"尝试连接 URL: {self.url}")
        print(f"用户: {self.user}")
        print(f"Token: {'*' * len(self.password) if self.password else '空'}")
        
        try:
            self._server = jenkins.Jenkins(
                self.url,
                username=self.user,
                password=self.password
            )
            # 测试连接
            user = self._server.get_whoami()
            print(f"连接成功! 用户: {user['fullName']}")
        except Exception as e:
            print(f"连接失败: {e}")

第三步：确保 Token 权限正确

1. 检查 Token 权限范围：
   - 在 Jenkins → 用户 → 设置 → API Token
   - 确认 Token 有访问指定 workspace/context 的权限
   - 可能需要重新生成一个具有更广权限的 Token
2. 检查用户权限：
   - 确认用户在 Jenkins 中有访问这个特定 context 的权限
   - 可能需要联系 Jenkins 管理员

第四步：使用 curl 调试 Python 代码

在你的 Python 代码中添加调试信息，看看它实际发送的请求是什么：

import requests
import logging

# 启用详细日志
logging.basicConfig(level=logging.DEBUG)

# 然后运行你的 Jenkins 连接代码

这会在控制台显示实际发送的 HTTP 请求，你可以和成功的 curl 命令对比。

第五步：备用方案 - 使用 requests 库直接调用

如果 python-jenkins 库一直有问题，可以先直接用 requests 测试：

import requests
import os

def test_jenkins_connection(base_url, context, user, token):
    # 尝试不同的 API 端点
    endpoints = [
        f"{base_url}/{context}/api/json",
        f"{base_url}/{context}/api/json?pretty=true",
        f"{base_url}/jenkins/{context}/api/json"
    ]
    
    for endpoint in endpoints:
        print(f"\n尝试访问: {endpoint}")
        try:
            response = requests.get(
                endpoint,
                auth=(user, token),
                timeout=10
            )
            print(f"状态码: {response.status_code}")
            if response.status_code == 200:
                print(f"成功! 响应: {response.json().get('description', '')}")
                return endpoint
            else:
                print(f"响应内容: {response.text[:200]}")
        except Exception as e:
            print(f"错误: {e}")
    
    return None

# 使用
url = test_jenkins_connection(
    base_url="http://jenkins-host:8080",
    context="production/1234",  # 或 "1234"
    user=os.environ.get("JENKINS_USER_1234"),
    token=os.environ.get("JENKINS_TOKEN_1234")
)

建议的排查步骤

1. 先用 curl 确认正确的 URL 和认证方式
2. 确保 curl 和 Python 代码使用完全相同的 URL 和认证信息
3. 在 Python 代码中启用详细日志
4. 先用 requests 库测试，确认基本认证能通
5. 再尝试用 python-jenkins 库连接
