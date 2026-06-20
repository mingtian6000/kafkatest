```
这是个非常好的观察，根子在于 GKE Cluster Autoscaler 对 Spot node pool 用了不同的 zone 分配策略。

核心原因：
"location_policy" 默认值不一样

GKE 从 1.24.1-gke.800 开始，给 Cluster Autoscaler 引入了一个叫 
"location_policy"（位置策略）的参数，而它的默认值在 Spot 和标准节点池上是不一样的：

Node Pool 类型 默认 
"location_policy" 行为
Standard（非抢占式） 
"BALANCED" 尽量把节点均匀铺在 A / B / C，兼顾各 zone 容量和 Pod 的 zone affinity
Spot / Preemptible 
"ANY" 不保证均匀，autoscaler 会优先往当前有剩余容量/库存充裕的那个 zone塞节点

所以你看到的"Spot 节点全挤在一个 zone 里"，不是 bug，而是 
"ANY" 策略的设计行为。

为什么 Spot 的默认是 
"ANY" 而不是 
"BALANCED"？

两个硬核原因：

1. Spot 库存（capacity）在各 zone 是不均匀的

Spot VM 本质上就是 Google 把闲置容量拿出来低价卖，各 zone 的剩余库存实时波动很大。某个时刻可能 zone-b 有一大把 e2-standard-4 闲着，zone-c 已经没库存了。Autoscaler 如果强行走 
"BALANCED"，就会在 zone-c 反复尝试创建 → 失败 → 重试，反而拖慢调度。
"ANY" 的做法是：哪有货就在哪扩，先把 Pod 跑起来。

2. Spot 本身就是"可中断"的，跨 zone 均匀分布的 HA 语义不一样

Standard 节点你期望"zone 故障不影响服务"，所以要均匀铺。Spot 节点本身随时可能被抢占（preempt），节点消失是正常预期，所以 autoscaler 的首要目标是"尽快找到能提供的容量"，而不是"美学上的均匀"。

结果就是：一旦 zone-b 有稳定库存，一连串 scale-up 事件都落在 zone-b，zone-a/c 就空着——看起来就像"全挤一块了"。

怎么让它均匀铺开？

如果你确实需要 Spot 节点跨 zone 分布（比如你的无状态 batch 虽然能用 Spot，但你还是希望 zone 故障时不会全军覆没），可以手动把 policy 改成 
"BALANCED"：

gcloud container node-pools update YOUR_POOL_NAME \
  --cluster=YOUR_CLUSTER \
  --location=YOUR_REGION \
  --location-policy=BALANCED

或者在创建 node pool 时就指定：

gcloud container node-pools create spot-pool \
  --cluster=YOUR_CLUSTER \
  --location=YOUR_REGION \
  --spot \
  --location-policy=BALANCED \
  --node-locations=us-central1-a,us-central1-b,us-central1-c \
  --enable-autoscaling --min-nodes=0 --max-nodes=20

⚠️ 但要注意：
"BALANCED" + Spot 的代价是某些 zone 库存不足时，对应的节点可能扩不出来，部分 Pod 会 Pending 更久。

一句话总结

Standard 默认 
"BALANCED" → 均匀跨 A/B/C；Spot 默认 
"ANY" → 追着有货的 zone 扩 → 看起来全堆一个 zone。 这是 GKE autoscaler 的有意设计，不是配置错误。想要均匀就手动切 
"--location-policy=BALANCED"，但要接受 Spot 库存不均带来的扩缩延迟风险。
如果你的场景是纯 batch / CI runner，其实全在一个 zone 也问题不大；但如果是面向用户的无状态服务混用 Spot，建议还是 
"BALANCED" + 一个小的标准 node pool 兜底。