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
        </