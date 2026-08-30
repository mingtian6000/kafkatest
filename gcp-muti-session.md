```
You are an expert cloud infrastructure architect. I will paste a JSON output of my GCP resources (from `gcloud asset list`, `terraform show -json`, or similar). 

Please analyze the JSON and:

1. Extract all GCP components (VPC, Subnets, Firewall, GKE clusters, Node Pools, MIGs, VMs, Cloud SQL, DNS zones, Secret Manager, Load Balancers, Cloud Storage buckets, IAM Service Accounts, etc.).
2. Infer relationships between components (e.g., which VMs/MIGs belong to which VPC, which GKE pods connect to which Cloud SQL, which firewall rules apply to which targets).
3. Generate a Mermaid `graph TD` diagram:
   - Group resources into subgraphs (e.g., `subgraph VPC`, `subgraph GKE Cluster`, `subgraph Database Layer`).
   - Draw arrows to show traffic/connection flow (e.g., `GKE_Pod -->|5432| Cloud_SQL`).
   - Use different shapes for different resource types (e.g., `[Rectangle]` for compute, `((Circle))` for databases, `{Diamond}` for firewalls).
4. Identify any resources that appear to be ORPHANED or STANDALONE (no dependencies, no connections to other resources) and mark them with a red dashed border or a "⚠️ Standalone" note.
5. If the JSON is too large to process at once, tell me which sections to extract and re-paste.

Here is my GCP resource JSON:
[paste your JSON here]
