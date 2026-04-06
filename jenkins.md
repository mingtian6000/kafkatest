```
node('cm-linux') {
    timeout(time: 3, unit: 'HOURS') {
        stage('Waiting for complete') {
            timeout(time: 200, unit: 'MINUTES') {
                def logFound = false
                waitUntil {
                    // 激活gcloud配置
                    sh(script: "gcloud config configurations activate brett-\$env", returnStdout: true)
                    sh(script: "gcloud config set pass_credentials_to_gsutil true", returnStdout: true)
                    
                    // 检查文件是否存在
                    echo "Checking for log file..."
                    def fileExists = sh(returnStdout: true, script: "gsutil -q stat gs://${pipelineParams.bucketName}/path/to/your/log/file.log && echo 'exists' || echo 'not exists'").trim()
                    
                    if (fileExists == 'exists') {
                        // 文件存在，检查日志内容
                        echo "Log file exists, checking for '卡森转10'..."
                        def logContent = sh(returnStdout: true, script: "gsutil cat gs://${pipelineParams.bucketName}/path/to/your/log/file.log | grep -c '卡森转10'").trim()
                        
                        if (logContent.toInteger() > 0) {
                            echo "✅ Found '卡森转10' in log! Exiting wait loop."
                            logFound = true
                            return true  // 跳出 waitUntil
                        } else {
                            echo "❌ '卡森转10' not found in log yet. Waiting 3 minutes..."
                            sleep(time: 3, unit: "MINUTES")
                            return false  // 继续循环
                        }
                    } else {
                        echo "Log file does not exist yet. Waiting 3 minutes..."
                        sleep(time: 3, unit: "MINUTES")
                        return false  // 继续循环
                    }
                }
                
                if (logFound) {
                    echo "Proceeding to next stage..."
                } else {
                    error("Timeout reached without finding '卡森转10' in log.")
                }
            }
        }
        
        // 下一个 stage
        stage('Next Stage') {
            echo "Moving to the next stage..."
            // 你的下一个阶段逻辑
        }
    }
}
