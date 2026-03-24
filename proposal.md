好的，你本地有 
"gh" 和 
"jq" 就足够了。这是一个完整、可直接运行的命令，它会为你生成一个标准的 CSV 文件。

第一步：确保工具就绪

确认你已安装：

# 检查 GitHub CLI
gh --version
# 检查 JSON 处理工具
jq --version

第二步：运行完整命令生成 CSV

将下面的 
"<YOUR_ORG>" 和 
"<YOUR_REPO>" 替换为你的组织名和仓库名。

# 单行命令（方便复制粘贴）
gh api graphql -f query='
query($owner: String!, $name: String!) {
  repository(owner: $owner, name: $name) {
    refs(refPrefix: "refs/heads/", first: 100) {
      nodes {
        name
        target {
          ... on Commit {
            committedDate
            author {
              name
            }
          }
        }
      }
    }
  }
}' -f owner=<YOUR_ORG> -f name=<YOUR_REPO> | jq -r '
  .data.repository.refs.nodes[] |
  [
    .name,
    .target.committedDate,
    (.target.author.name // "UNKNOWN")
  ] | @csv
' > branch_report.csv

执行完毕后，当前目录下就会生成 
"branch_report.csv" 文件。

第三步：解释与自定义

- 输出列：文件包含三列：
"分支名", 
"最后提交时间(ISO格式)", 
"最后提交者"。
- 处理 100+ 分支：默认 
"first: 100"，如果你的分支超过 100 个，需要处理分页。完整的脚本如下（保存为 
"fetch_branches.sh" 并运行）：

#!/bin/bash
OWNER="<YOUR_ORG>"
REPO="<YOUR_REPO>"
TEMP_FILE="temp_branches.json"
OUTPUT_FILE="branch_report.csv"

echo "branch_name,last_commit_date,last_committer" > "$OUTPUT_FILE"

cursor=""
has_next_page=true

while [ "$has_next_page" = true ]; do
  gh api graphql -f query='
  query($owner: String!, $name: String!, $cursor: String) {
    repository(owner: $owner, name: $name) {
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
              author { name }
            }
          }
        }
      }
    }
  }' -f owner="$OWNER" -f name="$REPO" -f cursor="$cursor" > "$TEMP_FILE"

  jq -r '
    .data.repository.refs.nodes[] |
    [
      .name,
      .target.committedDate,
      (.target.author.name // "UNKNOWN")
    ] | @csv
  ' "$TEMP_FILE" >> "$OUTPUT_FILE"

  has_next_page=$(jq -r '.data.repository.refs.pageInfo.hasNextPage' "$TEMP_FILE")
  cursor=$(jq -r '.data.repository.refs.pageInfo.endCursor' "$TEMP_FILE")
done

rm -f "$TEMP_FILE"
echo "报告已生成: $OUTPUT_FILE"

运行脚本：

chmod +x fetch_branches.sh
./fetch_branches.sh

注意事项

1. 权限：确保你已用 
"gh auth login" 登录，且有仓库的读取权限。
2. Owner 定义：这里记录的“author.name”是 最后提交该分支的人，这通常被认为是该分支当前的责任人。Git 不原生记录“分支创建者”。

运行脚本后，用 Excel 或 Numbers 打开 CSV 文件，即可清晰地看到所有分支的状态。你可以按“最后提交时间”排序，轻松找出不活跃（stale）的分支。