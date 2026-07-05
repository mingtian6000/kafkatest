```
import os
import json
from datetime import date
from typing import Optional
from dotenv import load_dotenv
from mcp.server.fastmcp import FastMCP
import httpx

load_dotenv()

mcp = FastMCP("Jira MCP")

JIRA_URL = os.getenv("JIRA_URL", "").rstrip("/")
JIRA_EMAIL = os.getenv("JIRA_EMAIL", "")
JIRA_API_TOKEN = os.getenv("JIRA_API_TOKEN", "")

if not JIRA_URL or not JIRA_EMAIL or not JIRA_API_TOKEN:
    raise ValueError("请设置 JIRA_URL, JIRA_EMAIL, JIRA_API_TOKEN 环境变量")

AUTH = httpx.BasicAuth(JIRA_EMAIL, JIRA_API_TOKEN)
HEADERS = {"Accept": "application/json"}

async def jira_get(path: str):
    async with httpx.AsyncClient(auth=AUTH, headers=HEADERS, timeout=30) as client:
        resp = await client.get(f"{JIRA_URL}{path}")
        resp.raise_for_status()
        return resp.json()

async def jira_post(path: str, body: dict):
    async with httpx.AsyncClient(auth=AUTH, headers={**HEADERS, "Content-Type": "application/json"}, timeout=30) as client:
        resp = await client.post(f"{JIRA_URL}{path}", json=body)
        resp.raise_for_status()
        return resp.json()


def _adf_to_text(adf: dict) -> str:
    """把 Atlassian Document Format 粗略压成纯文本（用于 comment 预览）"""
    parts = []
    for node in adf.get("content", []):
        if node.get("type") == "paragraph":
            for child in node.get("content", []):
                if child.get("type") == "text":
                    parts.append(child.get("text", ""))
    return " ".join(parts)


@mcp.tool()
async def list_dashboards() -> str:
    """列出所有 Dashboard 的 ID 和名称"""
    try:
        data = await jira_get("/rest/api/2/dashboard")
        dashboards = data.get("dashboards", [])
        if not dashboards:
            return "[]"
        result = [{"id": d["id"], "name": d["name"]} for d in dashboards]
        return json.dumps(result, ensure_ascii=False, indent=2)
    except Exception as e:
        return json.dumps({"error": f"获取 Dashboard 列表失败: {str(e)}"})


async def _fetch_dashboard_tasks_raw(dashboard_id: str, start_date: Optional[str] = None, end_date: Optional[str] = None) -> list:
    """内部函数：从 Dashboard 获取任务列表（原始 JSON 格式）"""
    gadgets = await jira_get(f"/rest/api/2/dashboard/{dashboard_id}/items")
    gadget_items = gadgets.get("items", [])
    if not gadget_items:
        return []

    all_issues = []
    date_clause = ""
    if start_date and end_date:
        date_clause = f" AND created >= '{start_date}' AND created <= '{end_date}'"

    for item in gadget_items:
        config = item.get("configuration", {})
        filter_id = config.get("filterId") or config.get("savedFilterId")
        if not filter_id:
            continue
        try:
            filter_data = await jira_get(f"/rest/api/2/filter/{filter_id}")
            jql = filter_data.get("jql", "")
        except Exception:
            continue
        if not jql:
            continue

        full_jql = jql + date_clause
        search_result = await jira_get(
            f"/rest/api/2/search?jql={httpx.QueryParams({'jql': full_jql})}&maxResults=500&fields=assignee,summary,status"
        )
        issues = search_result.get("issues", [])
        for issue in issues:
            fields = issue.get("fields", {})
            assignee_field = fields.get("assignee")
            assignee = assignee_field["displayName"] if assignee_field else "Unassigned"
            summary = fields.get("summary", "")
            status = fields.get("status", {}).get("name", "Unknown")
            all_issues.append({
                "assignee": assignee,
                "key": issue["key"],
                "summary": summary,
                "status": status
            })

    all_issues.sort(key=lambda x: x["assignee"])
    return all_issues


@mcp.tool()
async def get_dashboard_tasks(dashboard_id: str) -> str:
    """
    从指定 Dashboard 中提取所有任务（无日期过滤），返回 JSON 数组。
    """
    try:
        tasks = await _fetch_dashboard_tasks_raw(dashboard_id)
        return json.dumps(tasks, ensure_ascii=False, indent=2)
    except httpx.HTTPStatusError as e:
        if e.response.status_code == 404:
            return json.dumps({"error": f"Dashboard ID '{dashboard_id}' 不存在或无权访问"})
        return json.dumps({"error": f"HTTP 错误: {e.response.status_code}"})
    except Exception as e:
        return json.dumps({"error": f"获取 Dashboard 任务失败: {str(e)}"})


@mcp.tool()
async def get_dashboard_tasks_by_date(
    dashboard_id: str,
    start_date: Optional[str] = None,
    end_date: Optional[str] = None
) -> str:
    """
    从指定 Dashboard 中提取在日期范围内的任务，返回 JSON 数组。
    参数:
        dashboard_id: Dashboard 的数字 ID
        start_date: 起始日期，格式 YYYY-MM-DD（默认今天）
        end_date: 结束日期，格式 YYYY-MM-DD（默认今天）
    """
    today = date.today().isoformat()
    start = start_date or today
    end = end_date or today
    try:
        tasks = await _fetch_dashboard_tasks_raw(dashboard_id, start, end)
        return json.dumps(tasks, ensure_ascii=False, indent=2)
    except httpx.HTTPStatusError as e:
        if e.response.status_code == 404:
            return json.dumps({"error": f"Dashboard ID '{dashboard_id}' 不存在或无权访问"})
        return json.dumps({"error": f"HTTP 错误: {e.response.status_code}"})
    except Exception as e:
        return json.dumps({"error": f"获取 Dashboard 任务失败: {str(e)}"})


@mcp.tool()
async def get_jira_tasks(jql: str, max_results: int = 200) -> str:
    """
    直接通过 JQL 查询任务（支持任意日期条件），返回 JSON 数组。
    """
    try:
        encoded_jql = httpx.QueryParams({"jql": jql}).__str__().split("=", 1)[1]
        url = f"/rest/api/2/search?jql={encoded_jql}&maxResults={min(max_results, 500)}&fields=assignee,summary,status"
        data = await jira_get(url)
        issues = data.get("issues", [])
        rows = []
        for issue in issues:
            fields = issue.get("fields", {})
            assignee = fields.get("assignee")
            assignee_name = assignee["displayName"] if assignee else "Unassigned"
            summary = fields.get("summary", "")
            status = fields.get("status", {}).get("name", "Unknown")
            rows.append({
                "assignee": assignee_name,
                "key": issue["key"],
                "summary": summary,
                "status": status
            })
        rows.sort(key=lambda x: x["assignee"])
        return json.dumps(rows, ensure_ascii=False, indent=2)
    except Exception as e:
        return json.dumps({"error": f"JQL 查询失败: {str(e)}"})


@mcp.tool()
async def get_jira_detail(issue_key: str) -> str:
    """
    获取指定 Jira 任务的完整详情：
    - Metadata（Key / Type / Summary / Status / Priority / Assignee / Reporter / Epic / Labels）
    - Description（ADF JSON）
    - 最近 Comments（最多 20 条，含作者+日期）
    - Development Panel 里的 PR 链接（走 dev-status 内部端点）
    """
    try:
        fields_param = (
            "id,key,summary,description,status,issuetype,priority,"
            "assignee,reporter,labels,parent,"
            "customfield_10014,issuelinks"
        )
        issue = await jira_get(
            f"/rest/api/3/issue/{issue_key}?fields={fields_param}"
        )
        f = issue.get("fields", {})
        numeric_id = issue.get("id")

        lines = []
        lines.append(f"## Metadata")
        lines.append(f"- **Key**: {issue['key']}")
        lines.append(f"- **Type**: {f.get('issuetype', {}).get('name', '-')}")
        lines.append(f"- **Summary**: {f.get('summary', '-')}")
        lines.append(f"- **Status**: {f.get('status', {}).get('name', '-')}")
        lines.append(f"- **Priority**: {f.get('priority', {}).get('name', '-')}")
        lines.append(f"- **Assignee**: {f.get('assignee', {}).get('displayName', 'Unassigned')}")
        lines.append(f"- **Reporter**: {f.get('reporter', {}).get('displayName', '-')}")

        epic_val = f.get("customfield_10014")
        if epic_val:
            lines.append(f"- **Epic**: {epic_val}")
        elif f.get("parent"):
            lines.append(f"- **Parent**: {f['parent'].get('key', '-')} ({f['parent'].get('fields', {}).get('summary', '')})")

        lines.append(f"- **Labels**: {', '.join(f.get('labels', [])) or '-'}")

        desc = f.get("description")
        lines.append(f"\n## Description")
        if desc:
            lines.append("```json")
            lines.append(json.dumps(desc, ensure_ascii=False, indent=2))
            lines.append("```")
        else:
            lines.append("_No description_")

        comments_data = await jira_get(
            f"/rest/api/3/issue/{issue_key}/comment?maxResults=20&orderBy=-created"
        )
        comments = comments_data.get("comments", [])
        lines.append(f"\n## Comments ({len(comments)})")
        if not comments:
            lines.append("_No comments_")
        else:
            for c in comments:
                author = c.get("author", {}).get("displayName", "?")
                created = c.get("created", "")[:10]
                body = c.get("body", "")
                if isinstance(body, dict):
                    body_text = _adf_to_text(body)
                else:
                    body_text = str(body)
                body_text = body_text[:300] + ("..." if len(body_text) > 300 else "")
                lines.append(f"\n### {author} · {created}")
                lines.append(f"> {body_text}")

        if numeric_id:
            dev_summary = await jira_get(
                f"/rest/dev-status/latest/issue/summary?issueId={numeric_id}"
            )
            pr_overall = dev_summary.get("summary", {}).get("pullrequest", {}).get("overall", {})
            pr_count = pr_overall.get("count", 0)
            lines.append(f"\n## Pull Requests")
            if pr_count == 0:
                lines.append("_No linked PRs_")
            else:
                by_inst = dev_summary.get("summary", {}).get("pullrequest", {}).get("byInstanceType", {})
                for inst_type, inst_info in by_inst.items():
                    detail = await jira_get(
                        f"/rest/dev-status/latest/issue/detail?issueId={numeric_id}"
                        f"&applicationType={inst_type}&dataType=pullrequest"
                    )
                    for repo in detail.get("detail", []):
                        repo_name = repo.get("repository", {}).get("name", "?")
                        for pr in repo.get("pullRequests", []):
                            title = pr.get("title", "?")
                            url = pr.get("url", "")
                            status = pr.get("status", "?")
                            lines.append(f"- [{repo_name}] **{title}** — `{status}` — {url}")
                lines.append(f"\n共 {pr_count} 个 PR")

        return "\n".join(lines)

    except httpx.HTTPStatusError as e:
        if e.response.status_code == 404:
            return f"任务 {issue_key} 不存在或无权访问。"
        return f"HTTP 错误: {e.response.status_code}"
    except Exception as e:
        return f"获取任务详情失败: {str(e)}"


