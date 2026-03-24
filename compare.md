以下是符合 Confluence 页面兼容 和 Markdown 格式的文档，包含所有要求的结构（表格留空待填、流程描述、PR 逻辑、Stale Branch 检测规划）：

Branching Strategy (Enforced from FDZ 3.0)

Created by Alice Wang • Last updated Just a moment ago • 1 minute read

Background

Our codebase has suffered from uncontrolled branch proliferation, long - lived PRs (often open for months), and resulting code inconsistency across features/releases. This chaos has led to:

- Merging conflicts that are time - consuming to resolve.
- Inconsistent behavior between development, staging, and production environments.
- Difficulty in tracing which changes belong to which feature or release.

To address these issues, we are enforcing a standardized branching strategy from FDZ 3.0 onward.

Active Repos & Protected Branches

This section defines which repositories are active and which branches are protected (cannot be deleted or force - pushed without approval).

Repository Name Protected Branches Notes (e.g., Purpose)
[To be filled] [To be filled] [To be filled]

Core Reviews

This section outlines the core review process for pull requests (PRs).

Review Type Required Reviewers Approval Threshold Notes
[To be filled] [To be filled] [To be filled] [To be filled]

PR Process & Branch Workflow

We follow a two - stage PR workflow to ensure code quality and release stability:

1. Feature Development & Protection

- All 
"feature/*" branches are protected (no direct pushes; all changes require a PR).
- Developers create a 
"feature/<JIRA - TICKET - ID>-<SHORT - DESCRIPTION>" branch from 
"main" (or 
"develop").
- A PR must be opened from 
"feature/*" → 
"feature" (a shared “feature integration” branch) for initial review.

2. Merge Flow (Feature → Release → Production)

1. Feature → Feature Integration:
   - After the PR from 
"feature/*" → 
"feature" is approved, the branch is merged into 
"feature".
   - The 
"feature" branch acts as a staging area for all in - progress features.
2. ture → Release:Fea - Once the 
"feature" branch is stable (all planned features are merged and tested), a PR is opened from 
"feature" → 
"release/*" (e.g., 
"release/1.2.0").
   - Rejection Rule: PRs submitted directly to 
"release/*" (bypassing 
"feature") will be rejected immediately to enforce the two - stage flow.
3. Release → Production:
   - After the PR from 
"feature" → 
"release/*" is approved and tested, the 
"release/*" branch is merged into 
"production".
   - The 
"release/*" branch is then tagged (e.g., 
"v1.2.0") to mark the stable release version.

Visual Workflow (Mermaid Diagram)

