```
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
<title>Project Dashboard</title>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; border-radius: 0 !important; }
  body {
    font-family: 'Segoe UI', -apple-system, Arial, sans-serif;
    background: #ffffff;
    color: #1f2937;
    padding: 24px;
    font-size: 14px;
  }
  .app { max-width: 1300px; margin: 0 auto; }

  header { margin-bottom: 20px; border-bottom: 1px solid #d1d5db; padding-bottom: 14px; }
  header h1 { font-size: 20px; font-weight: 700; }
  header p { font-size: 13px; color: #6b7280; margin-top: 3px; }

  /* ===== Tab 通用（纯直角、浏览器原生感）===== */
  .tabstrip { display: flex; flex-wrap: wrap; }
  .tabstrip .tab {
    padding: 10px 22px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 600;
    color: #4b5563;
    background: #f9fafb;
    border: 1px solid #d1d5db;
    margin-right: -1px;          /* 边框合并 */
    transition: background .15s, color .15s;
  }
  .tabstrip .tab:hover { background: #eef2ff; }
  .tabstrip .tab.active {
    background: #ffffff;
    color: #2563eb;
    border-bottom: 2px solid #ffffff;  /* 用白底“顶开”下划线，形成选中口 */
    position: relative; z-index: 2;
  }

  /* 第一层主 Tab：更醒目 */
  .tabs-main { border-bottom: 1px solid #2563eb; }
  .tabs-main .tab {
    padding: 12px 28px;
    font-size: 15px;
    border-top: 1px solid #d1d5db;
    border-left: 1px solid #d1d5db;
    border-right: 1px solid #d1d5db;
    border-bottom: none;
    margin-right: 4px;
    background: #f3f4f6;
  }
  .tabs-main .tab.active {
    background: #ffffff;
    color: #2563eb;
    border-bottom: 2px solid #ffffff;
  }

  /* 内容面板 */
  .panel {
    border: 1px solid #d1d5db;
    border-top: none;
    padding: 22px;
    background: #ffffff;
    margin-bottom: 22px;
  }

  /* 整体进度区 */
  .overall { display: grid; grid-template-columns: 1fr 1fr; gap: 22px; margin-bottom: 22px; }
  @media (max-width: 860px) { .overall { grid-template-columns: 1fr; } }
  .chart-card { border: 1px solid #d1d5db; padding: 16px; }
  .chart-card h3 {
    font-size: 13px; color: #374151; margin-bottom: 12px; font-weight: 600;
    text-transform: uppercase; letter-spacing: .4px;
    border-bottom: 1px solid #e5e7eb; padding-bottom: 8px;
  }
  .chart-wrap { position: relative; height: 230px; }

  /* 子 Tab 标题 */
  .section-label {
    font-size: 12px; color: #6b7280; text-transform: uppercase;
    letter-spacing: .5px; margin: 6px 0 10px; font-weight: 600;
  }

  /* 环境区 */
  .env-detail { display: grid; grid-template-columns: 1fr 1.5fr; gap: 22px; align-items: start; }
  @media (max-width: 860px) { .env-detail { grid-template-columns: 1fr; } }
  .env-summary { border: 1px solid #d1d5db; padding: 16px; }
  .env-summary h4 {
    font-size: 13px; color: #374151; margin-bottom: 10px; font-weight: 600;
    text-transform: uppercase; letter-spacing: .4px;
    border-bottom: 1px solid #e5e7eb; padding-bottom: 8px;
  }
  .env-pct-big { font-size: 44px; font-weight: 800; line-height: 1; }
  .env-pct-big span { font-size: 16px; color: #6b7280; }
  .env-status { font-size: 12px; font-weight: 700; margin-top: 6px; text-transform: uppercase; }
  .env-mini { margin-top: 14px; height: 110px; }

  /* RWI 表格 */
  .rwi-section { border: 1px solid #d1d5db; padding: 16px; }
  .rwi-section h4 {
    font-size: 13px; color: #374151; margin-bottom: 12px; font-weight: 600;
    text-transform: uppercase; letter-spacing: .4px;
    border-bottom: 1px solid #e5e7eb; padding-bottom: 8px;
    display: flex; justify-content: space-between; align-items: center;
  }
  .rwi-section h4 .hint { font-size: 11px; color: #6b7280; font-weight: 400; text-transform: none; letter-spacing: 0; }
  table.rwi { width: 100%; border-collapse: collapse; font-size: 12.5px; }
  table.rwi th, table.rwi td {
    padding: 8px 10px; text-align: left; border-bottom: 1px solid #e5e7eb;
  }
  table.rwi th {
    color: #4b5563; font-weight: 600; font-size: 11px; text-transform: uppercase;
    letter-spacing: .4px; background: #f9fafb; border-bottom: 1px solid #d1d5db;
  }
  table.rwi td { color: #1f2937; }
  table.rwi tbody tr:hover td { background: #f9fafb; }
  .pill {
    display: inline-block; padding: 2px 8px; font-size: 11px; font-weight: 700;
    border: 1px solid; text-transform: uppercase;
  }
  .pill.done { background: #ecfdf5; color: #15803d; border-color: #86efac; }
  .pill.wip  { background: #fffbeb; color: #b45309; border-color: #fcd34d; }
  .pill.open { background: #fef2f2; color: #b91c1c; border-color: #fca5a5; }
  .pill-id { font-family: 'Consolas', monospace; color: #2563eb; font-weight: 600; }
  .empty-note { color: #6b7280; font-size: 13px; padding: 16px 0; }
</style>
</head>
<body>
<div class="app">

  <header>
    <h1>Project Dashboard</h1>
    <p>F24 &middot; C48 &middot; DDS &mdash; Infrastructure Delivery Status</p>
  </header>

  <!-- Main Tabs (Level 1) -->
  <div class="tabstrip tabs-main" id="tabsMain"></div>

  <div class="panel">

    <!-- Overall progress -->
    <div class="overall">
      <div class="chart-card">
        <h3>Overall Progress (by App)</h3>
        <div class="chart-wrap"><canvas id="overallBar"></canvas></div>
      </div>
      <div class="chart-card">
        <h3>Overall Completion</h3>
        <div class="chart-wrap"><canvas id="overallPie"></canvas></div>
      </div>
    </div>

    <!-- Sub Tabs (Level 2) -->
    <div class="section-label">Application</div>
    <div class="tabstrip tabs-sub" id="tabsSub"></div>

    <!-- Env Tabs (Level 3) -->
    <div style="height:18px"></div>
    <div class="section-label">Environment</div>
    <div class="tabstrip tabs-env" id="tabsEnv"></div>

    <!-- Env detail: chart + RWI table -->
    <div style="height:18px"></div>
    <div class="env-detail" id="envDetail"></div>

  </div>
</div>

<script>
/* ============ DATA MODEL ============ */
const DATA = {
  F24: {
    apps: {
      "Infra App":  { qa:85, pre:60, prod:30 },
      "Ham App":    { qa:70, pre:40, prod:10 },
      "Other App":  { qa:55, pre:25, prod:0  },
    },
    overall: { done:42, remaining:58 },
  },
  C48: {
    apps: {
      "Infra App":  { qa:95, pre:80, prod:65 },
      "Ham App":    { qa:90, pre:70, prod:45 },
      "Other App":  { qa:60, pre:35, prod:15 },
    },
    overall: { done:63, remaining:37 },
  },
  DDS: {
    apps: {
      "Infra App":  { qa:100, pre:95, prod:90 },
      "Ham App":    { qa:100, pre:90, prod:85 },
      "Other App":  { qa:80,  pre:60, prod:50 },
    },
    overall: { done:81, remaining:19 },
  },
};

// RWI (Release Work Items) — mock data, last 3 months, per project/app/env
const RWI_DATA = {
  F24: {
    "Infra App": {
      qa: [
        { id:"RWI-1021", component:"Auth Gateway",    title:"SAML redirect fix",         assignee:"M. Chen",   target:"2026-08-12", status:"Done" },
        { id:"RWI-1035", component:"Data Pipeline",   title:"Kafka consumer retry",      assignee:"A. Patel",  target:"2026-08-20", status:"Done" },
        { id:"RWI-1048", component:"IAM Service",     title:"Role policy migration",     assignee:"L. Garcia", target:"2026-09-05", status:"WIP"  },
        { id:"RWI-1062", component:"API Gateway",     title:"Rate-limit headers",         assignee:"M. Chen",   target:"2026-09-18", status:"WIP"  },
        { id:"RWI-1077", component:"Audit Log",       title:"TLS 1.3 enforcement",       assignee:"S. Kim",    target:"2026-10-02", status:"Open" },
        { id:"RWI-1083", component:"Config Service",  title:"Feature flag rollout v2",    assignee:"A. Patel",  target:"2026-10-15", status:"Open" },
        { id:"RWI-1091", component:"Auth Gateway",    title:"OIDC token refresh",         assignee:"L. Garcia", target:"2026-10-22", status:"Open" },
      ],
    },
  },
  C48: {
    "Infra App": {
      qa: [
        { id:"RWI-2014", component:"Message Broker",  title:"Dead-letter queue",          assignee:"J. Lee",    target:"2026-08-08", status:"Done" },
        { id:"RWI-2029", component:"Cache Layer",     title:"Redis cluster scaling",      assignee:"D. Brown",  target:"2026-09-01", status:"Done" },
        { id:"RWI-2043", component:"Secrets Manager", title:"Rotation policy",             assignee:"J. Lee",    target:"2026-09-14", status:"WIP"  },
        { id:"RWI-2056", component:"Ingress",         title:"mTLS peer verification",     assignee:"R. Singh",  target:"2026-09-28", status:"WIP"  },
        { id:"RWI-2070", component:"Message Broker",  title:"Exactly-once semantics",     assignee:"D. Brown",  target:"2026-10-10", status:"Open" },
      ],
    },
  },
  DDS: {
    "Infra App": {
      qa: [
        { id:"RWI-3010", component:"Storage Engine",  title:"Compression upgrade",         assignee:"T. Wang",   target:"2026-08-15", status:"Done" },
        { id:"RWI-3022", component:"Query Router",    title:"Plan cache optimization",     assignee:"E. Diaz",   target:"2026-09-02", status:"Done" },
        { id:"RWI-3035", component:"Backup Service",  title:"Incremental snapshot",        assignee:"T. Wang",   target:"2026-09-20", status:"Done" },
        { id:"RWI-3048", component:"Storage Engine",  title:"Checksum validation",         assignee:"E. Diaz",   target:"2026-10-05", status:"WIP"  },
      ],
    },
  },
};

const APP_ORDER = ["Infra App", "Ham App", "Other App"];
const ENVS = [
  { key:"qa",   label:"QA" },
  { key:"pre",  label:"Pre-Production" },
  { key:"prod", label:"Production" },
];

/* ============ STATE ============ */
let currentProject = "F24";
let currentApp = "Infra App";
let currentEnv = "qa";
let charts = {};
const $ = (s) => document.querySelector(s);

/* ============ TAB STRIPS ============ */
function renderTabStrip(elId, items, currentKey, onClick) {
  const box = $(elId);
  box.innerHTML = items.map(it => `
    <div class="tab ${it.key === currentKey ? 'active' : ''}" data-k="${it.key}">${it.label}</div>
  `).join("");
  box.querySelectorAll(".tab").forEach(el => {
    el.onclick = () => onClick(el.dataset.k);
  });
}

function renderMainTabs() {
  renderTabStrip("#tabsMain",
    Object.keys(DATA).map(p => ({ key: p, label: p })),
    currentProject,
    (k) => { currentProject = k; currentApp = APP_ORDER[0]; currentEnv = "qa"; renderAll(); });
}

function renderSubTabs() {
  renderTabStrip("#tabsSub",
    APP_ORDER.map(a => ({ key: a, label: a })),
    currentApp,
    (k) => { currentApp = k; renderAll(); });
}

function renderEnvTabs() {
  renderTabStrip("#tabsEnv", ENVS, currentEnv,
    (k) => { currentEnv = k; renderEnvDetail(); });
}

/* ============ OVERALL CHARTS ============ */
function renderOverall() {
  const proj = DATA[currentProject];
  const appVals = APP_ORDER.map(a => {
    const e = proj.apps[a]; return Math.round((e.qa + e.pre + e.prod) / 3);
  });

  if (charts.bar) charts.bar.destroy();
  charts.bar = new Chart($("#overallBar"), {
    type: "bar",
    data: {
      labels: APP_ORDER,
      datasets: [{ label: "Avg Completion %", data: appVals,
        backgroundColor: ["#818cf8", "#818cf8", "#818cf8"], borderRadius: 0, barThickness: 44 }],
    },
    options: chartOpts("bar"),
  });

  if (charts.pie) charts.pie.destroy();
  charts.pie = new Chart($("#overallPie"), {
    type: "doughnut",
    data: {
      labels: ["Completed", "Remaining"],
      datasets: [{ data: [proj.overall.done, proj.overall.remaining],
        backgroundColor: ["#2563eb", "#e5e7eb"], borderWidth: 0, cutout: "70%" }],
    },
    options: { plugins: { legend: { position: "bottom", labels: { color: "#374151" } } } },
  });
}

function chartOpts(type) {
  return {
    plugins: { legend: { display: false } },
    scales: type === "bar" ? {
      y: { beginAtZero: true, max: 100, ticks: { color: "#6b7280" }, grid: { color: "#e5e7eb" } },
      x: { ticks: { color: "#6b7280" }, grid: { display: false } },
    } : {},
  };
}

/* ============ ENV DETAIL (chart + RWI table) ============ */
function renderEnvDetail() {
  const envs = DATA[currentProject].apps[currentApp];
  const val = envs[currentEnv];
  const envLabel = ENVS.find(e => e.key === currentEnv).label;
  const statusText = val >= 90 ? "Healthy" : val >= 50 ? "In Progress" : "At Risk";

  // RWI rows for current project/app/env (only QA has data in mock)
  const rows = (RWI_DATA[currentProject] && RWI_DATA[currentProject][currentApp] && RWI_DATA[currentProject][currentApp][currentEnv]) || [];

  const box = $("#envDetail");
  box.innerHTML = `
    <div class="env-summary">
      <h4>${envLabel} &mdash; Completion</h4>
      <div class="env-pct-big">${val}<span>%</span></div>
      <div class="env-status">${statusText}</div>
      <div class="env-mini"><canvas id="envMiniChart"></canvas></div>
    </div>
    <div class="rwi-section">
      <h4>
        <span>Release Work Items (last 3 months)</span>
        <span class="hint">${rows.length} item${rows.length !== 1 ? 's' : ''}</span>
      </h4>
      ${rows.length ? `
        <table class="rwi">
          <thead>
            <tr>
              <th>RWI ID</th>
              <th>Component</th>
              <th>Title</th>
              <th>Assignee</th>
              <th>Target Date</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            ${rows.map(r => `
              <tr>
                <td class="pill-id">${r.id}</td>
                <td>${r.component}</td>
                <td>${r.title}</td>
                <td>${r.assignee}</td>
                <td>${r.target}</td>
                <td><span class="pill ${r.status.toLowerCase()}">${r.status}</span></td>
              </tr>
            `).join("")}
          </tbody>
        </table>
      ` : `<div class="empty-note">No release work items tracked for ${envLabel} yet &mdash; completion is computed from overall progress.</div>`}
    </div>
  `;

  // mini progress bar (Done vs Todo)
  if (charts.mini) charts.mini.destroy();
  charts.mini = new Chart($("#envMiniChart"), {
    type: "bar",
    data: {
      labels: ["Done", "Todo"],
      datasets: [{ data: [val, 100 - val], backgroundColor: ["#2563eb", "#e5e7eb"], borderRadius: 0, barThickness: 40 }],
    },
    options: {
      indexAxis: "y",
      plugins: { legend: { display: false }, tooltip: { enabled: false } },
      scales: { x: { beginAtZero: true, max: 100, display: false }, y: { display: false } },
    },
  });
}

/* ============ MAIN ============ */
function renderAll() {
  renderMainTabs();
  renderSubTabs();
  renderOverall();
  renderEnvTabs();
  renderEnvDetail();
}
renderAll();
</script>
</body>
</html>
