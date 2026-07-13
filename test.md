```
# 列某个 region 下所有 operation，UPGRADE 类型的
gcloud redis operations list \
  --region=<your-region> \
  --filter="metadata.verb=UPGRADE" \
  --format="table(
    name.basename():label=OP,
    metadata.resourceName.split('/').get(5):label=INSTANCE,
    startTime:label=START,
    endTime:label=END,
    done:label=DONE,
    status.message:label=MSG
  )"
