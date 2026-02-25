好的，以下是 Python 版本的 Playwright 配置示例：

Python 配置 Microsoft Edge

1. 在代码中直接启动（推荐）

from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    # 使用 chromium 引擎，但指定渠道为 msedge
    browser = p.chromium.launch(
        channel="msedge",  # 关键：使用系统 Edge
        headless=False     # 如果要看浏览器界面
    )
    
    page = browser.new_page()
    page.goto("https://example.com")
    # ... 你的操作
    browser.close()

2. 异步版本

import asyncio
from playwright.async_api import async_playwright

async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch(
            channel="msedge",
            headless=False
        )
        
        page = await browser.new_page()
        await page.goto("https://example.com")
        # ... 你的操作
        await browser.close()

asyncio.run(main())

3. 在 pytest-playwright 配置文件中配置

在 
"playwright.config.py" 中配置：

import pytest
from playwright.sync_api import Page

@pytest.fixture(scope="session")
def browser_context_args(browser_context_args):
    return {
        **browser_context_args,
        "viewport": {"width": 1920, "height": 1080}
    }

def pytest_configure(config):
    config.option.browser = ["chromium"]
    
def pytest_playwright_configure(config):
    config.channel = "msedge"  # 关键配置：使用系统 Edge

验证渠道是否可用

from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    try:
        # 尝试启动 Edge
        browser = p.chromium.launch(channel="msedge")
        print("✓ Edge 浏览器启动成功")
        browser.close()
    except Exception as e:
        print(f"✗ 启动失败: {e}")
        print("请先安装 Edge 浏览器或运行: playwright install msedge")

常用配置选项

# 完整配置示例
browser = p.chromium.launch(
    channel="msedge",           # 使用系统 Edge
    headless=False,             # 显示浏览器界面
    slow_mo=100,                # 操作延迟（毫秒），便于观察
    args=["--start-maximized"]  # 启动参数：最大化窗口
)

# 或使用自定义配置文件
context = browser.new_context(
    viewport={"width": 1920, "height": 1080},
    user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
)

如果系统没有安装 Edge

在终端中运行：

# 安装 Playwright 并安装 Edge 浏览器
pip install playwright
playwright install msedge

# 或一次性安装所有浏览器
playwright install

不同浏览器的启动方式对比

启动方式 Python 代码 说明
Playwright 自带 Chromium 
"p.chromium.launch()" 纯净版，适合自动化测试
系统 Edge 
"p.chromium.launch(channel="msedge")" 你需要的配置
系统 Chrome 
"p.chromium.launch(channel="chrome")" 使用系统 Chrome
Firefox 
"p.firefox.launch()" 不同的引擎
WebKit 
"p.webkit.launch()" Safari 内核

错误排查

如果遇到问题，可以这样检查：

from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    # 1. 检查 Playwright 安装的浏览器
    print("已安装的浏览器类型:", p.chromium, p.firefox, p.webkit)
    
    # 2. 尝试使用默认路径
    import subprocess
    try:
        # Windows 查找 Edge 路径
        result = subprocess.run(
            ["where", "msedge"], 
            capture_output=True, 
            text=True
        )
        if result.stdout:
            print(f"Edge 路径: {result.stdout}")
    except:
        pass

关键点：Python 和 TypeScript/JavaScript 的 API 几乎一样，只是语法不同。核心都是 
"p.chromium.launch(channel="msedge")"。