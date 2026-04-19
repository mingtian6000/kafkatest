```
# 使用只读模式，防止误操作
claude mcp add jenkins -- npx jenkins-mcp \
  --jenkins-url https://jenkins.example.com \
  --jenkins-username your-username \
  --jenkins-password your-api-token \
  --read-only
