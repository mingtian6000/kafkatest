```
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-server-config
  namespace: monitoring  # 请根据您的实际命名空间修改
  labels:
    app: prometheus
    component: server
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
      evaluation_interval: 15s
      external_labels:
        cluster: 'production'
        source: 'prometheus-server'

    # 远程写入配置 - 启用 remote write
    remote_write:
      - url: "http://prometheus-server:9090/api/v1/write"  # Prometheus 自身的远程写入端点
        # 可以添加其他远程存储地址，例如：
        # - url: "http://thanos-receive:10908/api/v1/receive"  # Thanos Receive
        # - url: "http://cortex-distributor:9009/api/prom/push"  # Cortex
        # - url: "https://prometheus-remote-storage.gcp.com/api/v1/write"  # GCP Managed Service
        
        # 远程写入队列配置（优化性能）
        queue_config:
          # 每个分片的最大样本数（默认 10000）
          capacity: 10000
          # 每次发送的最大样本数（默认 1000）
          max_samples_per_send: 1000
          # 批处理超时时间（默认 5s）
          batch_send_deadline: "5s"
          # 最大重试次数（默认 3）
          max_retries: 3
          # 最小退避时间（默认 100ms）
          min_backoff: "100ms"
          # 最大退避时间（默认 5s）
          max_backoff: "5s"
          # 最大分片数（默认 200）
          max_shards: 200
          # 最小分片数（默认 1）
          min_shards: 1
        
        # 重新标记配置（可选）
        write_relabel_configs:
          # 只保留 k6 相关的指标
          - source_labels: [__name__]
            regex: '(k6_.*|prometheus_.*|up)'
            action: keep
          # 添加额外的标签
          - target_label: "data_source"
            replacement: "k6_performance_test"
        
        # 如果需要认证，取消注释以下部分
        # basic_auth:
        #   username: "your_username"
        #   password: "your_password"
        # bearer_token: "your_bearer_token_here"
        # bearer_token_file: "/path/to/bearer/token/file"
        
        # TLS 配置（如果需要）
        # tls_config:
        #   insecure_skip_verify: false
        #   ca_file: "/path/to/ca.crt"
        #   cert_file: "/path/to/client.crt"
        #   key_file: "/path/to/client.key"
        
        # 代理配置（如果需要）
        # proxy_url: "http://proxy.example.com:8080"
        
        # 远程写入超时（默认 30s）
        remote_timeout: "30s"
        
        # 元数据配置
        metadata_config:
          # 是否发送元数据（默认 true）
          send: true
          # 发送间隔（默认 1m）
          send_interval: "1m"
        
        # 示例限制（可选）
        # sigv4: {}  # AWS SigV4 签名（用于 AWS Managed Service）

    # 远程读取配置（可选）
    remote_read:
      - url: "http://prometheus-server:9090/api/v1/read"
        read_recent: true

    # 抓取配置
    scrape_configs:
      # Prometheus 自身监控
      - job_name: 'prometheus'
        static_configs:
          - targets: ['localhost:9090']
      
      # 节点监控
      - job_name: 'node-exporter'
        static_configs:
          - targets: ['node-exporter:9100']
      
      # k6 指标抓取（如果需要从 k6 直接抓取）
      - job_name: 'k6-metrics'
        static_configs:
          - targets: ['k6-service:5656']  # k6 指标暴露端口
        scrape_interval: 5s
        metrics_path: '/metrics'
        
      # 其他应用监控
      - job_name: 'your-application'
        static_configs:
          - targets: ['app-service:8080']
        metrics_path: '/actuator/prometheus'
        
    # 告警规则配置
    rule_files:
      - /etc/prometheus/rules/*.yml
      
    # 告警管理器配置
    alerting:
      alertmanagers:
        - static_configs:
            - targets: ['alertmanager:9093']
              
  # 告警规则文件（示例）
  k6-alerts.yml: |
    groups:
      - name: k6_performance_alerts
        rules:
          # k6 成功率告警
          - alert: K6HighErrorRate
            expr: rate(k6_http_req_failed[5m]) > 0.05
            for: 2m
            labels:
              severity: warning
              service: k6
            annotations:
              summary: "k6 测试错误率过高"
              description: "k6 测试在过去5分钟内的错误率超过5% (当前值: {{ $value }})"
              
          # k6 响应时间告警
          - alert: K6HighResponseTime
            expr: histogram_quantile(0.95, rate(k6_http_req_duration_seconds_bucket[5m])) > 1
            for: 2m
            labels:
              severity: warning
              service: k6
            annotations:
              summary: "k6 测试响应时间过高"
              description: "k6 测试的95%分位响应时间超过1秒 (当前值: {{ $value }}秒)"
              
          # k6 活动虚拟用户告警
          - alert: K6HighVUCount
            expr: k6_vus > 100
            for: 1m
            labels:
              severity: info
              service: k6
            annotations:
              summary: "k6 高并发虚拟用户"
              description: "k6 测试虚拟用户数超过100 (当前值: {{ $value }})"
