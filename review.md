```
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>DevOps Engineering Excellence</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    background-color: #f0f4f8;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    color: #1e293b;
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
    overflow: hidden;
  }
  .slide-container {
    width: 1280px;
    height: 720px;
    background: #ffffff;
    border-radius: 16px;
    box-shadow: 0 10px 40px rgba(0,0,0,0.08);
    padding: 30px 40px;
    display: flex;
    flex-direction: column;
    position: relative;
    animation: fadeIn 0.6s ease-out;
  }
  @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
  
  .header {
    text-align: center;
    margin-bottom: 25px;
  }
  .header h1 {
    font-size: 28px;
    color: #0f172a;
    letter-spacing: -0.5px;
  }
  .header p {
    font-size: 14px;
    color: #64748b;
    margin-top: 8px;
  }
  
  .pillars-wrapper {
    display: flex;
    gap: 20px;
    flex: 1;
    min-height: 0;
  }
  .pillar {
    flex: 1;
    border-radius: 12px;
    padding: 20px;
    display: flex;
    flex-direction: column;
    border: 2px solid;
    background: #ffffff;
    box-shadow: 0 4px 15px rgba(0,0,0,0.03);
    transition: transform 0.3s ease;
  }
  .pillar-db { border-color: #bae6fd; }
  .pillar-cloud { border-color: #ddd6fe; }
  .pillar-cost { border-color: #bbf7d0; }
  
  .pillar-header {
    font-size: 16px;
    font-weight: 700;
    margin-bottom: 15px;
    padding-bottom: 10px;
    border-bottom: 2px solid;
    text-align: center;
  }
  .pillar-db .pillar-header { color: #0284c7; border-color: #e0f2fe; background: #f0f9ff; border-radius: 8px; padding: 10px;}
  .pillar-cloud .pillar-header { color: #7c3aed; border-color: #ede9fe; background: #f5f3ff; border-radius: 8px; padding: 10px;}
  .pillar-cost .pillar-header { color: #15803d; border-color: #dcfce7; background: #f0fdf4; border-radius: 8px; padding: 10px;}
  
  .nodes-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    overflow-y: auto;
  }
  .node-card {
    display: flex;
    align-items: center;
    background: #ffffff;
    padding: 12px 15px;
    border-radius: 10px;
    cursor: pointer;
    border: 1px solid #e2e8f0;
    box-shadow: 0 2px 8px rgba(0,0,0,0.04);
    transition: all 0.25s ease;
  }
  .node-card:hover {
    transform: translateX(5px) scale(1.02);
    box-shadow: 0 6px 15px rgba(0,0,0,0.08);
    border-color: #cbd5e1;
  }
  .pillar-db .node-card:hover { border-color: #7dd3fc; background: #f8fafc; }
  .pillar-cloud .node-card:hover { border-color: #a78bfa; background: #f8fafc; }
  .pillar-cost .node-card:hover { border-color: #86efac; background: #f8fafc; }
  
  .node-icon {
    font-size: 22px;
    margin-right: 15px;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
  }
  .pillar-db .node-icon { background: #e0f2fe; }
  .pillar-cloud .node-icon { background: #ede9fe; }
  .pillar-cost .node-icon { background: #dcfce7; }
  
  .node-info h3 {
    font-size: 14px;
    color: #1e293b;
    margin-bottom: 4px;
    font-weight: 600;
  }
  .node-info p {
    font-size: 12px;
    color: #64748b;
  }
  
  .footer-summary {
    text-align: center;
    margin-top: 20px;
    font-size: 13px;
    color: #475569;
    background: #f8fafc;
    padding: 12px;
    border-radius: 8px;
    border: 1px dashed #cbd5e1;
  }
  
  /* Interactive Sub-page (Modal) */
  .detail-panel {
    position: absolute;
    top: 0; left: 0; right: 0; bottom: 0;
    background: rgba(15, 23, 42, 0.4);
    backdrop-filter: blur(4px);
    display: flex;
    justify-content: center;
    align-items: center;
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.3s ease;
    z-index: 10;
    border-radius: 16px;
  }
  .detail-panel.active {
    opacity: 1;
    pointer-events: all;
  }
  .detail-content-box {
    background: #ffffff;
    width: 600px;
    border-radius: 16px;
    padding: 30px;
    box-shadow: 0 20px 50px rgba(0,0,0,0.15);
    transform: translateY(20px) scale(0.95);
    transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
    border-top: 5px solid #38bdf8;
  }
  .detail-panel.active .detail-content-box {
    transform: translateY(0) scale(1);
  }
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 15px;
    border-bottom: 1px solid #e2e8f0;
  }
  .detail-header h2 {
    font-size: 20px;
    color: #0f172a;
  }
  .close-btn {
    background: #f1f5f9;
    border: none;
    padding: 8px 12px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 13px;
    color: #475569;
    font-weight: 600;
    transition: background 0.2s;
  }
  .close-btn:hover { background: #e2e8f0; }
  
  .detail-grid {
    display: flex;
    gap: 20px;
    margin-bottom: 20px;
  }
  .detail-box {
    flex: 1;
    background: #f8fafc;
    padding: 15px;
    border-radius: 8px;
    border: 1px solid #e2e8f0;
  }
  .detail-box h3 {
    font-size: 13px;
    color: #64748b;
    margin-bottom: 10px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
  .detail-box ul {
    list-style: none;
    font-size: 13px;
    color: #334155;
  }
  .detail-box ul li {
    margin-bottom: 6px;
    position: relative;
    padding-left: 12px;
  }
  .detail-box ul li::before {
    content: "•";
    color: #38bdf8;
    position: absolute;
    left: 0;
    font-weight: bold;
  }
  .benefit-tag {
    background: #ecfdf5;
    color: #059669;
    padding: 12px 15px;
    border-radius: 8px;
    font-size: 13px;
    border-left: 4px solid #10b981;
    font-weight: 500;
  }
</style>
</head>
<body>

<div class="slide-container">
  <div class="header">
    <h1>DevOps Engineering Excellence</h1>
    <p>Automation Pillars & Operational Metrics Overview</p>
  </div>
  
  <div class="pillars-wrapper">
    <!-- Pillar 1 -->
    <div class="pillar pillar-db">
      <div class="pillar-header">DB Automation</div>
      <div class="nodes-list">
        <div class="node-card" onclick="showDetail('db', 'migration')">
          <div class="node-icon">🗄️</div>
          <div class="node-info"><h3>Schema Migration</h3><p>Auto CI/CD Deploys</p></div>
        </div>
        <div class="node-card" onclick="showDetail('db', 'drift')">
          <div class="node-icon">🔍</div>
          <div class="node-info"><h3>Data Drift Detect</h3><p>Structure Monitoring</p></div>
        </div>
        <div class="node-card" onclick="showDetail('db', 'backup')">
          <div class="node-icon">💾</div>
          <div class="node-info"><h3>Auto Backup</h3><p>1-Click Recovery</p></div>
        </div>
        <div class="node-card" onclick="showDetail('db', 'sync')">
          <div class="node-icon">🔄</div>
          <div class="node-info"><h3>Cross-Env Sync</h3><p>Mask & Sync Data</p></div>
        </div>
      </div>
    </div>
    
    <!-- Pillar 2 -->
    <div class="pillar pillar-cloud">
      <div class="pillar-header">Cloud Validation Auto-Fix</div>
      <div class="nodes-list">
        <div class="node-card" onclick="showDetail('cloud', 'compliance')">
          <div class="node-icon">🛡️</div>
          <div class="node-info"><h3>Compliance Fix</h3><p>Auto-Remediate Drift</p></div>
        </div>
        <div class="node-card" onclick="showDetail('cloud', 'tf')">
          <div class="node-icon">📝</div>
          <div class="node-info"><h3>TF Validation</h3><p>Policy Checks</p></div>
        </div>
        <div class="node-card" onclick="showDetail('cloud', 'patch')">
          <div class="node-icon">🩹</div>
          <div class="node-info"><h3>Auto Patching</h3><p>CVE Fixes</p></div>
        </div>
        <div class="node-card" onclick="showDetail('cloud', 'heal')">
          <div class="node-icon">⚙️</div>
          <div class="node-info"><h3>Self-Healing</h3><p>Restart Resources</p></div>
        </div>
      </div>
    </div>
    
    <!-- Pillar 3 -->
    <div class="pillar pillar-cost">
      <div class="pillar-header">Cloud Cost Control</div>
      <div class="nodes-list">
        <div class="node-card" onclick="showDetail('cost', 'anomaly')">
          <div class="node-icon">📈</div>
          <div class="node-info"><h3>Anomaly Detect</h3><p>ML Spend Analysis</p></div>
        </div>
        <div class="node-card" onclick="showDetail('cost', 'idle')">
          <div class="node-icon">⏸️</div>
          <div class="node-info"><h3>Idle Auto-Stop</h3><p>Off-hours Savings</p></div>
        </div>
        <div class="node-card" onclick="showDetail('cost', 'rightsize')">
          <div class="node-icon">📏</div>
          <div class="node-info"><h3>Right-Sizing</h3><p>Optimize Specs</p></div>
        </div>
        <div class="node-card" onclick="showDetail('cost', 'showback')">
          <div class="node-icon">🏷️</div>
          <div class="node-info"><h3>Showback</h3><p>Team Chargeback</p></div>
        </div>
      </div>
    </div>
  </div>
  
  <div class="footer-summary">
    Goal: Achieve <b>Zero-Touch Ops</b> | Reduce Toil by <b>40%</b> | Save Cloud Cost by <b>25%</b>
  </div>
  
  <!-- Interactive Sub-page Modal -->
  <div class="detail-panel" id="detailPanel">
    <div class="detail-content-box" id="detailBox">
      <div class="detail-header">
        <h2 id="dTitle">Details</h2>
        <button class="close-btn" onclick="closeDetail()">Close ✕</button>
      </div>
      <div class="detail-grid">
        <div class="detail-box">
          <h3>Workflow</h3>
          <ul id="dSteps"></ul>
        </div>
        <div class="detail-box">
          <h3>Key Metrics</h3>
          <ul id="dMetrics"></ul>
        </div>
      </div>
      <div class="benefit-tag" id="dBenefit">Business Benefit</div>
    </div>
  </div>
</div>

<script>
const dbData = {
  migration: { title: "Schema Migration CI", steps: ["Commit SQL", "Auto Test", "Deploy to Prod"], metrics: [["95%", "faster deploys"], ["0", "manual steps"]] },
  drift: { title: "Data Drift Detection", steps: ["Scan schema", "Compare baseline", "Alert team"], metrics: [["100%", "coverage"], ["5m", "alert time"]] },
  backup: { title: "Auto Backup & Recovery", steps: ["Daily dump", "Encrypt & store", "Verify restore"], metrics: [["99.9%", "recovery SLA"], ["10m", "restore time"]] },
  sync: { title: "Cross-Env Sync", steps: ["Mask data", "Export dump", "Import to dev"], metrics: [["Daily", "sync frequency"], ["Zero", "PII leaks"]] }
};
const cloudData = {
  compliance: { title: "Compliance Auto-Fix", steps: ["Detect drift", "Run fix playbook", "Verify state"], metrics: [["75%", "less manual work"], ["24/7", "monitoring"]] },
  tf: { title: "TF Validation", steps: ["Hook into pipeline", "Run policy checks", "Pass/Fail build"], metrics: [["80%", "risks blocked"], ["3m", "scan time"]] },
  patch: { title: "Auto Patching", steps: ["Scan CVEs", "Build new image", "Rolling update"], metrics: [["90%", "CVEs fixed fast"], ["Zero", "downtime"]] },
  heal: { title: "Self-Healing", steps: ["Health check", "Restart resource", "Notify on-call"], metrics: [["30%", "less pages"], ["99.9%", "uptime"]] }
};
const costData = {
  anomaly: { title: "Cost Anomaly Detection", steps: ["Collect metrics", "ML analysis", "Send alert"], metrics: [["90%", "anomalies detected"], ["15m", "first alert"]] },
  idle: { title: "Idle Auto-Stop", steps: ["Tag resources", "Stop at 7PM", "Start at 9AM"], metrics: [["35%", "cost drop"], ["Zero", "dev impact"]] },
  rightsize: { title: "Right-Sizing", steps: ["Check usage load", "Suggest size", "Approve & resize"], metrics: [["20%", "compute saved"], ["P95", "latency steady"]] },
  showback: { title: "Tagging & Showback", steps: ["Define tags", "Enforce rules", "Monthly report"], metrics: [["100%", "cost allocated"], ["1h", "report gen"]] }
};

function showDetail(category, key) {
  let data;
  if(category === 'db') data = dbData[key];
  if(category === 'cloud') data = cloudData[key];
  if(category === 'cost') data = costData[key];
  
  if(!data) return;
  
  // Dynamically set modal top border color based on category
  const box = document.getElementById('detailBox');
  if(category === 'db') box.style.borderTopColor = '#0ea5e9';
  if(category === 'cloud') box.style.borderTopColor = '#8b5cf6';
  if(category === 'cost') box.style.borderTopColor = '#22c55e';
  
  document.getElementById('dTitle').innerText = data.title;
  document.getElementById('dSteps').innerHTML = data.steps.map(s => `<li>${s}</li>`).join('');
  document.getElementById('dMetrics').innerHTML = data.metrics.map(m => `<li><b>${m[0]}</b> ${m[1]}</li>`).join('');
  document.getElementById('dBenefit').innerText = "Business Benefit: Improves efficiency, security, and reduces operational costs.";
  
  document.getElementById('detailPanel').classList.add('active');
}

function closeDetail() {
  document.getElementById('detailPanel').classList.remove('active');
}
</script>
</body>
</html>
