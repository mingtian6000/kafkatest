´´´
# 在好 VM 上
python3 -m pip install --upgrade pip wheel

# 1. 先锁版（你已经有了 requirements-lock.txt 也行）
python3 -m pip freeze > /tmp/req-lock.txt

# 2. 用 --no-deps 按已安装版本逐个重新 wheel 出来
#    这样 pip 不再去网上重新解析依赖图，只验证「我本地这个版本能不能打成 wheel」
mkdir -p /tmp/wh
python3 -m pip wheel --no-deps -r /tmp/req-lock.txt --wheel-dir=/tmp/wh
