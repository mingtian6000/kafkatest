<table>
<thead>
<tr>
<th><strong>Component / Area</strong></th>
<th><strong>Memo Notes</strong></th>
<th><strong>Scope / Comment / Owner</strong></th>
</tr>
</thead>
<tbody>
<tr>
<td><strong>1. Data Flow Configuration</strong></td>
<td>
<ul>
<li><strong>Input & Output:</strong> Pub/Sub topic – do we need to create a new one, or can an existing topic be used?</li>
<li>Is the output bucket already defined, or is a new one required?</li>
</ul>
</td>
<td><strong>Decision Needed:</strong> Confirm with source/downstream teams on reusability.<br><strong>Owner:</strong> Data/Platform Engineer</td>
</tr>
<tr>
<td><strong>2. Dataflow Jobs</strong></td>
<td>
<ul>
<li>Are the Dataflow jobs already existing? Are they the FDR-related ones?</li>
</ul>
</td>
<td><strong>To Clarify:</strong> Identify existing jobs to avoid duplication.<br><strong>Owner:</strong> Data Engineer</td>
</tr>
<tr>
<td><strong>3. Permissions & IAM</strong></td>
<td>
<ul>
<li>Create a DTP service account and grant all necessary permissions (no existing account).</li>
</ul>
</td>
<td><strong>To Do (Required):</strong> Must follow the principle of least privilege.<br><strong>Owner:</strong> Security/Infra Engineer</td>
</tr>
<tr>
<td><strong>4. Compute Infrastructure (DTP VM - MIG)</strong></td>
<td>
<ul>
<li><strong>Build Files:</strong> Which config/Ansible files to use? Base image RHEL 8?</li>
<li><strong>Write Terraform:</strong> Code for health check, scheduler policy, image/CMEK, DNS records, etc. Put initial variable files in the config bucket.</li>
</ul>
</td>
<td><strong>Core Dev Work:</strong> All items required.<br><strong>Decision:</strong> Finalize base image & config management tool.<br><strong>Owner:</strong> Infrastructure Engineer</td>
</tr>
<tr>
<td><strong>5. Configuration, Testing & Validation</strong></td>
<td>
<ul>
<li>Configuration in Pulse UI.</li>
<li><strong>Testing:</strong>
<ol>
<li>Instance should start up and run.</li>
<li>Test data preparation – is business team involvement needed?</li>
<li>How to perform End-to-End (E2E) testing?</li>
</ol>
</li>
</ul>
</td>
<td><strong>To Do:</strong> Post-deployment steps.<br><strong>Decision:</strong> Define E2E test plan and data prep responsibility.<br><strong>Owner:</strong> QA Engineer (with Business Team collaboration)</td>
</tr>
<tr>
<td><strong>6. Security & Compliance</strong></strong></td>
<td>
<ul>
<li>Is there any certificate management related to DTP?</li>
<li>Anything else to consider?</li>
</ul>
</td>
<td><strong>To Clarify:</strong> Engage security/compliance team for review.<br><strong>Owner:</strong> Security Engineer</td>
</tr>
</tbody>
</table>
