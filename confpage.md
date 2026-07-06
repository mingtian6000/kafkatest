```
# 看看有几个 classic 要删
rabbitmqctl list_queues -p / name type --no-table-headers | awk '$2=="classic"' | wc -l

# 列名字核对一遍
rabbitmqctl list_queues -p / name type --no-table-headers | awk '$2=="classic"'
