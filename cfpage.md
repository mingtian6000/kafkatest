太棒了！直接手动拼接 XML 字符串确实非常痛苦。为了让你的 FastMCP 工具更易维护，我们可以设计一个简单的 ConfluenceBuffer 类。
它的逻辑类似于“乐高积木”：你只需要调用不同的方法（如 add_code 或 add_info），最后它会统一生成 Confluence 能够识别的 Storage Format。
1. 核心代码实现：ConfluenceContentBuilder
你可以将这段代码集成到你的 MCP 项目中。
```python
class ConfluenceContentBuilder:
    def __init__(self):
        self.elements = []

    def add_text(self, text):
        """添加普通文本"""
        self.elements.append(f"<p>{text}</p>")
        return self

    def add_code(self, code_content, language="python", title=None):
        """添加代码块宏"""
        macro = f'''
        <ac:structured-macro ac:name="code">
            <ac:parameter ac:name="language">{language}</ac:parameter>
            {f'<ac:parameter ac:name="title">{title}</ac:parameter>' if title else ''}
            <ac:plain-text-body><![CDATA[{code_content}]]></ac:plain-text-body>
        </ac:structured-macro>
        '''
        self.elements.append(macro)
        return self

    def add_info_panel(self, content, title="Note", type="info"):
        """添加信息面板 (info, tip, note, warning)"""
        macro = f'''
        <ac:structured-macro ac:name="{type}">
            <ac:parameter ac:name="title">{title}</ac:parameter>
            <ac:rich-text-body><p>{content}</p></ac:rich-text-body>
        </ac:structured-macro>
        '''
        self.elements.append(macro)
        return self

    def add_image(self, filename):
        """引用已上传的附件图片"""
        img_tag = f'''
        <ac:image ac:align="center" ac:layout="center">
            <ri:attachment ri:filename="{filename}" />
        </ac:image>
        '''
        self.elements.append(img_tag)
        return self

    def build(self):
        """生成最终的 Storage Format 字符串"""
        return "".join(self.elements)

2. 在 FastMCP 中如何调用
假设你正在编写一个名为 create_rich_page 的工具：
@mcp.tool()
async def create_rich_page(title: str, space_key: str):
    builder = ConfluenceContentBuilder()
    
    # 构建丰富内容
    builder.add_text("这是通过 FastMCP 自动生成的架构文档。")
    builder.add_info_panel("请注意：生产环境操作需谨慎！", type="warning")
    builder.add_code("print('Hello MCP')", title="示例代码")
    builder.add_image("architecture.png") # 假设附件已存在
    
    storage_html = builder.build()
    
    # 构造 API 请求体
    payload = {
        "type": "page",
        "title": title,
        "space": {"key": space_key},
        "body": {
            "storage": {
                "value": storage_html,
                "representation": "storage"  # 关键点：声明使用存储格式
            }
        }
    }
    
    # 使用你的 httpx 或 requests 客户端发送 payload
    # ... 发送请求的代码 ...
    return f"页面 '{title}' 已创建成功。"

3. 实现细节解析
 * ac:rich-text-body vs ac:plain-text-body:
   * Rich Text: 用于可以包含其他 HTML 标签（如 <p>, <b>）的宏，比如 Info Panel。
   * Plain Text: 用于纯文本内容，比如代码块，通常配合 <![CDATA[ ... ]]> 使用，防止代码里的 < 或 & 破坏 XML 结构。
 * 多标签页 (Tabs):
   如果是 Confluence Cloud 原生，通常使用 Expand 宏来实现折叠。如果需要真正的 Tabs，通常需要第三方插件，对应的宏名称可能是 ac:name="tabs"。
详尽文档：实现步骤建议
 * 第一步：定义 Builder 类：将上面的 ConfluenceContentBuilder 放入一个独立的 utils.py 文件中。
 * 第二步：更新 API 配置：在你的请求头中确保 Content-Type: application/json。
 * 第三步：测试宏渲染：先尝试最简单的 add_info_panel，在 Confluence 页面查看是否显示为蓝色的信息框。
关于图片嵌入，你是否需要我为你写一个专门处理“本地图片上传并自动插入页面”的完整工作流函数？
