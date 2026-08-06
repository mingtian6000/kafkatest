```
python3.12 -m airflow connections add git_github \
  --conn-type git \
  --conn-host https://github.com/your-org/your-dags-repo.git \
  --conn-password $GITHUB_PAT
