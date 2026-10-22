```
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>Business-Driven Engineering Excellence</title>
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
  }
  .header {
    text-align: center;
    margin-bottom: 20px;
  }
  .header h1 {
    font-size: 26px;
    color: #0f172a;
    letter-spacing: -0.5px;
  }
  .header .tagline {
    font-size: 14px;
    color: #0284c7;
    font-weight: 500;
    margin-top: 6px;
  }
  .header .client-motto {
    font-size: 12px;
    color: #64748b;
    margin-top: 4px;
  }
  .business-stats {
    display: flex;
    justify-content: center;
    gap: 50px;
    margin-bottom: 20px;
  }
  .bstat {
    text-align: center;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    padding: 8px 25px;
  }
  .bstat-value {
    font-size: 20px;
    font-weight: 700;
    color: #0284c7;
  }
  .bstat-label {
    font-size: 11px;
    color: #64748b;
    margin-top: 2px;
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
    padding: 18px;
    display: flex;
    flex-direction: column;
    border: 2px solid;
    background: #ffffff;
    box-shadow: 0 4px 15px rgba(0,0,0,0.03);
  }
  .pillar-db { border-color: #bae6fd; }
  .pillar-cloud { border-color: #ddd6fe; }
  .pillar-cost { border-color: #bbf7d0; }

  .pillar-header {
    font-size: 15px;
    font-weight: 700;
    margin-bottom: 10px;
    padding: 10px;
    border-radius: 8px;
    text-align: center;
  }
  .pillar-db .pillar-header { color: #0284c7; background: #f0f9ff; border-bottom: 2px solid #7dd3fc; }
  .pillar-cloud .pillar-header { color: #7c3aed; background: #f5f3ff; border-bottom: 2px solid #c4b5fd; }
  .pillar-cost .pillar-header { color: #15803d; background: #f0fdf4; border-bottom: 2px solid #86efac; }

  .pillar-driver {
    font-size: 11px;
    color: #64748b;
    text-align: center;
    margin-bottom: 14px;
    font-style: italic;
    padding: 0 8px;
  }
  .nodes-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
    flex: 1;
    justify-content: space-between;
    overflow: hidden;
  }
  .node-card {
    display: flex;
    align-items: center;
    background: #ffffff;
    padding: 10px 14px;
    border-radius: 10px;
    cursor: pointer;
    border: 1px solid #e2e8f0;
    box-shadow: 0 2px 8px rgba(0,0,0,0.04);
    transition: all 0.25s ease;
  }
  .node-card:hover {
    transform: translateX(5px) scale(1.02);
    box-shadow: 0 6px 15px rgba(0,0,0,0.08);
  }
  .pillar-db .node-card:hover { border-color: #7dd3fc; }
  .pillar-cloud .node-card:hover { border-color: #a78bfa; }
  .pillar-cost .node-card:hover { border-color: #86efac; }

  .node-icon {
    font-size: 20px;
    margin-right: 12px;
    width: 38px;
    height: 38px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
  }
  .pillar-db .node-icon { background: #e0f2fe; }
  .pillar-cloud .node-icon { background: #ede9fe; }
  .pillar-cost .node-icon { background: #dcfce7; }

  .node-info h3 {
    font-size: 13px;
    color: #1e293b;
    margin-bottom: 3px;
    font-weight: 600;
  }
  .node-info p {
    font-size: 11px;
    color: #64748b;
  }
  .footer-summary {
    text-align: center;
    margin-top: 15px;
    font-size: 12px;
    color: #475569;
    background: #f8fafc;
    padding: 10px;
    border-radius: 8px;
    border: 1px dashed #cbd5e1;
  }
  .footer-summary b { color: #0284c7; }

  /* Modal - Sub-page */
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
  .detail-panel.active { opacity: 1; pointer-events: all; }
  .detail-content-box {
    background: #ffffff;
    width: 620px;
    border-radius: 16px;
    padding: 25px 30px;
    box-shadow: 0 20px 50px rgba(0,0,0,0.15);
    border-top: 5px solid #38bdf8;
  }
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 15px;
    padding-bottom: 12px;
    border-bottom: 1px solid #e2e8f0;
  }
  .detail-header h2 { font-size: 18px; color: #0f172a; }
  .close-btn {
    background: #f1f5f9;
    border: none;
    padding: 6px 12px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 12px;
    color: #475569;
    font-weight: 600;
    transition: background 0.2s;
  }
  .close-btn:hover { background: #e2e8f0; }
  .detail-desc {
    font-size: 13px;
    color: #475569;
    margin-bottom: 15px;
    line-height: 1.5;
  }
  .metric-cards {
    display: flex;
    gap: 12px;
    margin-bottom: 15px;
  }
  .metric-card {
    flex: 1;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 10px;
    text-align: center;
  }
  .metric-card .m-value {
    font-size: 17px;
    font-weight: 700;
    color: #0284c7;
  }
  .metric-card .m-label {
    font-size: 11px;
    color: #64748b;
    margin-top: 3px;
  }
  .detail-grid {
    display: flex;
    gap: 15px;
    margin-bottom: 15px;
  }
  .detail-box {
    flex: 1;
    background: #f8fafc;
    padding: 12px 15px;
    border-radius: 8px;
    border: 1px solid #e2e8f0;
  }
  .detail-box h3 {
    font-size: 12px;
    color: #64748b;
    margin-bottom: 8px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
  .detail-box ul {
    list-style: none;
    font-size: 12px;
    color: #334155;
  }
  .detail-box ul li {
    margin-bottom: 5px;
    position: relative;
    padding-left: 12px;
  }
  .detail-box ul li::before {
    content: "•";
    color: #38bdf8;
    position: absolute;
    left: 0;
  }
  .benefit-tag {
    background: #ecfdf5;
    color: #059669;
    padding: 10px 15px;
    border-radius: 8px;
    font-size: 12px;
    border-left: 4px solid #10b981;
  }
</style>
</head>
<body>

<div class="slide-container">
  <div class="header">
    <h1>Business-Driven Engineering Excellence</h1>
    <div class="tagline">Automation built for speed, reliability, and client trust</div>
    <div class="client-motto">Every automation is designed to protect the client experience and strengthen business outcomes</div>
  </div>

  <div class="business-stats">
    <div class="bstat">
      <div class="bstat-value">99.9%</div>
      <div class="bstat-label">Client Data Protection</div>
    </div>
    <div class="bstat">
      <div class="bstat-value">-50%</div>
      <div class="bstat-label">Time to Market</div>
    </div>
    <div class="bstat">
      <div class="bstat-value">Zero</div>
      <div class="bstat-label">Critical Client Incidents</div>
    </div>
  </div>

  <div class="pillars-wrapper">
    <!-- Pillar 1: DB -->
    <div class="pillar pillar-db">
      <div class="pillar-header">DB Automation</div>
      <div class="pillar-driver">Reliable data foundation for client-facing services</div>
      <div class="nodes-list">
        <div class="node-card" onclick="showDetail('db', 'vacuum')">
          <div class="node-icon">🧹</div>
          <div class="node-info"><h3>Auto Vacuum</h3><p>Weekly cleanup on 100+ instances</p></div>
        </div>
        <div class="node-card" onclick="showDetail('db', 'backup')">
          <div class="node-icon">💾</div>
          <div class="node-info"><h3>Backup & Verify</h3><p>Instance-level automated backup</p></div>
        </div>
        <div class="node-card" onclick="showDetail('db', 'index')">
          <div class="node-icon">📊</div>
          <div class="node-info"><h3>Index Analysis</h3><p>Cloud-run analysis, zero manual CRs</p></div>
        </div>
      </div>
    </div>

    <!-- Pillar 2: Cloud Validation -->
    <div class="pillar pillar-cloud">
      <div class="pillar-header">Cloud Validation Auto-Fix</div>
      <div class="pillar-driver">Secure operations ensure client trust</div>
      <div class="nodes-list">
        <div class="node-card" onclick="showDetail('cloud', 'compliance')">
          <div class="node-icon">🛡️</div>
          <div class="node-info"><h3>Compliance Fix</h3><p>Auto-remediate cloud drift</p></div>
        </div>
        <div class="node-card" onclick="showDetail('cloud', 'tf')">
          <div class="node-icon">📝</div>
          <div class="node-info"><h3>TF Policy Check</h3><p>Shift-left security validation</p></div>
        </div>
        <div class="node-card" onclick="showDetail('cloud', 'heal')">
          <div class="node-icon">⚙️</div>
          <div class="node-info"><h3>Self-Healing</h3><p>Protect uptime & client experience</p></div>
        </div>
      </div>
    </div>

    <!-- Pillar 3: Cost -->
    <div class="pillar pillar-cost">
      <div class="pillar-header">Cloud Cost Control</div>
      <div class="pillar-driver">Optimize spend to reinvest in client value</div>
      <div class="nodes-list">
        <div class="node-card" onclick="showDetail('cost', 'anomaly')">
          <div class="node-icon">📈</div>
          <div class="node-info"><h3>Anomaly Detection</h3><p>ML-driven cost alerting</p></div>
        </div>
        <div class="node-card" onclick="showDetail('cost', 'rightsize')">
          <div class="node-icon">📏</div>
          <div class="node-info"><h3>Right-Sizing</h3><p>Optimize cloud resources</p></div>
        </div>
        <div class="node-card" onclick="showDetail('cost', 'showback')">
          <div class="node-icon">🏷️</div>
          <div class="node-info"><h3>Cost Showback</h3><p>Business-aware allocation</p></div>
        </div>
      </div>
    </div>
  </div>

  <div class="footer-summary">
    Goal: <b>Zero-Touch Ops</b> · Reduce Toil by <b>40%</b> · Reinvest Savings into Client Innovation
  </div>

  <!-- Sub-page Modal -->
  <div class="detail-panel" id="detailPanel">
    <div class="detail-content-box" id="detailBox">
      <div class="detail-header">
        <h2 id="dTitle">Details</h2>
        <button class="close-btn" onclick="closeDetail()">Close ✕</button>
      </div>
      <div class="detail-desc" id="dDesc"></div>
      <div class="metric-cards" id="dMetrics"></div>
      <div class="detail-grid">
        <div class="detail-box">
          <h3>How It Works</h3>
          <ul id="dSteps"></ul>
        </div>
        <div class="detail-box">
          <h3>Key Outcomes</h3>
          <ul id="dOutcomes"></ul>
        </div>
      </div>
      <div class="benefit-tag" id="dBenefit"></div>
    </div>
  </div>
</div>

<script>
const dbData = {
  vacuum: {
    title: "Auto Vacuum",
    desc: "Periodic automated cleanup of expired and redundant indexes across 100+ Cloud SQL instances. Replaces manual DBA login work.",
    metrics: [
      { value: "100+", label: "DB instances" },
      { value: "8h", label: "saved per week" },
      { value: "100%", label: "automated" }
    ],
    steps: ["Schedule weekly Cloud Run job", "Scan expired indexes", "Auto-clean & report"],
    outcomes: ["No manual DBA login", "Zero missed cleanups", "Consistent DB performance"],
    benefit: "Saves roughly 400 human-hours per year and removes operational risk, ensuring a reliable data layer for client applications."
  },
  backup: {
    title: "Automated Backup & Verification",
    desc: "Instance-level and table-level backups for critical databases. Runs on Cloud Run jobs with verification of recoverability.",
    metrics: [
      { value: "100%", label: "backup coverage" },
      { value: "90%", label: "verified restores" },
      { value: "0", label: "missed backups" }
    ],
    steps: ["Cloud Run job triggers dump", "Encrypt & store snapshot", "Auto-verify restore"],
    outcomes: ["No manual backup scripts", "RPO risk minimized", "Fast recovery"],
    benefit: "Protects client data with automated recovery proof. Meets client SLA commitments without DBA intervention."
  },
  index: {
    title: "Automated Index Analysis",
    desc: "Automated index usage analysis using native commands. Replaces the previous monthly process of opening 100+ CRs.",
    metrics: [
      { value: "100+", label: "CRs eliminated" },
      { value: "5 days", label: "saved monthly" },
      { value: "Cloud Native", label: "Cloud Run based" }
    ],
    steps: ["Run analysis command", "Collect usage stats", "Auto-generate recommendations"],
    outcomes: ["No manual CR process", "Faster tuning cycles", "Cost-effective execution"],
    benefit: "Saves up to 200 human-hours per year and accelerates database performance improvements."
  }
};

const cloudData = {
  compliance: {
    title: "Compliance Auto-Fix",
    desc: "Detects and automatically remediates compliance drift in cloud infrastructure, without manual ops work.",
    metrics: [
      { value: "24/7", label: "monitoring" },
      { value: "-75%", label: "manual work" },
      { value: "100%", label: "audit ready" }
    ],
    steps: ["Detect configuration drift", "Run remediation playbook", "Verify & audit log"],
    outcomes: ["Always audit ready", "Reduces security risk", "Faster compliance checks"],
    benefit: "Builds client trust by ensuring secure infrastructure at all times, while reducing compliance overhead."
  },
  tf: {
    title: "TF Policy Validation",
    desc: "Validates every Terraform plan against security and cost policies before deployment, shifting left.",
    metrics: [
      { value: "80%", label: "risks blocked" },
      { value: "3 min", label: "scan time" },
      { value: "0", label: "post-deploy issues" }
    ],
    steps: ["Hook into CI pipeline", "Run policy checks", "Fail or pass build"],
    outcomes: ["Security issues prevented early", "Faster review cycles", "Fewer production incidents"],
    benefit: "Prevents risky deployments from reaching production, protecting client-facing services."
  },
  heal: {
    title: "Self-Healing Infrastructure",
    desc: "Automatically restarts or replaces unhealthy cloud resources, reducing downtime and manual intervention.",
    metrics: [
      { value: "99.9%", label: "uptime" },
      { value: "-30%", label: "on-call load" },
      { value: "<5 min", label: "auto recovery" }
    ],
    steps: ["Monitor health checks", "Detect failure", "Auto-restart & notify"],
    outcomes: ["Less downtime", "Reduced on-call burden", "Better client experience"],
    benefit: "Ensures continuous availability, directly protecting client-facing applications from disruption."
  }
};

const costData = {
  anomaly: {
    title: "Cost Anomaly Detection",
    desc: "ML-based detection of abnormal cloud spending patterns, alerting teams before budgets are exceeded.",
    metrics: [
      { value: "90%", label: "anomaly detection" },
      { value: "15 min", label: "alert time" },
      { value: "-20%", label: "cost variance" }
    ],
    steps: ["Collect spend metrics", "Run ML detection", "Alert owning team"],
    outcomes: ["No budget surprises", "Faster corrective action", "Better cost governance"],
    benefit: "Prevents unnecessary spend, freeing budget to reinvest into client-facing innovation."
  },
  rightsize: {
    title: "Right-Sizing Recommendation",
    desc: "Analyzes actual utilization and recommends optimal instance sizes to reduce cost without impacting performance.",
    metrics: [
      { value: "-25%", label: "compute cost" },
      { value: "P95", label: "latency steady" },
      { value: "Auto", label: "suggestion engine" }
    ],
    steps: ["Collect utilization metrics", "Compare with thresholds", "Apply or approve"],
    outcomes: ["Lower cloud bill", "No performance impact", "Smarter scaling decisions"],
    benefit: "Optimizes spend and enables more investment into product and client experience."
  },
  showback: {
    title: "Cost Tagging & Showback",
    desc: "Enforces resource tagging and generates monthly cost allocation reports, improving accountability.",
    metrics: [
      { value: "100%", label: "cost allocated" },
      { value: "1h", label: "report generation" },
      { value: "Auto", label: "tag enforcement" }
    ],
    steps: ["Define tag taxonomy", "Enforce tag rules", "Generate monthly report"],
    outcomes: ["Full cost transparency", "Team accountability", "Business-aware allocation"],
    benefit: "Connects cloud spend directly to business value, reinforcing a cost-conscious client-centric culture."
  }
};

function showDetail(category, key) {
  let data;
  if(category === 'db') data = dbData[key];
  if(category === 'cloud') data = cloudData[key];
  if(category === 'cost') data = costData[key];
  if(!data) return;

  const box = document.getElementById('detailBox');
  if(category === 'db') box.style.borderTopColor = '#0ea5e9';
  if(category === 'cloud') box.style.borderTopColor = '#8b5cf6';
  if(category === 'cost') box.style.borderTopColor = '#22c55e';

  document.getElementById('dTitle').innerText = data.title;
  document.getElementById('dDesc').innerText = data.desc;
  document.getElementById('dMetrics').innerHTML = data.metrics.map(m => `
    <div class="metric-card">
      <div class="m-value">${m.value}</div>
      <div class="m-label">${m.label}</div>
    </div>
  `).join('');
  document.getElementById('dSteps').innerHTML = data.steps.map(s => `<li>${s}</li>`).join('');
  document.getElementById('dOutcomes').innerHTML = data.outcomes.map(o => `<li>${o}</li>`).join('');
  document.getElementById('dBenefit').innerText = "Business Benefit: " + data.benefit;

  document.getElementById('detailPanel').classList.add('active');
}

function closeDetail() {
  document.getElementById('detailPanel').classList.remove('active');
}
</script>
</body>
</html>
