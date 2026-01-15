# 伪代码示例
from mcp import ClientSession

async with ClientSession(server_params) as session:
    # 就像调用本地函数一样调用 MCP 工具
    result = await session.call_tool("create_structured_page", arguments={
        "space": "QA",
        "title": "自动排版测试",
        "markdown_content": "# 标题\n## 子标题\n这里是自动生成的内容..."
    })