graph TD
    A[Developer: Create feature/branch] --> B[PR: feature/* → feature]
    B --> C{Approved?}
    C -->|Yes| D[Merge to feature]
    D --> E[PR: feature → release/*]
    E --> F{Approved?}
    F -->|Yes| G[Merge to release/*]
    G --> H[Tag release: vX.Y.Z]
    H --> I[PR: release/* → production]
    I --> J{Approved?}
    J -->|Yes| K[Merge to production]
    C -->|No| L[Revise & Resubmit]
    F -->|No| L
    J -->|No| L
    M[Direct PR to release/*] --> N[REJECTED]

Stale Branch Detection & Reporting

We use a Jenkins Pipeline to detect and report stale branches:

1. Detection Logic

- Definition of “Stale”: A branch is stale if it has had no commits for [X] days (configurable via Jenkins parameter, e.g., 30/60/90 days).
- Report Columns:
   - Branch Name
   - Last Commit Date
   - Last Commit Author (Owner)

2. Jenkins Pipeline Workflow

- Schedule: Runs every 2–3 days (via 
"cron" trigger).
- Output: Generates a CSV report (
"stale_branches_<DATE>.csv") with all stale branches.
- Notification: Sends an email alert to the team with the report, highlighting branches that need cleanup.

3. Cleanup Policy

- Stale branches will be reviewed by the team.
- If a stale branch has no active PRs or ongoing work, it will be archived or deleted after approval.

Jenkins Pipeline Details

The pipeline uses the 
"gh" CLI (GitHub CLI) to fetch branch data and 
"jq" to process it into a CSV.

Sample Pipeline Script (Groovy)

pipeline {
    agent any
    parameters {
        choice(
            name: 'STALE_DAYS',
            choices: ['30', '60', '90'],
            description: 'Days without commits to consider a branch stale'
        )
    }
    triggers {
        cron('H 2 */2 * *') // Run every 2 days at ~2 AM
    }
    environment {
        GITHUB_TOKEN = credentials('github-token')
        GITHUB_OWNER = 'your-org'
        GITHUB_REPO = 'your-repo'
    }
    stages {
        stage('Checkout & Authenticate') {
            steps {
                sh '''
                    gh auth status || gh auth login --with-token <<< "$GITHUB_TOKEN"
                '''
            }
        }
        stage('Detect Stale Branches') {
            steps {
                sh '''
                    # Fetch branch data with last commit info
                    gh api graphql -f query='
                    query($owner: String!, $name: String!) {
                      repository(owner: $owner, name: $name) {
                        r

Of course. Here is a detailed comparison of RabbitMQ's Mirrored Queues and Quorum Queues, focusing on their mechanisms, trade-offs, and use cases.

Core Comparison: Mirrored Queues vs. Quorum Queues

Aspect Mirrored Queues (Classic) Quorum Queues (Modern)
Replication Mechanism Leader/Follower (Master/Slave) with asynchronous replication via the GM (Guaranteed Multicast) protocol. Raft consensus algorithm, with synchronous replication requiring a quorum of nodes to acknowledge each write.
Data Consistency Eventual Consistency (Weak). Writes are confirmed as soon as the leader accepts them. Followers may lag, leading to potential data loss if the leader fails before replication completes. Strong Consistency. A write is only confirmed after a majority (N/2+1) of replicas have persisted it, guaranteeing no acknowledged data is lost.
Failure Handling Automatic failover via RabbitMQ's internal promotion mechanism. This process can be complex during network partitions and may lead to split-brain scenarios, requiring manual intervention. Automatic leader election via the Raft protocol. It is designed to handle failures and network partitions gracefully without data loss or split-brain conditions.
Performance Higher throughput, lower latency. Producers are not blocked waiting for replicas, making it ideal for high-volume workloads where speed is critical. Lower throughput, higher latency. The quorum acknowledgment adds overhead. Performance scales with the number of replicas and network latency.
Primary Use Case High-throughput, latency-sensitive applications where the loss of a few in-flight messages is acceptable (e.g., log aggregation, real-time telemetry, non-critical tasks). Mission-critical data where absolute data safety is paramount and performance is a secondary concern (e.g., financial transactions, order processing, audit logs).
Recommended For Legacy systems or scenarios where ultimate performance is the top priority. Default choice for new deployments on RabbitMQ 3.8+, as it provides superior data safety and simpler operational semantics.

Detailed Technical Breakdown

Mirrored Queues

* How it works: One queue has a single leader (master) on one node and one or more mirrors (slaves) on other nodes. The leader handles all operations and asynchronously replicates state changes to the mirrors.
* Key Benefit: Performance. The leader can acknowledge operations immediately, offering excellent speed.
* Key Drawback: Data Safety & Complexity. The weak consistency model and the potential for split-brain during network partitions are its main weaknesses. Configuration and monitoring of mirroring policies (
"ha-mode", 
"ha-sync-mode") can be complex.

Quorum Queues

* How it works: The queue is a Raft replication group distributed across multiple nodes. All operations go through the elected Raft leader. A write is successful only after a quorum of nodes in the group have durably stored it.
* Key Benefit: Data Safety. The Raft protocol guarantees strong consistency, linearizability, and automatic, safe leader election, eliminating split-brain and ensuring no acknowledged writes are lost.
* Key Drawback: Performance & Resource Overhead. The synchronous replication adds inherent latency. Each queue is a full Raft state machine, consuming more memory and file descriptors than a mirrored queue.

How to Choose?

* Use Quorum Queues if: You are building a new system on RabbitMQ 3.8+ and your primary requirement is data integrity and safety. This should be your default choice for critical data flows. The simplified failure handling is a major operational advantage.
* Use Mirrored Queues if: You are optimizing for maximum throughput and minimum latency for non-critical data, or you are maintaining a legacy system on an older RabbitMQ version. Be prepared to handle the complexity of mirroring policies and potential network partition recovery.

Industry Trend: The RabbitMQ project explicitly recommends Quorum Queues as the primary HA queue type for modern deployments due to their stronger guarantees and simpler failure recovery, accepting the performance trade-off as a worthy cost for data safety.