@mcp.tool()
async def transition_issue(issue_key: str, target_status: str) -> str:
    """
    将指定 Jira 任务的状态变更为目标状态。
    例如：将 PROJ-123 从 "To Do" 变为 "In Progress"。
    """
    try:
        transitions_url = f"/rest/api/2/issue/{issue_key}/transitions"
        trans_data = await jira_get(transitions_url)
        transitions = trans_data.get("transitions", [])
        if not transitions:
            return f"任务 {issue_key} 当前没有可用的状态转换。"

        matched_transition = None
        for t in transitions:
            if t["to"]["name"].lower() == target_status.lower():
                matched_transition = t
                break

        if not matched_transition:
            available = [t["to"]["name"] for t in transitions]
            return (
                f"目标状态 '{target_status}' 不可用。当前可用的转换状态有：{', '.join(available)}。"
                f"\n提示：请检查任务当前状态，或使用正确的状态名称。"
            )

        transition_id = matched_transition["id"]
        payload = {"transition": {"id": transition_id}}
        await jira_post(transitions_url, payload)
        return f"✅ 成功将 {issue_key} 的状态变更为 '{target_status}'。"

    except httpx.HTTPStatusError as e:
        if e.response.status_code == 404:
            return f"任务 {issue_key} 不存在或无权访问。"
        return f"HTTP 错误: {e.response.status_code} - {e.response.text}"
    except Exception as e:
        return f"变更状态失败: {str(e)}"


if __name__ == "__main__":
    mcp.run(transport="stdio")
