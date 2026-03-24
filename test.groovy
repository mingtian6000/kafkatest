pipeline {
    agent any
    parameters {
        choice(
            name: 'STALE_THRESHOLD',
            choices: ['30', '60', '90'],
            description: 'Days threshold to mark branch as stale'
        )
    }
    triggers {
        cron('H 2 */2 * *')  // 每2天凌晨2点运行
    }
    environment {
        GITHUB_TOKEN = credentials('github-token')
        GITHUB_OWNER = 'your-org'
        GITHUB_REPO = 'your-repo'
        TEAM_EMAIL = 'dev-team@yourcompany.com'
    }
    stages {
        stage('检测陈旧分支') {
            steps {
                script {
                    // 获取分支数据
                    sh '''
                        # 获取所有分支信息
                        gh api graphql -f query='
                        query($owner: String!, $name: String!) {
                          repository(owner: $owner, name: $name) {
                            refs(refPrefix: "refs/heads/", first: 100) {
                              nodes {
                                name
                                target {
                                  ... on Commit {
                                    committedDate
                                    author { name email }
                                  }
                                }
                              }
                            }
                          }
                        }' -f owner="$GITHUB_OWNER" -f name="$GITHUB_REPO" > branches.json
                        
                        # 计算陈旧天数并输出为表格格式
                        python3 << 'EOF'
import json
import datetime
import os
import csv
from datetime import datetime, timezone

# 读取分支数据
with open('branches.json', 'r') as f:
    data = json.load(f)

branches = data['data']['repository']['refs']['nodes']

# 获取阈值
stale_days = int(os.getenv('STALE_THRESHOLD', 60))
now = datetime.now(timezone.utc)

# 准备数据
all_branches = []
stale_branches = []
warning_branches = []

for branch in branches:
    if not branch['target']:
        continue
    
    branch_name = branch['name']
    commit_date_str = branch['target']['committedDate']
    author_name = branch['target']['author']['name'] if branch['target']['author'] else 'Unknown'
    
    # 计算天数
    commit_date = datetime.fromisoformat(commit_date_str.replace('Z', '+00:00'))
    days_ago = (now - commit_date).days
    
    branch_info = {
        'name': branch_name,
        'last_commit': commit_date_str,
        'author': author_name,
        'days_ago': days_ago,
        'status': 'stale' if days_ago > stale_days else ('warning' if days_ago > stale_days/2 else 'active')
    }
    
    all_branches.append(branch_info)
    
    if days_ago > stale_days:
        stale_branches.append(branch_info)
    elif days_ago > stale_days/2:
        warning_branches.append(branch_info)

# 按天数降序排序
all_branches.sort(key=lambda x: x['days_ago'], reverse=True)
stale_branches.sort(key=lambda x: x['days_ago'], reverse=True)
warning_branches.sort(key=lambda x: x['days_ago'], reverse=True)

# 生成 HTML 报告
html_content = '''
<!DOCTYPE html>
<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #333; border-bottom: 2px solid #4CAF50; padding-bottom: 10px; }
        h2 { color: #555; margin-top: 30px; }
        .summary { background: #f9f9f9; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
        .summary-box { display: inline-block; padding: 10px 20px; margin-right: 20px; border-radius: 5px; }
        .total { background: #e3f2fd; }
        .stale { background: #ffebee; color: #c62828; }
        .warning { background: #fff3e0; color: #ef6c00; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th { background: #4CAF50; color: white; padding: 12px; text-align: left; }
        td { padding: 10px; border-bottom: 1px solid #ddd; }
        tr:hover { background: #f5f5f5; }
        .stale-row { background: #ffebee !important; }
        .warning-row { background: #fff3e0 !important; }
        .days-badge { 
            padding: 3px 8px; 
            border-radius: 10px; 
            font-size: 12px; 
            font-weight: bold;
        }
        .days-stale { background: #ffcdd2; color: #c62828; }
        .days-warning { background: #ffcc80; color: #ef6c00; }
        .days-active { background: #c8e6c9; color: #2e7d32; }
        .timestamp { color: #666; font-size: 12px; margin-top: 20px; }
    </style>
</head>
<body>
    <h1>🏷️ GitHub 陈旧分支报告</h1>
    
    <div class="summary">
        <div class="summary-box total">
            <h3>总分支数</h3>
            <p style="font-size: 24px; font-weight: bold;">''' + str(len(all_branches)) + '''</p>
        </div>
        <div class="summary-box stale">
            <h3>陈旧分支 (>''' + str(stale_days) + '''天)</h3>
            <p style="font-size: 24px; font-weight: bold;">''' + str(len(stale_branches)) + '''</p>
        </div>
        <div class="summary-box warning">
            <h3>警告分支 (''' + str(int(stale_days/2)) + '''-''' + str(stale_days) + '''天)</h3>
            <p style="font-size: 24px; font-weight: bold;">''' + str(len(warning_branches)) + '''</p>
        </div>
    </div>
'''

if stale_branches:
    html_content += '''
    <h2>🔴 陈旧分支 (需要立即处理)</h2>
    <table>
        <tr>
            <th>分支名称</th>
            <th>最后提交</th>
            <th>提交者</th>
            <th>闲置天数</th>
        </tr>
    '''
    
    for branch in stale_branches:
        days_class = "days-stale"
        row_class = "stale-row"
        
        html_content += f'''
        <tr class="{row_class}">
            <td><strong>{branch['name']}</strong></td>
            <td>{branch['last_commit']}</td>
            <td>{branch['author']}</td>
            <td><span class="days-badge {days_class}">{branch['days_ago']} 天</span></td>
        </tr>
        '''
    
    html_content += '</table>'

if warning_branches:
    html_content += '''
    <h2>🟡 警告分支 (即将过期)</h2>
    <table>
        <tr>
            <th>分支名称</th>
            <th>最后提交</th>
            <th>提交者</th>
            <th>闲置天数</th>
        </tr>
    '''
    
    for branch in warning_branches:
        days_class = "days-warning"
        row_class = "warning-row"
        
        html_content += f'''
        <tr class="{row_class}">
            <td><strong>{branch['name']}</strong></td>
            <td>{branch['last_commit']}</td>
            <td>{branch['author']}</td>
            <td><span class="days-badge {days_class}">{branch['days_ago']} 天</span></td>
        </tr>
        '''
    
    html_content += '</table>'

# 所有分支表格
html_content += '''
    <h2>📊 全部分支概览</h2>
    <table>
        <tr>
            <th>分支名称</th>
            <th>最后提交</th>
            <th>提交者</th>
            <th>闲置天数</th>
            <th>状态</th>
        </tr>
'''

for branch in all_branches:
    if branch['status'] == 'stale':
        days_class = "days-stale"
        status_text = "🟥 陈旧"
    elif branch['status'] == 'warning':
        days_class = "days-warning"
        status_text = "🟨 警告"
    else:
        days_class = "days-active"
        status_text = "🟩 活跃"
    
    html_content += f'''
    <tr>
        <td>{branch['name']}</td>
        <td>{branch['last_commit']}</td>
        <td>{branch['author']}</td>
        <td><span class="days-badge {days_class}">{branch['days_ago']} 天</span></td>
        <td>{status_text}</td>
    </tr>
    '''

html_content += f'''
    </table>
    
    <div class="timestamp">
        报告生成时间: {now.strftime("%Y-%m-%d %H:%M:%S")} UTC<br>
        陈旧阈值: {stale_days} 天<br>
        仓库: {os.getenv('GITHUB_OWNER')}/{os.getenv('GITHUB_REPO')}
    </div>
</body>
</html>
'''

# 写入HTML文件
with open('branch_report.html', 'w') as f:
    f.write(html_content)

# 写入CSV文件
with open('stale_branches.csv', 'w', newline='') as csvfile:
    fieldnames = ['branch_name', 'last_commit', 'author', 'days_ago', 'status']
    writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
    writer.writeheader()
    for branch in all_branches:
        writer.writerow({
            'branch_name': branch['name'],
            'last_commit': branch['last_commit'],
            'author': branch['author'],
            'days_ago': branch['days_ago'],
            'status': branch['status']
        })

print(f"报告已生成: {len(all_branches)} 个分支, {len(stale_branches)} 个陈旧分支")
EOF
                    '''
                }
            }
        }
        
        stage('发送邮件报告') {
            steps {
                script {
                    // 读取生成的HTML内容
                    def htmlContent = readFile(file: 'branch_report.html')
                    
                    // 获取陈旧分支数量
                    sh '''
                        python3 << 'EOF'
import json
with open('branches.json', 'r') as f:
    data = json.load(f)
branches = data['data']['repository']['refs']['nodes']
stale_days = int(os.getenv('STALE_THRESHOLD', 60))
from datetime import datetime, timezone
now = datetime.now(timezone.utc)
stale_count = 0
for branch in branches:
    if branch['target']:
        commit_date = datetime.fromisoformat(branch['target']['committedDate'].replace('Z', '+00:00'))
        days_ago = (now - commit_date).days
        if days_ago > stale_days:
            stale_count += 1
print(stale_count)
EOF
                    '''.trim()
                    
                    def staleCount = sh(script: '''
                        python3 << 'EOF'
import json, os
from datetime import datetime, timezone
with open('branches.json', 'r') as f:
    data = json.load(f)
branches = data['data']['repository']['refs']['nodes']
stale_days = int(os.getenv('STALE_THRESHOLD', 60))
now = datetime.now(timezone.utc)
stale_count = 0
for branch in branches:
    if branch['target']:
        commit_date = datetime.fromisoformat(branch['target']['committedDate'].replace('Z', '+00:00'))
        days_ago = (now - commit_date).days
        if days_ago > stale_days:
            stale_count += 1
print(stale_count)
EOF
                    ''', returnStdout: true).trim()
                    
                    // 发送HTML格式邮件
                    emailext(
                        to: "${env.TEAM_EMAIL}",
                        subject: "🚨 陈旧分支报告: ${staleCount} 个分支超过 ${params.STALE_THRESHOLD} 天未更新",
                        body: """
                        <html>
                        <body>
                            <p>团队好，</p>
                            <p>以下是自动生成的陈旧分支报告。检测到 <strong>${staleCount}</strong> 个分支超过 <strong>${params.STALE_THRESHOLD}</strong> 天未更新。</p>
                            <p>请及时清理这些分支以保持代码库整洁。</p>
                            <hr>
                            ${htmlContent}
                            <hr>
                            <p><small>此邮件由 Jenkins 自动发送，请勿直接回复。</small></p>
                        </body>
                        </html>
                        """,
                        mimeType: 'text/html',
                        attachmentsPattern: 'stale_branches.csv',
                        replyTo: 'jenkins@yourcompany.com'
                    )
                    
                    echo "邮件报告已发送至: ${env.TEAM_EMAIL}"
                }
            }
        }
    }
    
    post {
        always {
            // 归档报告文件
            archiveArtifacts artifacts: 'branch_report.html, stale_branches.csv, branches.json'
            
            // 清理工作空间
            cleanWs()
        }
    }
}
