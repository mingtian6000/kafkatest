```
# 看看有几个 classic 要删
rabbitmqctl list_queues -p / name type --no-table-headers | awk '$2=="classic"' | wc -l

# 列名字核对一遍
rabbitmqctl list_queues -p / name type --no-table-headers | awk '$2=="classic"'
# 默认 vhost /
rabbitmqctl list_queues -p / name type --no-table-headers \
  | awk '$2 == "classic" {print $1}' \
  | xargs -n1 rabbitmqctl delete_queue --force

# 指定 vhost
rabbitmqctl list_queues -p <vhost> name type --no-table-headers \
  | awk '$2 == "classic" {print $1}' \
  | xargs -n1 rabbitmqctl delete_queue --force
