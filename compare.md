```
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>DevOps Engineering Excellence</title>
<style>
  /* 基础重置与紧凑尺寸限制 (16:9 比例) */
  body { margin: 0; padding: 10px; background: #0f172a; color: #e2e8f0; font-family: system-ui, -apple-system, sans-serif; font-size: 13px; }
  .ppt-container { max-width: 1280px !important; margin: 0 auto !important; height: 720px !important; overflow: hidden !important; background: #1e293b; border-radius: 12px; padding: 20px; box-sizing: border-box; position: relative; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
  
  /* 头部标题区 */
  .header { text-align: center; margin-bottom: 15px; }
  .header h1 { margin: 0 0 5px 0; font-size: 22px; color: #38bdf8; }
  .header p { margin: 0; font-size: 13px; color: #94a3b8; }
  
  /* 三大支柱网格布局 */
  .pillars-grid { display: grid !important; grid-template-columns: 1fr 1fr 1fr !important; gap: 15px !important; height: 480px !important; }
  .pillar-col { background: #334155; border-radius: 8px; padding: 12px; border-top: 3px solid #38bdf8; }
  .pillar-col:nth-child(2) { border-top-color: #f472b6; }
  .pillar-col:nth-child(3) { border-top-color: #a78bfa; }
  .pillar-title { font-size: 15px; font-weight: bold; margin-bottom: 12px; text-align: center; color: #fff; }
  
  /* 节点卡片列表 */
  .node-list { display: flex; flex-direction: column; gap: 8px; }
  .node-card { background: #475569; padding: 10px; border-radius: 6px; cursor: pointer; transition: transform 0.2s, background 0.2s; display: flex; align-items: center; gap: 10px; }
  .node-card:hover { transform: translateX(3px); background: #64748b; }
  .node-icon { font-size: 18px; }
  .node-text h4 { margin: 0 0 2px 0; font-size: 13px; color: #38bdf8; }
  .node-text p { margin: 0; font-size: 11px; color: #cbd5e1; }
  
  /* 底部总结 */
  .footer-summary { position: absolute; bottom: 20px; left: 20px; right: 20px; background: #334155; padding: 10px; border-radius: 8px; text-align: center; font-size: 12px; color: #94a3b8; }
  
  /* 交互弹窗 (Sub-page) */
  .detail-panel { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: rgba(15,23,42,0.95); z-index: 10; display: none; flex-direction: column; padding: 30px; box-sizing: border-box; border-radius: 12px; }
  .detail-panel.active { display: flex !important; }
  .detail-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; border-bottom: 1px solid #475569; padding-bottom: 10px; }
  .detail-header h2 { margin: 0; color: #38bdf8; font-size: 20px; }
  .close-btn { background: #ef4444; color: white; border: none; padding: 6px 12px; border-radius: 4px; cursor: pointer; font-size: 12px; }
  .detail-content { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; font-size: 13px; }
  .detail-box { background: #334155; padding: 15px; border-radius: 8px; }
  .detail-box h3 { margin: 0 0 10px 0; color: #f472b6; font-size: 15px; }
  .detail-box ul { margin: 0; padding-left: 20px; }
  .detail-box li { margin-bottom: 5px; }
  .benefit-tag { margin-top: 15px; background: #065f46; color: #a7f3d0; padding: 10px; border-radius: 6px; text-align: center; font-weight: bold; font-size: 13px; }
</style>
</head>
<body>

<div class="ppt-container">
  <div class="header">
    <h1>DevOps Engineering Excellence</h1>
    <p>Driven by Automation · Secured by Validation · Optimized by Intelligence</p>
  </div>
  
  <div class="pillars-grid">
    <!-- Pillar 1 -->
    <div class="pillar-col">
      <div class="pillar-title">DB Automation</div>
      <div class="node-list">
        <div class="node-card" onclick="showDetail('db', 'migration')">
          <span class="node-icon">🚀</span>
          <div class="node-text"><h4>Schema Migration</h4><p>Auto CI/CD deploys</p></div>
        </div>
        <div class="node-card" onclick="showDetail('db', 'drift')">
          <span class="node-icon">🔍</span>
          <div class="node-text"><h4>Data Drift Detect</h4><p>Schema monitoring</p></div>
        </div>
        <div class="node-card" onclick="showDetail('db', 'backup')">
          <span class="node-icon">💾</span>
          <div class="node-text"><h4>Auto Backup</h4><p>1-click recovery</p></div>
        </div>
        <div class="node-card" onclick="showDetail('db', 'sync')">
          <span class="node-icon">🔄</span>
          <div class="node-text"><h4>Cross-Env Sync</h4><p>Mask & sync data</p></div>
        </div>
      </div>
    </div>
    
    <!-- Pillar 2 -->
    <div class="pillar-col">
      <div class="pillar-title">Cloud Validation Auto-Fix</div>
      <div class="node-list">
        <div class="node-card" onclick="showDetail('cloud', 'compliance')">
          <span class="node-icon">🛡️</span>
          <div class="node-text"><h4>Compliance Fix</h4><p>Auto-remediate drift</p></div>
        </div>
        <div class="node-card" onclick="showDetail('cloud', 'tf')">
          <span class="node-icon">📝</span>
          <div class="node-text"><h4>TF Validation</h4><p>Policy check plan</p></div>
        </div>
        <div class="node-card" onclick="showDetail('cloud', 'patch')">
          <span class="node-icon">🩹</span>
          <div class="node-text"><h4>Auto Patching</h4><p>CVE fix workflow</p></div>
        </div>
        <div class="node-card" onclick="showDetail('cloud', 'heal')">
          <span class="node-icon">⚙️</span>
          <div class="node-text"><h4>Self-Healing</h4><p>Restart unhealthy</p></div>
        </div>
      </div>
    </div>
    
    <!-- Pillar 3 -->
    <div class="pillar-col">
      <div class="pillar-title">Cloud Cost Control</div>
      <div class="node-list">
        <div class="node-card" onclick="showDetail('cost', 'anomaly')">
          <span class="node-icon">📈</span>
          <div class="node-text"><h4>Anomaly Detect</h4><p>ML spend alert</p></div>
        </div>
        <div class="node-card" onclick="showDetail('cost', 'idle')">
          <span class="node-icon">⏸️</span>
          <div class="node-text"><h4>Idle Auto-Stop</h4><p>Off-hour shutdown</p></div>
        </div>
        <div class="node-card" onclick="showDetail('cost', 'rightsize')">
          <span class="node-icon">📏</span>
          <div class="node-text"><h4>Right-Sizing</h4><p>Optimize specs</p></div>
        </div>
        <div class="node-card" onclick="showDetail('cost', 'showback')">
          <span class="node-icon">🏷️</span>
          <div class="node-text"><h4>Showback</h4><p>Team chargeback</p></div>
        </div>
      </div>
    </div>
  </div>
  
  <div class="footer-summary">
    Goal: Achieve <b>Zero-Touch Ops</b> | Reduce Toil by <b>40%</b> | Save Cloud Cost by <b>25%</b>
  </div>
  
  <!-- 交互弹窗 Sub-page -->
  <div class="detail-panel" id="detailPanel">
    <div class="detail-header">
      <h2 id="dTitle">Details</h2>
      <button class="close-btn" onclick="closeDetail()">Close ✕</button>
    </div>
    <div class="detail-content" id="dContent">
      <!-- 动态注入内容 -->
    </div>
    <div class="benefit-tag" id="dBenefit">Business Benefit</div>
  </div>
</div>

<script>
const dbData = {
  migration: { title: "Schema Migration CI", steps: ["Commit SQL", "Auto Test", "Deploy to Prod"], metrics: [["95%", "faster"], ["0", "manual"]] },
  drift: { title: "Data Drift Detection", steps: ["Scan schema", "Compare base", "Alert"], metrics: [["100%", "coverage"], ["5m", "alert"]] },
  backup: { title: "Auto Backup & Recovery", steps: ["Daily dump", "Encrypt", "Verify"], metrics: [["99.9%", "SLA"], ["10m", "restore"]] },
  sync: { title: "Cross-Env Sync", steps: ["Mask data", "Export", "Import"], metrics: [["Daily", "sync"], ["Zero", "PII leak"]] }
};
const cloudData = {
  compliance: { title: "Compliance Auto-Fix", steps: ["Detect", "Run playbook", "Verify"], metrics: [["75%", "less manual"], ["24/7", "monitor"]] },
  tf: { title: "TF Validation", steps: ["Hook CI", "Policy check", "Pass/Fail"], metrics: [["80%", "risks blocked"], ["3m", "scan"]] },
  patch: { title: "Auto Patching", steps: ["Scan CVE", "Build image", "Rollout"], metrics: [["90%", "CVEs fixed"], ["Zero", "downtime"]] },
  heal: { title: "Self-Healing", steps: ["Health check", "Restart", "Notify"], metrics: [["30%", "less pages"], ["99.9%", "uptime"]] }
};
const costData = {
  anomaly: { title: "Cost Anomaly Detection", steps: ["Collect", "ML check", "Alert"], metrics: [["90%", "detected"], ["15m", "alert"]] },
  idle: { title: "Idle Auto-Stop", steps: ["Tag", "Stop 7PM", "Start 9AM"], metrics: [["35%", "cost drop"], ["Zero", "impact"]] },
  rightsize: { title: "Right-Sizing", steps: ["Check load", "Suggest", "Resize"], metrics: [["20%", "saved"], ["P95", "steady"]] },
  showback: { title: "Tagging & Showback", steps: ["Define tags", "Enforce", "Report"], metrics: [["100%", "allocated"], ["1h", "gen"]] }
};

function showDetail(category, key) {
  let data;
  if(category === 'db') data = dbData[key];
  if(category === 'cloud') data = cloudData[key];
  if(category === 'cost') data = costData[key];
  
  if(!data) return;
  
  document.getElementById('dTitle').innerText = data.title;
  document.getElementById('dContent').innerHTML = `
    <div class="detail-box">
      <h3>Workflow</h3>
      <ul>${data.steps.map(s => `<li>${s}</li>`).join('')}</ul>
    </div>
    <div class="detail-box">
      <h3>Key Metrics</h3>
      <ul>${data.metrics.map(m => `<li><b>${m[0]}</b> ${m[1]}</li>`).join('')}</ul>
    </div>
  `;
  document.getElementById('dBenefit').innerText = "Business Benefit: Improves efficiency, security, and reduces operational costs.";
  document.getElementById('detailPanel').classList.add('active');
}

function closeDetail() {
  document.getElementById('detailPanel').classList.remove('active');
}
</script>
</body>
</html>
