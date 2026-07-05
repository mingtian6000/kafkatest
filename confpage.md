```
@mcp.tool()
async def get_board_tasks(board_id: str) -> str:
    """
    从指定 Jira Agile Board 拉取所有任务（不分 Sprint），返回 JSON 数组。
    适用于 Rapid View / Scrum Board / Kanban Board。
    
    参数:
        board_id: Board 的数字 ID（即 Rapid View ID，如 "123"）
    """
    try:
        # agile API 路径和 core API 不同，但 host 复用 JIRA_URL
        url = f"/rest/agile/1.0/board/{board_id}/issue?maxResults=500&fields=assignee,summary,status,issuetype,priority,customfield_10014"
        data = await jira_get(url)
        issues = data.get("issues", [])
        rows = []
        for issue in issues:
            f = issue.get("fields", {})
            assignee = f.get("assignee")
            assignee_name = assignee["displayName"] if assignee else "Unassigned"
            epic = f.get("customfield_10014", "")
            rows.append({
                "assignee": assignee_name,
                "key": issue["key"],
                "summary": f.get("summary", ""),
                "status": f.get("status", {}).get("name", "Unknown"),
                "type": f.get("issuetype", {}).get("name", "-"),
                "priority": f.get("priority", {}).get("name", "-"),
                "epic": epic or "-",
            })
        rows.sort(key=lambda x: x["assignee"])
        return json.dumps(rows, ensure_ascii=False, indent=2)
    except httpx.HTTPStatusError as e:
        if e.response.status_code == 404:
            return json.dumps({"error": f"Board ID '{board_id}' 不存在或无权访问"})
        return json.dumps({"error": f"HTTP 错误: {e.response.status_code}"})
    except Exception as e:
        return json.dumps({"error": f"获取 Board 任务失败: {str(e)}"})


@mcp.tool()
async def get_active_sprint_tasks(board_id: str) -> str:
    """
    从指定 Board 的【当前活跃 Sprint】拉取所有任务，返回 JSON 数组。
    如果你的 Rapid Board 是按 Sprint 跑的（Scrum），这个最常用——
    比 get_board_tasks 更聚焦，只拿正在跑的那个 Sprint。
    
    参数:
        board_id: Board 的数字 ID（Rapid View ID）
    """
    try:
        # 1. 先拿这个 Board 下的 active sprint
        sprint_url = f"/rest/agile/1.0/board/{board_id}/sprint?state=active&maxResults=50"
        sprint_data = await jira_get(sprint_url)
        sprints = sprint_data.get("values", [])
        if not sprints:
            return json.dumps({"info": f"Board {board_id} 当前没有活跃 Sprint"}, ensure_ascii=False)

        # 一个 Board 理论上只有一个 active sprint，但兜底遍历
        all_rows = []
        for sprint in sprints:
            sprint_id = sprint["id"]
            sprint_name = sprint["name"]
            # 2. 拉这个 Sprint 下的 issue
            issue_url = (
                f"/rest/agile/1.0/sprint/{sprint_id}/issue"
                f"?maxResults=500&fields=assignee,summary,status,issuetype,priority,customfield_10014"
            )
            issue_data = await jira_get(issue_url)
            issues = issue_data.get("issues", [])
            for issue in issues:
                f = issue.get("fields", {})
                assignee = f.get("assignee")
                assignee_name = assignee["displayName"] if assignee else "Unassigned"
                epic = f.get("customfield_10014", "")
                all_rows.append({
                    "sprint": sprint_name,
                    "assignee": assignee_name,
                    "key": issue["key"],
                    "summary": f.get("summary", ""),
                    "status": f.get("status", {}).get("name", "Unknown"),
                    "type": f.get("issuetype", {}).get("name", "-"),
                    "priority": f.get("priority", {}).get("name", "-"),
                    "epic": epic or "-",
                })

        all_rows.sort(key=lambda x: (x["sprint"], x["assignee"]))
        return json.dumps(all_rows, ensure_ascii=False, indent=2)
    except httpx.HTTPStatusError as e:
        if e.response.status_code == 404:
            return json.dumps({"error": f"Board ID '{board_id}' 不存在或无权访问"})
        return json.dumps({"error": f"HTTP 错误: {e.response.status_code}"})
    except Exception as e:
        return json.dumps({"error": f"获取活跃 Sprint 任务失败: {str(e)}"})
