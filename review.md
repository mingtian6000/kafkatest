```
# 读取标准输入 (stdin) 的 JSON 数据
$inputData = $stdin.ReadToEnd()

try {
    $json = $inputData | ConvertFrom-Json
    
    # 获取工具名称和文件路径
    $toolName = $json.toolName
    $filePath = $json.input.filePath  # 注意：不同工具的 JSON 结构可能略有不同，这里适配常见的 writeFile/editFile
    
    # 调试输出：把看到的内容打印出来（可以在 Output 面板查看）
    Write-Output "Hook 检测到调用: $toolName -> $filePath"

    # 拦截逻辑：只要是写文件操作，且路径里包含 mcp 关键字
    if ($toolName -match "write|edit|create|patch" -and $filePath -match "mcp\.json") {
        Write-Output "🚫 已被 Hook 强制拦截：不允许写入 MCP 配置文件 ($filePath)"
        # 关键：退出码必须是 2 才能强制阻止 Agent 执行
        exit 2 
    }
}
catch {
    # 如果 JSON 解析失败，为了安全起见，放过这次请求（或者也可以选择 exit 2 严格拦截）
    Write-Output "Hook 解析错误: $_"
}

# 放行
exit 0
