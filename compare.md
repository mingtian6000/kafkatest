```
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>DevOps Engineering Excellence</title>
<style>
  body {
    margin: 0;
    padding: 15px;
    background: #0f172a;
    color: #e2e8f0;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    font-size: 14px;
    height: 100vh;
    box-sizing: border-box;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }
  .header {
    text-align: center;
    margin-bottom: 15px;
  }
  .header h1 {
    margin: 0 0 10px 0;
    font-size: 26px;
    color: #38bdf8;
  }
  .stats {
    display: flex;
    justify-content: center;
    gap: 40px;
  }
  .stat {
    background: #1e293b;
    padding: 8px 20px;
    border-radius: 6px;
    border: 1px solid #334155;
  }
  .stat-val {
    font-size: 20px;
    font-weight: bold;
    color: #4ade80;
  }
  .stat-label {
    font-size: 12px;
    color: #94a3b8;
  }
  .pillars {
    display: flex;
    gap: 15px;
    flex: 1;
    min-height: 0;
  }
  .pillar {
    flex: 1;
    background: #1e293b;
    border-radius: 8px;
    padding: 12px;
    border: 1px solid #334155;
    display: flex;
    flex-direction: column;
  }
  .pillar-title {
    text-align: center;
    font-size: 16px;
    font-weight: bold;
    margin-bottom: 12px;
    color: #f472b6;
    border-bottom: 1px solid #334155;
    padding-bottom: 8px;
  }
  .nodes {
    display: flex;
    flex-direction: column;
    gap: 10px;
    flex: 1;
    justify-content: center;
  }
  .node-card {
    background: #0f172a;
    border: 1px solid #475569;
    border-radius: 6px;
    padding: 10px;
    cursor: pointer;
    transition: all 0.2s;
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .node-card:hover {
    border-color: #38bdf8;
    background: #1e293b;
    transform: scale(1.02);
  }
  .node-icon {
    font-size: 20px;
  }
  .node-info h3 {
    margin: 0;
    font-size: 13px;
    color: #e2e8f0;
  }
  .node-info p {
    margin: 3px 0 0 0;
    font-size: 11px;
    color: #94a3b8;
  }
  .detail-panel {
    position: fixed;
    bottom: 15px;
    left: 15px;
    right: 15px;
    background: #1e293b;
    border: 1px solid #38bdf8;
    border-radius: 8px;
    padding: 15px;
    display: none;
    z-index: 10;
    box-shadow: 0 -4px 15px rgba(0,0,0,0.3);
  }
  .detail-panel.active {
    display: block;
  }
  .detail-close {
    float: right;
    cursor: pointer;
    color: #94a3b8;
    font-size: 18px;
    line-height: 1;
  }
  .detail-close:hover {
    color: #fff;
  }
  .detail-grid {
    display: flex;
    gap: 30px;
    margin-top: 10px;
  }
  .detail-block {
    flex: 1;
  }
  .detail-block h4 {
    margin: 0 0 8px 0;
    color: #4ade80;
    font-size: 13px;
  }
  .detail-block ul {
    margin: 0;
    padding-left: 18px;
    font-size: 12px;
    color: #cbd5e1;
  }
  .detail-block li {
    margin-bottom: 4px;
  }
  .benefit {
    margin-top: 10px;
    padding-top: 10px;
    border-top: 1px dashed #475569;
    font-size: 12px;
    color: #f472b6;
  }
</style>
</head>
<body>

<div class="header">
  <h1>DevOps Engineering Excellence</h1>
  <div class="stats">
    <div class="stat"><div class="stat-val">15+</div><div class="stat-label">Auto Workflows</div></div>
    <div class="stat"><div class="stat-val">40%</div><div class="stat-label">Time Saved</div></div>
    <div class="stat"><div class="stat-val">$250k</div><div class="stat-label">Cost Reduced</div></div>
  </div>
</div>

<div class="pillars">
  <div class="pillar">
    <div class="pillar-title">DB Automation</div>
    <div class="nodes">
      <div class="node-card" onclick="showDetail('db', 'migration')">
        <div class="node-icon">🗄️</div>
        <div class="node-info"><h3>Schema Migration</h3><p>Auto CI/CD for DB</p></div>
      </div>
      <div class="node-card" onclick="showDetail('db', 'drift')">
        <div class="node-icon">🔍</div>
        <div class="node-info"><h3>Data Drift Check</h3><p>Detect & alert drift</p></div>
      </div>
      <div class="node-card" onclick="showDetail('db', 'backup')">
        <div class="node-icon">💾</div>
        <div class="node-info"><h3>Auto Backup</h3><p>Zero-downtime recovery</p></div>
      </div>
      <div class="node-card" onclick="showDetail('db', 'sync')">
        <div class="node-icon">🔄</div>
        <div class="node-info"><h3>Env Sync</h3><p>Sync dev/prod data</p></div>
      </div>
    </div>
  </div>
  
  <div class="pillar">
    <div class="pillar-title">Cloud Validation Auto-Fix</div>
    <div class="nodes">
      <div class="node-card" onclick="showDetail('cloud', 'compliance')">
        <div class="node-icon">🛡️</div>
        <div class="node-info"><h3>Compliance Fix</h3><p>Auto-remediate drift</p></div>
      </div>
      <div class="node-card" onclick="showDetail('cloud', 'terraform')">
        <div class="node-icon">🧱</div>
        <div class="node-info"><h3>TF Validation</h3><p>Plan policy checks</p></div>
      </div>
      <div class="node-card" onclick="showDetail('cloud', 'vuln')">
        <div class="node-icon">🔐</div>
        <div class="node-info"><h3>Auto Patching</h3><p>CVE fix without downtime</p></div>
      </div>
      <div class="node-card" onclick="showDetail('cloud', 'heal')">
        <div class="node-icon">⚙️</div>
        <div class="node-info"><h3>Self-Healing</h3><p>Auto-restart unhealthy</p></div>
      </div>
    </div>
  </div>
  
  <div class="pillar">
    <div class="pillar-title">Cloud Cost Control</div>
    <div class="nodes">
      <div class="node-card" onclick="showDetail('cost', 'anomaly')">
        <div class="node-icon">📈</div>
        <div class="node-info"><h3>Anomaly Detect</h3><p>ML-based alerting</p></div>
      </div>
      <div class="node-card" onclick="showDetail('cost', 'idle')">
        <div class="node-icon">⏸️</div>
        <div class="node-info"><h3>Idle Stop</h3><p>Off-hour auto-shutdown</p></div>
      </div>
      <div class="node-card" onclick="showDetail('cost', 'rightsize')">
        <div class="node-icon">📏</div>
        <div class="node-info"><h3>Right-Sizing</h3><p>Match load optimally</p></div>
      </div>
      <div class="node-card" onclick="showDetail('cost', 'tag')">
        <div class="node-icon">🏷️</div>
        <div class="node-info"><h3>Showback</h3><p>Team cost allocation</p></div>
      </div>
    </div>
  </div>
</div>

<div class="detail-panel" id="detailPanel">
  <span class="detail-close" onclick="closeDetail()">×</span>
  <div id="detailContent"></div>
</div>

<script>
const details = {
  db: {
    migration: { title: "Schema Migration CI", desc: "Automates database schema changes via CI/CD pipelines.", steps: ["Commit SQL", "Run dry-check", "Apply to DB"], metrics: [["95%", "faster deploys"], ["0", "manual steps"]] },
    drift: { title: "Data Drift Detection", desc: "Continuously monitors and alerts on data structure drift.", steps: ["Scan schema", "Compare baseline", "Alert team"], metrics: [["100%", "coverage"], ["5m", "alert time"]] },
    backup: { title: "Auto Backup & Recovery", desc: "Scheduled backups with one-click restore capabilities.", steps: ["Daily dump", "Encrypt & store", "Verify restore"], metrics: [["99.9%", "recovery SLA"], ["10m", "restore time"]] },
    sync: { title: "Cross-Env Sync", desc: "Syncs production data anonymization to dev environments.", steps: ["Mask data", "Export", "Import to dev"], metrics: [["Daily", "sync freq"], ["Zero", "PII leak"]] }
  },
  cloud: {
    compliance: { title: "Compliance Auto-Fix", desc: "Auto-remediates cloud security and compliance drifts.", steps: ["Detect drift", "Run fix playbook", "Verify state"], metrics: [["75%", "less manual work"], ["24/7", "monitoring"]] },
    terraform: { title: "Terraform Validation", desc: "Scans Terraform plans against policies before apply.", steps: ["Hook pipeline", "Policy check", "Pass/Fail"], metrics: [["80%", "risks blocked"], ["3m", "scan time"]] },
    vuln: { title: "Auto Patching", desc: "Patches vulnerable images automatically.", steps: ["Scan CVEs", "Build new image", "Rollout"], metrics: [["90%", "CVEs fixed fast"], ["Zero", "downtime"]] },
    heal: { title: "Self-Healing", desc: "Restarts or replaces unhealthy resources automatically.", steps: ["Health check", "Trigger action", "Notify"], metrics: [["30%", "less pages"], ["99.9%", "uptime"]] }
  },
  cost: {
    anomaly: { title: "Cost Anomaly Detection", desc: "Detects abnormal spending patterns via ML.", steps: ["Collect metrics", "ML analysis", "Alert"], metrics: [["90%", "detected"], ["15m", "first alert"]] },
    idle: { title: "Idle Auto-Stop", desc: "Stops dev/staging environments during off-hours.", steps: ["Tag resources", "Stop at 7PM", "Start at 9AM"], metrics: [["35%", "cost drop"], ["Zero", "dev impact"]] },
    rightsize: { title: "Right-Sizing", desc: "Suggests optimal instance sizes based on load.", steps: ["Check usage", "Suggest size", "Approve & resize"], metrics: [["20%", "compute saved"], ["P95", "latency steady"]] },
    tag: { title: "Tagging & Showback", desc: "Enforces tags and reports costs per team.", steps: ["Define tags", "Enforce rules", "Monthly report"], metrics: [["100%", "allocated"], ["1h", "report gen"]] }
  }
};

function showDetail(pillar, node) {
  const data = details[pillar][node];
  if(!data) return;
  document.getElementById('detailContent').innerHTML = `
    <h3 style="margin:0 0 5px 0; color:#38bdf8; font-size:16px;">${data.title}</h3>
    <p style="margin:0 0 10px 0; font-size:13px; color:#cbd5e1;">${data.desc}</p>
    <div class="detail-grid">
      <div class="detail-block">
        <h4>Workflow</h4>
        <ul>${data.steps.map(s => `<li>${s}</li>`).join('')}</ul>
      </div>
      <div class="detail-block">
        <h4>Key Metrics</h4>
        <ul>${data.metrics.map(m => `<li><b>${m[0]}</b> ${m[1]}</li>`).join('')}</ul>
      </div>
    </div>
    <div class="benefit"><b>Business Benefit:</b> Improves efficiency, security, and saves costs.</div>
  `;
  document.getElementById('detailPanel').classList.add('active');
}

function closeDetail() {
  document.getElementById('detailPanel').classList.remove('active');
}
</script>
</body>
</html>
