```python
@mcp.tool()
def clone_page_with_attachments(source_page_id: str, target_space: str, new_title: str):
    """
    克隆页面内容及其所有附件到新页面。
    """
    # 1. 获取原页面内容和附件
    source_page = conf.get_page_by_id(source_page_id, expand='body.storage')
    attachments = conf.get_attachments_from_content(source_page_id)['results']
    
    # 2. 创建新页面
    new_page = conf.create_page(space=target_space, title=new_title, 
                                body=source_page['body']['storage']['value'])
    new_id = new_page['id']
    
    # 3. 循环处理附件
    for att in attachments:
        file_name = att['title']
        # 下载附件内容
        file_content = conf.get_attachment_content(att['id'])
        # 上传到新页面
        conf.attach_file(file_content, name=file_name, page_id=new_id)
        
    return f"克隆完成！新页面 ID: {new_id}，已迁移 {len(attachments)} 个附件。"
