```
python3.12 -m airflow connections add git_github \
  --conn-type git \
  --conn-host https://github.com/your-org/your-dags-repo.git \
  --conn-password $GITHUB_PAT


export AIRFLOW__DAG_PROCESSOR__DAG_BUNDLE_CONFIG_LIST='[
  {
    "name": "github-dags",
    "classpath": "airflow.providers.git.bundles.git.GitDagBundle",
    "kwargs": {
      "git_conn_id": "git_github",
      "tracking_ref": "main",
      "subdir": "dags",
      "refresh_interval": 60
    }
  }
]'
