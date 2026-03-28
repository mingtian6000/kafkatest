```
jq -s '
if length > 0 then
  "<h2>🔍 Stale Branches Detected</h2>The following branches have not been updated for more than '"${DAYS_THRESHOLD}"' days:<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\" style=\"border-collapse: collapse; font-family: Arial, sans-serif; width: 100%;\">Branch NameLast CommitDays Ago" +
  (map("" + .branch + "" + (.last_commit | .[0:10]) + "" + (.days_ago | tostring) + "") | join("")) +
  "<em>Scan Time: '$(date +"%Y-%m-%d %H:%M:%S")'</em>"
else
  "<h2>✅ No Stale Branches Found</h2>All non-protected branches are within the threshold of '"${DAYS_THRESHOLD}"' days.<em>Scan Time: '$(date +"%Y-%m-%d %H:%M:%S")'</em>"
end'
