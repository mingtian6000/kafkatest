#!/bin/bash

OWNER="你的用户名"
REPO="你的仓库名"
DAYS_THRESHOLD=60

# 计算目标时间戳
TARGET_TIMESTAMP=$(date -d "$DAYS_THRESHOLD days ago" +%s)

# 执行 GraphQL 查询
gh api graphql -f query='
query($owner: String!, $repo: String!, $cursor: String) {
  repository(owner: $owner, name: $repo) {
    refs(refPrefix: "refs/heads/", first: 100, after: $cursor) {
      pageInfo {
        hasNextPage
        endCursor
      }
      nodes {
        name
        target {
          ... on Commit {
            committedDate
            oid
          }
        }
      }
    }
    branchProtectionRules(first: 10) {
      nodes {
        pattern
      }
    }
  }
}' -f owner="$OWNER" -f repo="$REPO" \
--paginate \
--jq '
  .data.repository as $repo |
  $repo.branchProtectionRules.nodes[].pattern as $pat |
  $pat |
  reduce $repo.refs.nodes[] as $branch ({};
    .[$branch.name] = {
      protected: (reduce $repo.branchProtectionRules.nodes[] as $rule (false;
        if (. == false) then
          ($branch.name | test(($rule.pattern | gsub("\\*"; ".*"))) // false)
        else .
        end
      )),
      date: $branch.target.committedDate
    }
  )' \
| jq --arg threshold "$TARGET_TIMESTAMP" '
  to_entries[] | 
  select(.value.protected == false) |
  select(.value.date != null) |
  (.value.date | sub("\\..*Z$"; "Z") | fromdateiso8601) as $date_ts |
  select($date_ts < ($threshold | tonumber)) |
  {
    branch: .key,
    last_commit: .value.date,
    days_ago: (now - $date_ts) / 86400 | floor
  }' \
| jq -s '
  if length > 0 then
    "找到以下陈旧分支:\n" + 
    (map("  - \(.branch) (最后提交: \(.last_commit | .[0:10]), 闲置: \(.days_ago) 天)") | join("\n"))
  else
    "✅ 没有超过阈值的非保护陈旧分支"
  end'
