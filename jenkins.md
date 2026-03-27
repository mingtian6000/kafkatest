```
#!/bin/bash

# 配置参数
REPO_OWNER="你的用户名/组织名"
REPO_NAME="你的仓库名"
DAYS_THRESHOLD=60

echo "=== 开始扫描超过 ${DAYS_THRESHOLD} 天的非保护分支 ==="

# 1. 获取所有保护规则的模式
PROTECTED_PATTERNS=$(
  gh api repos/$REPO_OWNER/$REPO_NAME/branches/protection/rules \
    --jq '.[].pattern' 2>/dev/null || echo ""
)

# 2. 获取所有分支，过滤掉受保护的分支
STALE_BRANCHES=$(
  gh api --paginate repos/$REPO_OWNER/$REPO_NAME/branches \
    --jq '.[] | {name: .name, commit: .commit.sha}' | \
  while IFS= read -r branch_info; do
    # 解析分支名
    BRANCH_NAME=$(echo "$branch_info" | jq -r '.name')
    COMMIT_SHA=$(echo "$branch_info" | jq -r '.commit')
    
    # 检查是否匹配任何保护规则
    IS_PROTECTED=false
    for pattern in $PROTECTED_PATTERNS; do
      # 将 * 转换为 .* 用于正则匹配
      REGEX_PATTERN=$(echo "$pattern" | sed 's/\*/.*/g')
      if [[ "$BRANCH_NAME" =~ ^$REGEX_PATTERN$ ]]; then
        IS_PROTECTED=true
        echo "[跳过] 分支 '$BRANCH_NAME' 匹配保护规则: $pattern" >&2
        break
      fi
    done
    
    if [ "$IS_PROTECTED" = "true" ]; then
      continue
    fi
    
    # 获取最后提交时间
    COMMIT_DATE=$(gh api repos/$REPO_OWNER/$REPO_NAME/commits/$COMMIT_SHA \
      --jq '.commit.author.date' 2>/dev/null)
    
    if [ -n "$COMMIT_DATE" ]; then
      # 转换为 Unix 时间戳
      COMMIT_TIMESTAMP=$(date -d "$COMMIT_DATE" +%s 2>/dev/null)
      CURRENT_TIMESTAMP=$(date +%s)
      DAYS_AGO=$(( (CURRENT_TIMESTAMP - COMMIT_TIMESTAMP) / 86400 ))
      
      if [ "$DAYS_AGO" -gt "$DAYS_THRESHOLD" ]; then
        echo "$BRANCH_NAME|$COMMIT_DATE|$DAYS_AGO"
      fi
    fi
  done
)

# 3. 输出结果
echo ""
if [ -z "$STALE_BRANCHES" ]; then
  echo "✅ 没有找到超过 ${DAYS_THRESHOLD} 天的非保护分支"
else
  echo "⚠️  找到以下需要清理的分支："
  echo "========================================"
  printf "%-30s | %-20s | %s\n" "分支名" "最后提交" "闲置天数"
  echo "----------------------------------------"
  echo "$STALE_BRANCHES" | while IFS='|' read -r name date days; do
    printf "%-30s | %-20s | %d 天\n" "$name" "${date:0:10}" "$days"
  done
  echo "========================================"
fi
