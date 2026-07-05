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
    raise ValueError("Please set JIRA_URL, JIRA_EMAIL, JIRA_API_TOKEN environment variables")

AUTH = httpx.BasicAuth(JIRA_EMAIL, JIRA_API_TOKEN)
HEADERS = {"Accept": "application/json"}


async def jira_get(path: str):
    async with httpx.AsyncClient(auth=AUTH, headers=HEADERS, timeout=30) as client:
        resp = await client.get(f"{JIRA_URL}{path}")
        resp.raise_for_status()
        return resp.json()


async def jira_post(path: str, body: dict):
    async with httpx.AsyncClient(
        auth=AUTH,
        headers={**HEADERS, "Content-Type": "application/json"},
        timeout=30,
    ) as client:
        resp = await client.post(f"{JIRA_URL}{path}", json=body)
        resp.raise_for_status()
        return resp.json()


def _adf_to_text(adf: dict) -> str:
    """Convert Atlassian Document Format to plain text (rough, for comment preview)."""
    parts = []
    for node in adf.get("content", []):
        if node.get("type") == "paragraph":
            for child in node.get("content", []):
                if child.get("type") == "text":
                    parts.append(child.get("text", ""))
    return " ".join(parts)


@mcp.tool()
async def list_dashboards() -> str:
    """
    List all accessible Jira Dashboards.
    Returns a JSON array of objects with 'id' and 'name'.
    """
    try:
        data = await jira_get("/rest/api/2/dashboard")
        dashboards = data.get("dashboards", [])
        if not dashboards:
            return "[]"
        result = [{"id": d["id"], "name": d["name"]} for d in dashboards]
        return json.dumps(result, ensure_ascii=False, indent=2)
    except Exception as e:
        return json.dumps({"error": f"Failed to list dashboards: {str(e)}"})


async def _fetch_dashboard_tasks_raw(
    dashboard_id: str,
    start_date: Optional[str] = None,
    end_date: Optional[str] = None,
) -> list:
    """Internal: fetch tasks from a dashboard's gadgets (Filter Results only)."""
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
            all_issues.append(
                {
                    "assignee": assignee,
                    "key": issue["key"],
                    "summary": summary,
                    "status": status,
                }
            )

    all_issues.sort(key=lambda x: x["assignee"])
    return all_issues


@mcp.tool()
async def get_dashboard_tasks(dashboard_id: str) -> str:
    """
    Get all tasks from a given Dashboard (no date filter).
    Returns JSON array of {assignee, key, summary, status}.
    """
    try:
        tasks = await _fetch_dashboard_tasks_raw(dashboard_id)
        return json.dumps(tasks, ensure_ascii=False, indent=2)
    except httpx.HTTPStatusError as e:
        if e.response.status_code == 404:
            return json.dumps({"error": f"Dashboard ID '{dashboard_id}' does not exist or access denied"})
        return json.dumps({"error": f"HTTP error: {e.response.status_code}"})
    except Exception as e:
        return json.dumps({"error": f"Failed to get dashboard tasks: {str(e)}"})


@mcp.tool()
async def get_dashboard_tasks_by_date(
    dashboard_id: str,
    start_date: Optional[str] = None,
    end_date: Optional[str] = None,
) -> str:
    """
    Get tasks from a Dashboard filtered by creation date range.
    Parameters:
        dashboard_id: numeric Dashboard ID
