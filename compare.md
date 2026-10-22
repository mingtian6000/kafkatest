```
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>AI-Powered Ops Excellence</title>
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
    padding: 24px 32px;
    display: flex;
    flex-direction: column;
    position: relative;
  }
  .header {
    text-align: center;
    margin-bottom: 12px;
  }
  .header h1 {
    font-size: 27px;
    color: #0f172a;
    letter-spacing: -0.5px;
  }
  .header .tagline {
    font-size: 14px;
    color: #6d28d9;
    font-weight: 500;
    margin-top: 5px;
  }
  .header .client-motto {
    font-size: 12px;
    color: #64748b;
    margin-top: 3px;
  }
  .business-stats {
    display: flex;
    justify-content: center;
    gap: 40px;
    margin-bottom: 12px;
  }
  .bstat {
    text-align: center;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    padding: 6px 24px;
  }
  .bstat-value {
    font-size: 21px;
    font-weight: 700;
    color: #6d28d9;
  }
  .bstat-label {
    font-size: 11px;
    color: #64748b;
    margin-top: 2px;
  }
  .pillars-wrapper {
    display: flex;
    gap: 14px;
    flex: 1;
    min-height: 0;
  }
  .pillar {
    flex: 1;
    border-radius: 12px;
    padding: 14px;
    display: flex;
    flex-direction: column;
    border: 2px solid;
    background: #ffffff;
    box-shadow: 0 4px 15px rgba(0,0,0,0.03);
  }
  .pillar-detect { border-color: #ddd6fe; }
  .pillar-workflow { border-color: #a5f3fc; }
  .pillar-knowledge { border-color: #bbf7d0; }
  .pillar-adoption { border-color: #fde68a; }

  .pillar-header {
    font-size: 13.5px;
    font-weight: 700;
    margin-bottom: 6px;
    padding: 7px;
    border-radius: 8px;
    text-align: center;
  }
  .pillar-detect .pillar-header { color: #6d28d9; background: #f5f3ff; border-bottom: 2px solid #c4b5fd; }
  .pillar-workflow .pillar-header { color: #0e7490; background: #ecfeff; border-bottom: 2px solid #67e8f9; }
  .pillar-knowledge .pillar-header { color: #15803d; background: #f0fdf4; border-bottom: 2px solid #86efac; }
  .pillar-adoption .pillar-header { color: #b45309; background: #fffbeb; border-bottom: 2px solid #fcd34d; }

  .pillar-driver {
    font-size: 10.5px;
    color: #64748b;
    text-align: center;
    margin-bottom: 8px;
    font-style: italic;
  }
  .nodes-list {
    display: flex;
    flex-direction: column;
    gap: 7px;
    flex: 1;
    justify-content: space-between;
    overflow: hidden;
  }
  .node-card {
    display: flex;
    align-items: center;
    background: #ffffff;
    padding: 6px 10px;
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
  .pillar-detect .node-card:hover { border-color: #a78bfa; }
  .pillar-workflow .node-card:hover { border-color: #22d3ee; }
  .pillar-knowledge .node-card:hover { border-color: #86efac; }
  .pillar-adoption .node-card:hover { border-color: #fbbf24; }

  .node-icon {
    font-size: 18px;
    margin-right: 10px;
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
  }
  .pillar-detect .node-icon { background: #ede9fe; }
  .pillar-workflow .node-icon { background: #cffafe; }
  .pillar-knowledge .node-icon { background: #dcfce7; }
  .pillar-adoption .node-icon { background: #fef3c7; }

  .node-info h3 {
    font-size: 12.5px;
    color: #1e293b;
    margin-bottom: 2px;
    font-weight: 600;
  }
  .node-info p {
    font-size: 10.5px;
    color: #64748b;
  }
  .footer-summary {
    text-align: center;
    margin-top: 10px;
    font-size: 12.5px;
    color: #475569;
    background: #f8fafc;
    padding: 9px;
    border-radius: 8px;
    border: 1px dashed #cbd5e1;
  }
  .footer-summary b { color: #6d28d9; }

  /* Modal / Sub-page */
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
    width: 650px;
    max-height: 620px;
    overflow-y: auto;
    border-radius: 16px;
    padding: 22px 28px;
    box-shadow: 0 20px 50px rgba(0,0,0,0.15);
    border-top: 5px solid #6d28d9;
  }
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
    padding-bottom: 8px;
    border-bottom: 1px solid #e2e8f0;
  }
  .detail-header h2 { font-size: 17px; color: #0f172a; }
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
    margin-bottom: 10px;
    line-height: 1.5;
  }
  .detail-img {
    width: 100%;
    height: 120px;
    border: 2px dashed #cbd5e1;
    border-radius: 8px;
    background: #f8fafc;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 10px;
    font-size: 12px;
    color: #94a3b8;
  }
  .metric-cards {
    display: flex;
    gap: 10px;
    margin-bottom: 10px;
  }
  .metric-card {
    flex: 1;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 7px;
    text-align: center;
  }
  .metric-card .m-value {
    font-size: 16px;
    font-weight: 700;
    color: #6d28d9;
  }
  .metric-card .m-label {
    font-size: 10.5px;
    color: #64748b;
    margin-top: 3px;
  }
  .detail-grid {
    display: flex;
    gap: 12px;
    margin-bottom: 10px;
  }
  .detail-box {
    flex: 1;
    background: #f8fafc;
    padding: 10px 12px;
    border-radius: 8px;
    border: 1px solid #e2e8f0;
  }
  .detail-box h3 {
    font-size: 11.5px;
    color: #64748b;
    margin-bottom: 6px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
  .detail-box ul {
    list-style: none;
    font-size: 12px;
    color: #334155;
  }
  .detail-box ul li {
    margin-bottom: 4px;
    position: relative;
    padding-left: 12px;
  }
  .detail-box ul li::before {
    content: "•";
    color: #6d28d9;
    position: absolute;
    left: 0;
  }
  .benefit-tag {
    background: #ecfdf5;
    color: #059669;
    padding: 9px 14px;
    border-radius: 8px;
    font-size: 12px;
    border-left: 4px solid #10b981;
  }
</style>
</head>
<body>

<div class="slide-container">
  <div class="header">
    <h1>AI-Powered Ops Excellence</h1>
    <div class="tagline">Not just AI for code — AI that runs GCP operations, fixes issues, and learns every time</div>
    <div class="client-motto">Every resolved incident makes the team faster, smarter, and more cost-efficient</div>
  </div>

  <div class="business-stats">
    <div class="bstat">
      <div class="bstat-value">-60%</div>
      <div class="bstat-label">MTTR</div>
    </div>
    <div class="bstat">
      <div class="bstat-value">-50%</div>
      <div class="bstat-label">On-Call Toil</div>
    </div>
    <div class="bstat">
      <div class="bstat-value">100%</div>
      <div class="bstat-label">Knowledge Captured</div>
    </div>
    <div class="bstat">
      <div class="bstat-value">-40%</div>
      <div class="bstat-label">Token Cost</div>
    </div>
  </div>

  <div class="pillars-wrapper">
    <!-- Pillar 1: AI Troubleshooting Agent -->
    <div class="pillar pillar-detect">
      <div class="pillar-header">AI Troubleshooting Agent</div>
      <div class="pillar-driver">Live GCP monitoring · Root cause · Fix suggestions</div>
      <div class="nodes-list">
        <div class="node-card" onclick="showDetail('detect', 'prometheus')">
          <div class="node-icon">📡</div>
          <div class="node-info"><h3>Prometheus Live Sync</h3><p>AI reads real-time metrics</p></div>
        </div>
        <div class="node-card" onclick="showDetail('detect', 'grafana')">
          <div class="node-icon">🔍</div>
          <div class="node-info"><h3>Grafana Root Cause</h3><p>Logs + metrics correlation</p></div>
        </div>
        <div class="node-card" onclick="showDetail('detect', 'autofix')">
          <div class="node-icon">🛠️</div>
          <div class="node-info"><h3>Auto Fix / Playbook</h3><p>Suggested actions & execution</p></div>
        </div>
      </div>
    </div>

    <!-- Pillar 2: AI Workflow Reshaper -->
    <div class="pillar pillar-workflow">
      <div class="pillar-header">AI Workflow Reshaper</div>
      <div class="pillar-driver">Every incident auto-documented · Team shared · Reusable</div>
      <div class="nodes-list">
        <div class="node-card" onclick="showDetail('workflow', 'markdown')">
          <div class="node-icon">📝</div>
          <div class="node-info"><h3>Troubleshoot to Markdown</h3><p>Auto-generated runbooks</p></div>
        </div>
        <div class="node-card" onclick="showDetail('workflow', 'hub')">
          <div class="node-icon">📚</div>
          <div class="node-info"><h3>Team Hub Sharing</h3><p>Published and searchable</p></div>
        </div>
        <div class="node-card" onclick="showDetail('workflow', 'reuse')">
          <div class="node-icon">♻️</div>
          <div class="node-info"><h3>Similar Case Matching</h3><p>Previous solutions reused</p></div>
        </div>
      </div>
    </div>

    <!-- Pillar 3: AI Knowledge Hub -->
    <div class="pillar pillar-knowledge">
      <div class="pillar-header">AI Knowledge Hub</div>
      <div class="pillar-driver">From firefighting to organizational memory</div>
      <div class="nodes-list">
        <div class="node-card" onclick="showDetail('knowledge', 'cross')">
          <div class="node-icon">🌐</div>
          <div class="node-info"><h3>Cross-Team Reuse</h3><p>Indexed across SRE teams</p></div>
        </div>
        <div class="node-card" onclick="showDetail('knowledge', 'assistant')">
          <div class="node-icon">🤖</div>
          <div class="node-info"><h3>Chat with AI Assistant</h3><p>Ask in natural language</p></div>
        </div>
        <div class="node-card" onclick="showDetail('knowledge', 'learn')">
          <div class="node-icon">📈</div>
          <div class="node-info"><h3>Continuous Improvement</h3><p>Cases refine AI runbooks</p></div>
        </div>
      </div>
    </div>

    <!-- Pillar 4: AI Adoption & Token Efficiency -->
    <div class="pillar pillar-adoption">
      <div class="pillar-header">AI Adoption & Token Efficiency</div>
      <div class="pillar-driver">AI only where it adds value · Deterministic tasks stay deterministic</div>
      <div class="nodes-list">
        <div class="node-card" onclick="showDetail('adoption', 'routing')">
          <div class="node-icon">🧭</div>
          <div class="node-info"><h3>Smart Task Routing</h3><p>Decide AI vs deterministic job</p></div>
        </div>
        <div class="node-card" onclick="showDetail('adoption', 'pipeline')">
          <div class="node-icon">⚙️</div>
          <div class="node-info"><h3>Cloud Run / Scheduler</h3><p>Deterministic workloads as jobs</p></div>
        </div>
        <div class="node-card" onclick="showDetail('adoption', 'token')">
          <div class="node-icon">🎯</div>
          <div class="node-info"><h3>Token Efficiency Guardrails</h3><p>Measure & cap token usage</p></div>
        </div>
      </div>
    </div>
  </div>

  <div class="footer-summary">
    Goal: <b>Incident Flywheel</b> — Repair it once · Document it once · Reuse it forever · Use AI wisely
  </div>

  <!-- Sub-page Modal -->
  <div class="detail-panel" id="detailPanel">
    <div class="detail-content-box" id="detailBox">
      <div class="detail-header">
        <h2 id="dTitle">Details</h2>
        <button class="close-btn" onclick="closeDetail()">Close ✕</button>
      </div>
      <div class="detail-desc" id="dDesc"></div>
      <div class="detail-img" id="dImage">Image placeholder</div>
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
const detectData = {
  prometheus: {
    title: "Prometheus Live Sync",
    desc: "AI agent continuously connects to Prometheus endpoints, monitors all GCP service metrics, and detects anomalies instantly.",
    imagePlaceholder: "Prometheus metrics dashboard screenshot — replace with real image",
    metrics: [
      { value: "500+", label: "metric streams" },
      { value: "< 3 min", label: "detection time" },
      { value: "24/7", label: "monitoring" }
    ],
    steps: ["Connect to Prometheus API", "Scan metric baselines", "Alert on anomaly"],
    outcomes: ["Faster alerting", "No reliance on manual dashboards", "Always-on watch"],
    benefit: "Drastically reduces mean time to detection and prevents client-facing incidents from lingering."
  },
  grafana: {
    title: "Grafana Root Cause Analysis",
    desc: "AI reads Grafana dashboards, correlates logs and metrics, and automatically isolates the root cause of an issue.",
    imagePlaceholder: "Grafana correlated logs + metrics view — replace with real image",
    metrics: [
      { value: "90%", label: "root cause accuracy" },
      { value: "< 5 min", label: "diagnosis time" },
      { value: "Multi-signal", label: "logs + metrics" }
    ],
    steps: ["Collect dashboard context", "Analyze error logs", "Pinpoint likely cause"],
    outcomes: ["Faster resolution", "Reduced human investigation", "Fewer escalation loops"],
    benefit: "Minimizes service downtime and directly improves client experience."
  },
  autofix: {
    title: "Auto Fix & Playbook Generation",
    desc: "After identifying root cause, AI provides a recommended fix, or executes it directly for low-risk cases.",
    imagePlaceholder: "AI generated fix playbook example — replace with real image",
    metrics: [
      { value: "70%", label: "fixable automatically" },
      { value: "30%", label: "needs human approval" },
      { value: "Zero", label: "missed runbooks" }
    ],
    steps: ["Match incident to known playbook", "Generate command/action", "Execute or route to engineer"],
    outcomes: ["Less human toil", "Faster MTTR", "Standardized responses"],
    benefit: "Frees engineers from repetitive troubleshooting and ensures fast, consistent recovery."
  }
};

const workflowData = {
  markdown: {
    title: "Troubleshoot to Markdown",
    desc: "Every troubleshooting session is automatically converted into a structured Markdown document, ready for sharing.",
    imagePlaceholder: "Sample Markdown incident doc — replace with real image",
    metrics: [
      { value: "100%", label: "sessions captured" },
      { value: "< 10 min", label: "generation time" },
      { value: "Auto", label: "structured format" }
    ],
    steps: ["AI tracks incident actions", "Generate Markdown outline", "Save to knowledge base"],
    outcomes: ["No manual write-ups", "Consistent structure", "Easy team sharing"],
    benefit: "Turns tribal knowledge into team assets, so no context is lost when engineers change roles."
  },
  hub: {
    title: "Team Hub Sharing",
    desc: "Markdown runbooks are automatically posted to the team hub and visible to the whole organization.",
    imagePlaceholder: "Team knowledge hub UI — replace with real image",
    metrics: [
      { value: "100%", label: "documentation coverage" },
      { value: "+60%", label: "team collaboration" },
      { value: "Live", label: "auto publication" }
    ],
    steps: ["Trigger share event", "Push to hub", "Notify in Teams"],
    outcomes: ["Transparent ops knowledge", "Reduced duplication", "Cross-team visibility"],
    benefit: "Creates a living knowledge base that accelerates every future incident response."
  },
  reuse: {
    title: "Similar Case Matching",
    desc: "When a new incident occurs, AI instantly searches past runbooks and recommends the most similar case.",
    imagePlaceholder: "Similar case matching search result — replace with real image",
    metrics: [
      { value: "< 1 min", label: "match time" },
      { value: "80%", label: "relevant suggestions" },
      { value: "-50%", label: "repeated issues" }
    ],
    steps: ["Extract incident signature", "Search vector database", "Present relevant docs"],
    outcomes: ["Faster decision making", "Lower repeat incidents", "Better onboarding"],
    benefit: "Engineers solve new problems on the shoulders of past experience, dramatically raising efficiency."
  }
};

const knowledgeData = {
  cross: {
    title: "Cross-Team Reuse",
    desc: "All generated documents are indexed, categorized, and made available to multiple SRE and DevOps teams.",
    imagePlaceholder: "Organization-wide knowledge index — replace with real image",
    metrics: [
      { value: "5+", label: "teams connected" },
      { value: "200+", label: "runbooks indexed" },
      { value: "Central", label: "single source of truth" }
    ],
    steps: ["Tag documents by service", "Sync to shared hub", "Track usage analytics"],
    outcomes: ["No duplicated effort", "Consistent fixes", "Org-wide efficiency"],
    benefit: "Investment in one team's AI becomes an asset for the entire engineering organization."
  },
  assistant: {
    title: "Chat with AI Assistant",
    desc: "Engineers can ask natural-language questions and get answers drawn from past troubleshooting knowledge.",
    imagePlaceholder: "Chat assistant answering with source docs — replace with real image",
    metrics: [
      { value: "24/7", label: "availability" },
      { value: "80%", label: "self-service success" },
      { value: "NL", label: "natural language" }
    ],
    steps: ["Ask question", "AI retrieves relevant docs", "Return actionable answer"],
    outcomes: ["Less context switching", "Faster training", "Empowered junior engineers"],
    benefit: "Every team member becomes a senior engineer able to solve incidents with confidence."
  },
  learn: {
    title: "Continuous Improvement",
    desc: "Each new incident, fix, and engineer correction feeds back into AI runbooks and prompts.",
    imagePlaceholder: "AI training / feedback loop diagram — replace with real image",
    metrics: [
      { value: "Monthly", label: "model refresh" },
      { value: "100%", label: "fixes applied" },
      { value: "Loop", label: "case-driven learning" }
    ],
    steps: ["Collect engineer feedback", "Update runbook templates", "Retrain AI prompts"],
    outcomes: ["AI gets smarter over time", "Runbooks stay current", "Higher future success rate"],
    benefit: "The team's AI capability compounds, making operations faster and more reliable every quarter."
  }
};

const adoptionData = {
  routing: {
    title: "Smart Task Routing",
    desc: "Every incoming task is first classified: deterministic, repetitive work goes to a Cloud Run job, while ambiguous tasks requiring judgment are routed to the AI agent.",
    imagePlaceholder: "Task routing decision diagram — replace with real image",
    metrics: [
      { value: "70%", label: "tasks stay deterministic" },
      { value: "30%", label: "AI-handled decisions" },
      { value: "0", label: "AI for the sake of AI" }
    ],
    steps: ["Classify task type", "Route to Cloud Run job or AI agent", "Track outcome & cost"],
    outcomes: ["No AI overuse", "Predictable deterministic flows", "Right tool for the job"],
    benefit: "Proves to senior management that AI is applied deliberately, not as a buzzword, maximizing ROI."
  },
  pipeline: {
    title: "Cloud Run / Scheduler Pipelines",
    desc: "For deterministic workloads, we keep using proven Cloud Run jobs and Cloud Scheduler — no LLM involved, cheaper and 100% reliable.",
    imagePlaceholder: "Cloud Run job pipeline diagram — replace with real image",
    metrics: [
      { value: "100%", label: "reliability" },
      { value: "Low", label: "operational cost" },
      { value: "0", label: "token spent" }
    ],
    steps: ["Identify deterministic task", "Wrap in Cloud Run job", "Schedule with Cloud Scheduler"],
    outcomes: ["Cost-efficient execution", "Reproducible results", "No hallucination risk"],
    benefit: "Saves significant AI budget by reserving AI only for genuinely ambiguous, high-value decisions."
  },
  token: {
    title: "Token Efficiency Guardrails",
    desc: "AI usage is continuously measured at the token level. We cap, cache, and optimize prompts so AI stays affordable at scale.",
    imagePlaceholder: "Token usage dashboard / guardrails — replace with real image",
    metrics: [
      { value: "-40%", label: "token cost YoY" },
      { value: "Capped", label: "per-task token limit" },
      { value: "Cached", label: "prompt reuse" }
    ],
    steps: ["Measure tokens per task", "Set per-task caps", "Cache & reuse prompts"],
    outcomes: ["AI spend predictable", "Scalable AI usage", "No bill surprises"],
    benefit: "Demonstrates disciplined, measurable AI adoption — a key differentiator that other teams lack."
  }
};

function showDetail(category, key) {
  let data;
  if(category === 'detect') data = detectData[key];
  if(category === 'workflow') data = workflowData[key];
  if(category === 'knowledge') data = knowledgeData[key];
  if(category === 'adoption') data = adoptionData[key];
  if(!data) return;

  const box = document.getElementById('detailBox');
  if(category === 'detect') box.style.borderTopColor = '#6d28d9';
  if(category === 'workflow') box.style.borderTopColor = '#0e7490';
  if(category === 'knowledge') box.style.borderTopColor = '#15803d';
  if(category === 'adoption') box.style.borderTopColor = '#b45309';

  document.getElementById('dTitle').innerText = data.title;
  document.getElementById('dDesc').innerText = data.desc;

  const imgEl = document.getElementById('dImage');
  if (data.imagePlaceholder) {
    imgEl.style.display = 'flex';
    imgEl.innerText = '🖼️ ' + data.imagePlaceholder;
  } else {
    imgEl.style.display = 'none';
  }

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
