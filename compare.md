Architectural Comparison: Cloud SQL Auth Proxy Modes

Scenario: Multiple application teams (e.g., Airflow DAGs, Python scripts) need to connect to a Cloud SQL instance. Two primary proxy deployment models are under consideration.

1. Cloud SQL Auth Proxy (Local/Binary Mode)

This is the standard, Google-recommended method where the proxy runs as a process alongside the application.

* Deployment Model: A standalone binary. It is installed directly on the host machine (e.g., Airflow worker node) and run as a process or daemon.
* Connection Endpoint: Applications connect to 
"localhost:5432" (or 
"127.0.0.1").
* Operational Overhead: Low. No OS management, stateless, and lifecycle is tied to the application host.
* Availability: High. Each application/worker has its own independent connection path. The failure of one does not affect others.
* Network Performance: Excellent. Communication is via the local loopback interface, introducing minimal latency.
* Security Model: Uses IAM for authentication. No need to whitelist IPs; the connection is automatically encrypted (TLS) via the proxy's secure tunnel.
* Cost: Negligible. No additional infrastructure costs.
* Primary Use Case:
   * Your exact scenario: Airflow DAGs, Python scripts, microservices, and CI/CD pipelines.
   * Any application where the proxy can be deployed on the same host or as a sidecar container.

2. Cloud SQL Auth Proxy (Dedicated VM / "Bastion" Mode)

A centralized model where a single VM instance runs the proxy, and all applications route their traffic through it.

* Deployment Model: A Compute Engine (GCE) Virtual Machine. The proxy binary runs on this dedicated VM.
* Connection Endpoint: Applications connect to the VM's Internal IP address (e.g., 
"10.0.0.5:5432").
* Operational Overhead: High. Requires full VM management: OS updates, security patches, monitoring, firewall rules, and logging.
* Availability: Low (Single Point of Failure). If the VM stops, restarts, or encounters issues, all dependent applications lose database connectivity.
* Network Performance: Good. Traffic traverses the internal VPC network, adding a small hop and latency compared to localhost.
* Security Model: Relies on VPC firewall rules. The Cloud SQL instance must whitelist the VM's internal (or external) IP address. IAM credentials are still used by the proxy on the VM.
* Cost: Ongoing. You incur the cost of running the GCE instance 24/7.
* Primary Use Case:
   * Legacy applications that cannot be modified to use a new connection string.
   * Scenarios requiring a single, fixed egress IP for external auditing or third-party whitelisting requirements.
   * (Generally not recommended for new architectures connecting from within GCP).

Recommendation for Your Airflow Environment

Use the Local/Binary Mode. It is the superior, cloud-native design for your use case.

Reasoning:

1. Simplicity & Fit: It perfectly matches the architecture of Airflow. You deploy the proxy once on each Airflow Worker (e.g., as a 
"systemd" service). DAGs then simply connect to 
"localhost", requiring no code changes.
2. Eliminates SPOF: The dedicated VM is a critical risk. Its failure would halt all data pipelines. The local model distributes this risk.
3. Reduces Overhead: You avoid the ongoing burden of securing, patching, and monitoring a production VM. The proxy is just a lightweight binary.

Proposed Implementation:

1. Install the Cloud SQL Auth Proxy binary on all Airflow Worker nodes.
2. Configure it as a system service to auto-start and listen on 
"127.0.0.1:5432".
3. Update your DAG connection strings to point to 
"host: 127.0.0.1".

This provides a secure, performant, and resilient connection pattern without the complexity and fragility of a shared proxy VM.


这个问题问得非常核心，直接关系到 K8s 负载均衡的底层逻辑。简单来说，Kubernetes 默认的流量分配是“轮询”，但最终落到哪个 Pod 上，是由操作系统内核决定的，而不是 K8s 自己。

下面我帮你拆解一下这背后的“黑盒”机制，以及为什么会出现你看到的“CPU 不均”现象。

1. 流量是怎么分到 Pod 的？（核心机制）

K8s 的流量分发其实分两步走：

* 第一步：K8s 的“轮询”：Kubernetes 的 Service 对象（比如 
"ClusterIP"）背后有一个 
"Endpoints" 列表，里面记录了所有健康 Pod 的 IP。当流量到达 Service 时，K8s 的 
"kube-proxy" 组件会维护一个规则，默认采用轮询（Round Robin） 算法，将请求依次分发给这 5 个 Pod 的 IP。
* 第二步：操作系统的“连接跟踪”：当流量到达节点（Node）时，Linux 内核的 
"conntrack"（连接跟踪）模块会接管。它有一个核心机制：对于同一个客户端 IP 和端口发来的请求，为了保持会话一致性，内核会倾向于将其发往同一个 Pod。

结论：虽然 K8s 想“雨露均沾”，但操作系统为了效率，可能会把来自同一个压力测试工具的流量（源 IP 固定）都扔给同一个 Pod。

2. 为什么 CPU 会“贫富不均”？

结合你的场景，最可能的原因是：

* 压力测试工具（如 JMeter）的“连接复用”：性能测试工具为了减少 TCP 握手开销，通常会复用长连接。如果测试脚本配置了连接池，且只用了少量线程/连接，那么这些连接在 
"conntrack" 看来就是“已建立的连接”，会被内核直接路由到最初建立连接的那个 Pod。
* 结果：这就导致一个 Pod 在“负重前行”，扛了大部分压力；而其他 4 个 Pod 在“岁月静好”，只处理零星的新连接。

3. 怎么解决？（对症下药）

既然知道了是“连接复用”导致的内核粘滞，解决方案就是打破这种粘滞：

1. 调整测试工具配置（推荐）：
   * 在 JMeter 或你的测试脚本中，关闭 HTTP 连接复用（例如在 JMeter 的 HTTP 请求中勾选 “Use KeepAlive” 的相反选项）。
   * 或者，增加并发线程数，让测试工具建立大量不同的 TCP 连接，这样内核才会被迫进行轮询。
2. 修改 Service 类型（治本）：
   * 将 Service 的 
"sessionAffinity" 设置为 
"None"（默认就是，但确认一下）。
   * 如果问题依然存在，可以考虑使用 IPVS 代理模式（替代默认的 iptables）。IPVS 提供了更丰富的负载均衡算法（如最少连接数），能更好地在 Pod 间分配负载。但这需要修改 
"kube-proxy" 的启动参数。

建议：先从调整测试工具入手，这是最快、最直接的验证方法。如果调整后 5 个 Pod 的 CPU 都均匀地升到了 70%-80%，那就说明问题解决了。