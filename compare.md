```
SELECT
  resource.labels.instance_name AS node_name,
  MIN(IF(protoPayload.methodName = 'compute.instances.start', timestamp, NULL)) AS vm_started_at,
  MAX(IF(protoPayload.methodName IN ('compute.instances.delete','compute.instances.preempted'), timestamp, NULL)) AS vm_stopped_at,
  ARRAY_AGG(DISTINCT protoPayload.methodName) AS actions
FROM `projects/YOUR_PROJECT_ID/logs/cloudaudit_googleapis_com_system_event`
WHERE
  resource.type = 'gce_instance'
  AND resource.labels.project_id = 'YOUR_PROJECT_ID'
  AND protoPayload.methodName IN (
    'compute.instances.start',
    'compute.instances.delete',
    'compute.instances.preempted'
  )
  AND timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 14 DAY)
GROUP BY node_name
ORDER BY vm_started_at DESC
