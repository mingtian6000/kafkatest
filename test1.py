from fastmcp import FastMCP
from atlassian import Confluence
import markdown2  # pip install markdown2
import os

mcp = FastMCP("Confluence-Smart-Editor")

# ... (此处保留之前的 Confluence 初始化代码) ...

def format_for_confluence(markdown_text: str):
    """将 Markdown 转换为 Confluence 识别的格式并注入高级组件"""
    # 1. 基础转换
    html_content = markdown2.markdown(markdown_text, extras=["tables", "fenced-code-blocks"])
    
    # 2. 注入目录 (TOC) 宏 - 放在页面最前面
    toc_macro = '<ac:structured-macro ac:name="toc" ac:schema-version="1" />'
    
    # 3. 包装代码块 (将 <pre><code> 替换为 Confluence 的 code macro)
    # 这部分可以根据需要用正则进一步精细化处理
    
    return f"{toc_macro}{html_content}"

@mcp.tool()
def create_structured_page(space: str, title: str, markdown_content: str, parent_id: str = None):
    """
    接收 Markdown 内容，自动排版并生成带目录和格式的 Confluence 页面。
    """
    formatted_html = format_for_confluence(markdown_content)
    try:
        response = conf.create_page(
            space=space,
            title=title,
            body=formatted_html,
            parent_id=parent_id,
            representation='storage'
        )
        return f"已生成排版精美的页面！ID: {response['id']}"
    except Exception as e:
        return f"失败: {str(e)}"
