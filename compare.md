Cassandra Backup with DSBulk – Confidence Page

1. Overview

This document describes the process of backing up a production Cassandra cluster using DataStax Bulk Loader (DSBulk).

The production Cassandra cluster is configured with 3 racks, each containing 3 nodes (pods). DSBulk connects to a single, healthy pod to perform a full, consistent backup of designated tables during a scheduled maintenance window, with minimal operational impact.

2. Prerequisites

- Access & Credentials: Network access and valid authentication credentials (username/password) for a Cassandra pod.
- DSBulk Installation: DSBulk is installed and available on the backup server or execution host.
- Storage: Sufficient storage space in the target backup directory (
"/path/to/backup/").
- Environment: The chosen Cassandra pod is healthy and part of the production cluster.

3. Backup Strategy & Scheduling

- Connection Point: DSBulk will connect to one healthy pod in the cluster. In Cassandra, connecting to one node is sufficient to access all data distributed across the cluster.
- Schedule: Execute during a predefined maintenance window or off-peak hours to minimize performance impact.
- Scope: Backup all keyspaces, or a specific list of keyspaces and tables, as required.

4. Backup Procedure

4.1. Identify a Healthy Pod

Select a pod from the cluster that is in 
"Up" and 
"Normal" (UN) state. Verify using 
"nodetool status" or your cluster management tool.

4.2. Execute the DSBulk Unload (Backup) Command

Run a command in the following format. Adjust the 
"-h" (host), credentials, keyspace, table, and backup path as needed.

# Example: Backup a specific table
dsbulk unload \
  -h <CASSANDRA_POD_IP> \
  -u <USERNAME> \
  -p <PASSWORD> \
  -k <KEYSPACE> \
  -t <TABLE_NAME> \
  --connector.csv.url /path/to/backup/keyspace_table \
  --maxConcurrentFiles 1

# Example: Backup an entire keyspace
dsbulk unload \
  -h 10.1.2.3 \
  -u cassandra_user \
  -p 'secure_password' \
  -k my_keyspace \
  --connector.csv.url /backup/data/my_keyspace \
  --maxConcurrentFiles 1

Key Parameters & Notes:

- 
"-h": The IP/hostname of the selected healthy pod.
- 
"-k": The keyspace to back up.
- 
"-t": (Optional) A specific table to back up. Omit to back up all tables in the keyspace.
- 
"--connector.csv.url": The directory where DSBulk will write CSV backup files. Subdirectories for each table are created automatically.
- 
"--maxConcurrentFiles 1": A recommended setting for backup to reduce load. Increase with caution.
- Consistency: Consider using 
"-cl ONE" (or 
"LOCAL_QUORUM") for performance if eventual consistency is acceptable for the backup. For full consistency, use 
"-cl ALL" (with potential performance impact).
- Best Practice: Run a test backup on a non-production cluster first to validate the process and storage requirements.

4.3. Verify Backup Success

1. Check DSBulk Logs: Review the command-line output for a success message and summary of rows unloaded.
2. Validate Files: Ensure backup files (
".csv" and 
".csv.metadata") are created in the target directory.
ls -lh /path/to/backup/
3. Optional Data Check: Perform a quick row count comparison between a source table and the backup file (e.g., using 
"wc -l" on the CSV and a simple 
"COUNT" query in 
"cqlsh").

5. Restore Procedure (for Confidence)

A backup is only as good as your ability to restore. Test this procedure periodically in a non-production environment.

5.1. Restore Using DSBulk Load

To restore data to a table (e.g., after a truncation or into a test cluster), use the 
"dsbulk load" command.

dsbulk load \
  -h <TARGET_CLUSTER_POD_IP> \
  -u <USERNAME> \
  -p <PASSWORD> \
  -k <KEYSPACE> \
  -t <TABLE_NAME> \
  --connector.csv.url /path/to/backup/keyspace_table \
  --maxConcurrentFiles 1

Note: Ensure the target table schema exists before running the load operation.

6. Scheduling & Automation

- Cron Job (Example): Schedule the backup command (from 4.2) using a cron job on the backup server.
# Example: Run every Sunday at 2 AM
0 2 * * 0 /usr/bin/dsbulk unload [options] > /var/log/cassandra_backup.log 2>&1
- Logging & Monitoring: Redirect logs to a file and implement monitoring to check for job success/failure.

7. Troubleshooting Common Issues

- Connection Errors: Verify network connectivity, pod status, and credentials.
- Permission Errors: Ensure the backup directory is writable and Cassandra user has the necessary permissions (
"SELECT" on tables, 
"MODIFY" on keyspace if using certain options).
- Insufficient Space: Monitor disk space in the backup directory.
- Performance Impact: If the backup causes high load, consider further throttling with 
"--maxConcurrentFiles 1" and 
"--executor.maxPerSecond" in DSBulk.

This document provides a standard framework. Always adapt commands and schedules to your specific environment, data size, and recovery objectives.