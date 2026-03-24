'''
pipeline {
    agent any
    parameters {
        choice(
            name: 'STALE_DAYS',
            choices: ['30', '60', '90', '180'],
            description: '设置判定为陈旧分支的天数阈值'
        )
    }
    triggers {
        // 每两天的凌晨2点运行
        cron('H 2 */2 * *')
    }
    environment {
        GITHUB_TOKEN = credentials('github-token')
        GITHUB_OWNER = 'your-org-name'
        GITHUB_REPO = 'your-repo-name'
    }
    stages {
        stage('检出分支信息') {
            steps {
                script {
                    // 确保有 gh CLI
                    sh '''
                        gh auth status || gh auth login --with-token <<< "$GITHUB_TOKEN"
                    '''
                    // 运行检测脚本
                    sh '''
                        # 使用之前的脚本逻辑，但添加日期筛选
                        ./fetch_stale_branches.sh "$GITHUB_OWNER" "$GITHUB_REPO" "$STALE_DAYS"
                    '''
                }
            }
        }
        stage('分析并生成报告') {
            steps {
                script {
                    // 生成 HTML 格式的摘要报告
                    sh '''
                        python generate_html_report.py stale_branches.csv
                    '''
                }
            }
        }
    }
    post {
        always {
            // 1. 存档 CSV 报告
            archiveArtifacts artifacts: 'stale_branches.csv, branch_report.html'
            // 2. 将报告作为邮件附件发送
            emailext(
                to: 'team@your-company.com',
                subject: "[Jenkins] ${env.JOB_NAME} - 陈旧分支报告",
                body: "请查收最新的陈旧分支报告。\n\n检出链接: ${env.BUILD_URL}",
                attachmentsPattern: 'stale_branches.csv,branch_report.html'
            )
        }
    }
}

'''
#!/bin/bash
# fetch_stale_branches.sh
OWNER=$1
REPO=$2
STALE_DAYS=$3
GITHUB_TOKEN=$4

# 计算截止日期
CUTOFF_DATE=$(date -d "$STALE_DAYS days ago" +%Y-%m-%dT%H:%M:%SZ)

# 获取所有分支
curl -s -H "Authorization: token $GITHUB_TOKEN" \
  "https://api.github.com/repos/$OWNER/$REPO/branches" \
  | jq -r '.[] | [.name, .commit.commit.author.date, .commit.commit.author.name] | @csv' \
  > all_branches.csv

# 筛选出陈旧分支
awk -v cutoff="$CUTOFF_DATE" -F, '
BEGIN {OFS=","; print "branch,last_commit,committer,stale_days"}
{
  if ($2 < cutoff) {
    split($2, d, "T");
    cmd="date -d \"" d[1] "\" +%s";
    cmd | getline last_sec; close(cmd);
    "date +%s" | getline now; close("date +%s");
    days=int((now - last_sec) / 86400);
    print $1, $2, $3, days
  }
}' all_branches.csv > stale_branches.csv
