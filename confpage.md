```
下面这套做法就是干一件事：把"画大饼（requests）"和"真吃多少（usage）"放同一张表，老板一秒看出哪些 Workload 虚胖、cluster 是不是被 request 撑大而不是真的用完。

前置：确认能拿到 usage（否则先装 metrics-server）

kubectl top nodes 2>/dev/null | head
kubectl top pods -A 2>/dev/null | head

如果提示 
"Metrics API not available"，装/确认 metrics-server（GKE 一般自带）：

kubectl get apiservices v1beta1.metrics.k8s.io -o jsonpath='{.status.conditions[-1].message}'

1）Cluster 全局：Requests vs Real Usage（老板最关心的"要不要扩 node"）

1.1 算"被分配走的 requests"（所有 Pod 的 request 总和）

kubectl get pods -A -o json |
  jq '[.items[]
    | select(.spec.nodeName!=null)               # 只看已调度
    | .spec.containers[]
    | {
        cpu: (.resources.requests.cpu // "0"),
        mem: (.resources.requests.memory // "0")
      }
  ]
  | reduce .[] as $r (
      {cpu_nano:0, mem_bytes:0};
      .cpu_nano += ($r.cpu  | ltrimstr("m") | (if ($r.cpu|endswith("m")) then . else .*1000 end)),
        .mem_bytes += ($r.mem | ltrimstr("Ki") | ltrimstr("Mi") | ltrimstr("Gi") |
          (if ($r.mem|endswith("Ki")) then (.[:-2]|tonumber)*1024
           elif ($r.mem|endswith("Mi")) then (.[:-2]|tonumber)*1048576
           elif ($r.mem|endswith("Gi")) then (.[:-2]|tonumber)*1073741824
           else ($r.mem|tonumber) end))
  )
  | "Total requested CPU ≈ \(.cpu_nano/1000) cores   (\(.cpu_nano) millicores)\nTotal requested Memory ≈ \(.mem_bytes/1048576|floor) Mi"'

如果你不想看 jq 恐怖语法，下面有更实用的简化版（用 top + requests 分开看），老板更容易读。

1.2 实际正在吃多少（Real Usage）

# 节点实际用量
kubectl top nodes --no-headers | awk '{printf "%-30s CPU=%s MEM=%s\n", $1, $2, $3}'

# Pod 用量汇总（等价于节点汇总）
kubectl top pods -A --no-headers \
  | awk '{cpu+=$3; mem+=$4}
     END{
       printf "Pods actual CPU≈%s core-units  Mem≈%s Mi-units (raw sum)\n", cpu/1000, mem/1024/1024
     }'

注意：
"kubectl top" 的单位可能是 
"m"/
"Mi"，上面只是示意方向；你要精确的话我给你一套单位标准化 awk（你告诉我 
"top pods" 输出格式我帮你对齐）。

2）老板视角：直接列出「虚胖 Deployment」（request >> usage）

这是你要的一眼看懂版本 👇

逻辑：对每个 Deployment，把它名下 Pods 的 CPU/MEM request 合计 vs top 实际 usage 合计拉出来。

2.1 先把「deployment → pods」关系建出来（稳妥法）

#!/usr/bin/env bash
set -euo pipefail

OUT=$(mktemp)
echo -e "DEPLOYMENT\tNS\tREPLICAS\tREQ_CPU(m)\tTOP_CPU(m)\tREQ_MEM(Mi)\tTOP_MEM(Mi)\tWASTE_FLAG" > "$OUT"

for DEP in $(kubectl get deploy -A -o json | jq -r '.items[] | "\(.metadata.namespace)/\(.metadata.name)"' | sort); do
  NS="${DEP%/*}"
  NAME="${DEP##*/}"

  REPLICAS=$(kubectl get deploy -n "$NS" "$NAME" -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "?")

  # ---- Request 汇总（deploy 的 template，一般够了；严格可改算 pods）----
  REQ_CPU_M=$(kubectl get deploy -n "$NS" "$NAME" -o json \
    | jq '[.spec.template.spec.containers[].resources.requests.cpu//"0m"] 
          | map(if endswith("m") then (ltrimstr("m")|tonumber) else tonumber*1000 end)
          | add')

  REQ_MEM_Mi=$(kubectl get deploy -n "$NS" "$NAME" -o json \
    | jq '[.spec.template.spec.containers[].resources.requests.memory//"0Mi"]
          | map(
              if endswith("Ki") then (ltrimstr("Ki")|tonumber)*1024/1048576
              elif endswith("Mi") then (ltrimstr("Mi")|tonumber)
              elif endswith("Gi") then (ltrimstr("Gi")|tonumber)*1024
              else 0 end
            )
          | add|floor')

  # ---- Actual Usage（pod 级别汇总）----
  TOP_CPU_m=$( (kubectl top pods -n "$NS" --no-headers 2>/dev/null || true) \
    | awk -v dep="$NAME" '$1 ~ "^"dep"-[a-z0-9]+-[a-z0-9]+$" {gsub(/m/,"",$3); s+=$3} END{print s+0}')
  TOP_MEM_Mi=$( (kubectl top pods -n "$NS" --no-headers 2>/dev/null || true) \
    | awk -v dep="$NAME" '$1 ~ "^"dep"-[a-z0-9]+-[a-z0-9]+$" {gsub(/Ki/,"",$4); s+=$4} END{printf "%.0f", s/1024}')

  # 简单 waste flag
  WASTE=""
  if [[ -n "$TOP_CPU_m" && "$TOP_CPU_m" -gt 0 ]]; then
    RATIO=$(( REQ_CPU_M / (TOP_CPU_m + 1) ))
    (( RATIO > 3 )) && WASTE="⚠️ CPU虚胖(${RATIO}x)"
  fi

  echo -e "${NAME}\t${NS}\t${REPLICAS}\t${REQ_CPU_M}\t${TOP_CPU_m}\t${REQ_MEM_Mi}\t${TOP_MEM_Mi}\t${WASTE}" >> "$OUT"
done

column -t -s $'\t' "$OUT" | tee cluster_resource_waste.txt
echo "✅ 已输出到 cluster_resource_waste.txt"

运行完你会得到一张表：

DEPLOYMENT     NS      REPLICAS  REQ_CPU(m)  TOP_CPU(m)  REQ_MEM(Mi)  TOP_MEM(Mi)  WASTE_FLAG
api-server     prod    3         3000        420         4096         680          ⚠️ CPU虚胖(7x)
worker         prod    6         6000        5800        8192         6100
cache-refresh  batch   2         2000        80          2048         120          ⚠️ CPU虚胖(25x)

3）最"老板友好"的一页总结：画两张数

A. 节点层（证明"node size 没必要加"）

kubectl top nodes

老板看的就是：

- Allocatable vs Usage 差距大不大
- Requests 把 node 填满了，但 
"top" 里 CPU/Memory 根本没满 → 调度拥挤 ≠ 资源真不够

B. 你补一句定性结论（关键）

"我们现在看到的容量压力，主要是 requests 画的大饼把节点塞满（Over-provisioned requests），不是实际 workload 把 CPU/RAM 吃完。把 request 按真实 usage+P99 收敛后，同样规格的 node pool 能扛更多，不需要加 size。"

4）如果你只想最快的一条命令（不看脚本）

# 把"谁的 request 明显大于 usage"先筛出来（pod 级，快）
kubectl top pods -A --no-headers 2>/dev/null \
  | while read ns pod cpu mem; do
      req_cpu=$(kubectl get pod -n "$ns" "$pod" -o json \
        | jq '[.spec.containers[].resources.requests.cpu//"0m"]
              | map(if endswith("m") then (ltrimstr("m")|tonumber) else tonumber*1000 end)|add')
      echo -e "$ns/$pod\treq_cpu=${req_cpu}m\ttop_cpu=$cpu\ttop_mem=$mem"
    done | sort -t= -k2 -nr | head -20

给你一个经验阈值（写进报告很好用）

比值 含义

"request / actual_usage" > 3~5x 大概率虚胖，request 给保守估计过头

"request / actual_usage" ≈ 1~2x 合理余量
actual usage 长期靠近 request 才是真的需要谈扩容/right-size

如果你愿意把下面两行输出原样贴我（脱敏即可），我能把上面的脚本给你改成一行都不用手动算单位的干净版：

1. 
"kubectl top pods -A --no-headers | head -5"
2. 
"kubectl get deploy -n <某一个ns> <某一个名字> -o yaml | grep -A5 requests:"

Here's a clear English paragraph that captures your intent:

Background:

In our lower environments (SIT/UAT), cost efficiency is prioritized over high availability. The GKE clusters in these environments are provisioned with multiple nodes, but we deliberately avoid using node affinity or anti-affinity rules to spread replicas across different nodes. Instead, we prefer to schedule all replicas of a deployment onto the same node whenever possible. For example, a deployment with 2–3 replicas will be packed onto one node until its resources are exhausted, and only then will a second node be brought into use. This compact scheduling strategy significantly reduces the number of active nodes required, thereby lowering infrastructure costs in SIT and UAT without compromising basic functional testing.

Feel free to adjust the tone or specifics. Let me know if you need it shorter or more formal.