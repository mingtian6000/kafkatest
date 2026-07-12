```
📌 Icon 的偷懒法：不用  emoticon  宏也行，直接往 XML 里塞 Unicode emoji（✅ ❌ ⚠️ 🟢 🔴）最稳，Confluence 渲染没问题，还少一层宏嵌套。
💡 最快的"抄作业"法：先在 Confluence 编辑页里手工拖出你想要的那套 Tab 效果（插件装好后），然后装 Source Editor 插件 → Tools → View Storage Format → 把那段 XML 拷出来，就是你要塞进 API 的模板。比你查文档快 10 倍。

<!-- 大标题 -->
<h1>Weekly Health Report - Project XXX</h1>

<!-- info 高亮块（重点字突出） -->
<ac:structured-macro ac:name="info">
  <ac:parameter ac:name="title">Summary</ac:parameter>
  <ac:rich-text-body>
    <p><strong>3 个 MIG 异常</strong>，Cloud SQL 全部 healthy，详见下表 👇</p>
  </ac:rich-text-body>
</ac:structured-macro>

<!-- 原生表格（也可以用 wiki markup ||表头|| 更短） -->
<table>
  <tbody>
    <tr><th>Project</th><th>MIG</th><th>Health</th><th>Icon</th></tr>
    <tr>
      <td>proj-A</td>
      <td>mig-web</td>
      <td><font color="green">RUNNABLE</font></td>
      <td><ac:emoticon ac:name="tick"/></td>
    </tr>
    <tr>
      <td>proj-B</td>
      <td>mig-worker</td>
      <td><font color="red">UNHEALTHY ×2</font></td>
      <td><ac:emoticon ac:name="cross"/></td>
    </tr>
  </tbody>
</table>

<!-- expand 折叠块，当伪 tab 用 -->
<ac:structured-macro ac:name="expand">
  <ac:parameter ac:name="title">查看异常详情</ac:parameter>
  <ac:rich-text-body>
    <p>这里是展开后才看的细节……</p>
  </ac:rich-text-body>
</ac:structured-macro>
