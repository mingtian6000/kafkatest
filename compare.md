# 1. 看 Pod 实际从 metadata server 拿到的身份
curl -s -H "Metadata-Flavor: Google" \
  http://169.254.169.254/computeMetadata/v1/instance/service-accounts/default/email
