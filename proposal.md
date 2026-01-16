```python
from fastmcp import FastMCP
from atlassian import Confluence
import os

# 初始化 MCP
mcp = FastMCP("Confluence-Manager")

# Confluence 配置 (建议通过环境变量传入)
CONFLUENCE_URL = os.getenv("CONFLUENCE_URL")
CONFLUENCE_USERNAME = os.getenv("CONFLUENCE_USERNAME")
CONFLUENCE_TOKEN = os.getenv("CONFLUENCE_TOKEN") # 在 Atlassian 账户设置中生成

conf = Confluence(
    url=CONFLUENCE_URL,
    username=CONFLUENCE_USERNAME,
    password=CONFLUENCE_TOKEN,
    cloud=True
)

@mcp.tool()
def create_confluence_page(space: str, title: str, body_content: str, parent_id: str = None):
    """
    在指定 Space 创建一个层次清晰的 Confluence 页面。
    :param space: 空间关键字 (如 'PROJ')
    :param title: 页面标题
    :param body_content: HTML 格式的页面内容
    :param parent_id: 父级页面 ID (可选)
    """
    try:
        response = conf.create_page(
            space=space,
            title=title,
            body=body_content,
            parent_id=parent_id,
            type='page',
            representation='storage' # 使用 Confluence 存储格式 (XHTML)
        )
        return f"页面创建成功！ID: {response['id']}, 链接: {response['_links']['base']}{response['_links']['webui']}"
    except Exception as e:
        return f"创建失败: {str(e)}"

@mcp.tool()
def update_confluence_page(page_id: str, title: str, body_content: str):
    """
    更新现有的 Confluence 页面内容。
    """
    try:
        conf.update_page(
            page_id=page_id,
            title=title,
            body=body_content,
            type='page',
            representation='storage'
        )
        return f"页面 {page_id} 更新成功！"
    except Exception as e:
        return f"更新失败: {str(e)}"

if __name__ == "__main__":
    mcp.run()


```python
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
