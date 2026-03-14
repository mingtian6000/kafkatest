#
# Apache Airflow 2.10.5 + Python 3.9 精简 Constraints (增强版)
# 
# 包含组件:
#   - Google Cloud Provider (完整 GCP 支持)
#   - Kubernetes (CNCF Provider)
#   - Celery 5.5.0 + Kombu 5.5.0 (RabbitMQ 消息队列)
#   - Flower (Celery 监控)
#   - LDAP 认证 (python-ldap + ldap3)
#   - Cloud SQL (MySQL/PostgreSQL)
#
# 重要说明:
#   1. GCS Fuse (Google Cloud Storage FUSE) 是系统级工具，不是 Python 包
#      安装方法: apt-get install gcsfuse (Debian/Ubuntu)
#      Python 中通过 gcsfs (已包含) 访问挂载的 GCS  bucket
#
#   2. LDAP 认证需要系统依赖:
#      - apt-get install libldap2-dev libsasl2-dev (python-ldap 需要)
#
#   3. RabbitMQ 连接使用 py-amqp (已包含)，如需 librabbitmq (C 优化版):
#      - apt-get install librabbitmq-dev
#      - pip install librabbitmq
#
# 从原始 731 个包精简到 229 个包
# 生成时间: 2025-03-14 (基于官方 constraints-2.10.5 修改)
#
# 安装命令:
#   pip install "apache-airflow[celery,cncf.kubernetes,google]==2.10.5" \
#       --constraint constraints-airflow-2.10.5-gcp-k8s-celery-py39.txt
#

alembic==1.14.1
amqp==5.3.1
anyio==4.8.0
apache-airflow-providers-celery==3.10.0
apache-airflow-providers-cncf-kubernetes==10.1.0
apache-airflow-providers-common-compat==1.3.0
apache-airflow-providers-common-io==1.5.0
apache-airflow-providers-common-sql==1.21.0
apache-airflow-providers-fab==1.5.2
apache-airflow-providers-ftp==3.12.0
apache-airflow-providers-google==12.0.0
apache-airflow-providers-http==5.0.0
apache-airflow-providers-imap==3.8.0
apache-airflow-providers-smtp==1.9.0
apache-airflow-providers-sqlite==4.0.0
apispec==6.8.1
asgiref==3.8.1
async-timeout==5.0.1
asyncpg==0.30.0
attrs==25.1.0
Authlib==1.3.1
bcrypt==4.2.1
billiard==4.2.1
cattrs==24.1.2
celery==5.5.0
certifi==2025.1.31
cffi==1.17.1
charset-normalizer==3.4.1
click-didyoumean==0.3.1
click-plugins==1.1.1
click-repl==0.3.0
click==8.1.8
clickclick==20.10.2
colorama==0.4.6
colorlog==6.9.0
ConfigUpdater==3.2
connexion==2.14.2
cron-descriptor==1.4.5
croniter==6.0.0
cryptography==42.0.8
db-dtypes==1.4.0
Deprecated==1.2.18
dill==0.3.1.1
fastjsonschema==2.21.1
Flask-AppBuilder==4.5.2
Flask-Babel==2.0.0
Flask-Bcrypt==1.0.1
Flask-Caching==2.3.0
Flask-JWT-Extended==4.7.1
Flask-Limiter==3.10.1
Flask-Login==0.6.3
Flask-Session==0.5.0
Flask-SQLAlchemy==2.5.1
Flask-WTF==1.2.2
Flask==2.2.5
flower==2.0.1
fsspec==2025.2.0
gcloud-aio-auth==5.3.2
gcloud-aio-bigquery==7.1.0
gcloud-aio-storage==9.3.0
gcsfs==2025.2.0
google-api-core==2.24.1
google-api-python-client==2.160.0
google-auth-httplib2==0.2.0
google-auth-oauthlib==1.2.1
google-auth==2.38.0
google-cloud-aiplatform==1.79.0
google-cloud-alloydb==0.4.1
google-cloud-appengine-logging==1.5.0
google-cloud-audit-log==0.3.0
google-cloud-automl==2.15.0
google-cloud-batch==0.17.33
google-cloud-bigquery-datatransfer==3.18.0
google-cloud-bigquery==3.20.1
google-cloud-bigtable==2.28.1
google-cloud-build==3.29.0
google-cloud-compute==1.24.0
google-cloud-container==2.55.1
google-cloud-core==2.4.1
google-cloud-datacatalog==3.24.1
google-cloud-dataflow-client==0.8.15
google-cloud-dataform==0.5.14
google-cloud-dataplex==2.6.0
google-cloud-dataproc-metastore==1.17.0
google-cloud-dataproc==5.16.0
google-cloud-dlp==3.26.0
google-cloud-kms==3.2.2
google-cloud-language==2.16.0
google-cloud-logging==3.11.4
google-cloud-memcache==1.11.0
google-cloud-monitoring==2.26.0
google-cloud-orchestration-airflow==1.16.1
google-cloud-os-login==2.16.0
google-cloud-pubsub==2.28.0
google-cloud-redis==2.17.0
google-cloud-resource-manager==1.14.0
google-cloud-run==0.10.14
google-cloud-secret-manager==2.22.1
google-cloud-speech==2.30.0
google-cloud-storage-transfer==1.15.0
google-cloud-storage==2.19.0
google-cloud-tasks==2.18.0
google-cloud-texttospeech==2.24.0
google-cloud-translate==3.19.0
google-cloud-videointelligence==2.15.0
google-cloud-vision==3.9.0
google-cloud-workflows==1.16.0
google-crc32c==1.6.0
google-re2==1.1.20240702
google-resumable-media==2.7.2
googleapis-common-protos==1.66.0
greenlet==3.1.1
grpc-google-iam-v1==0.14.0
grpcio-gcp==0.2.2
grpcio-health-checking==1.62.3
grpcio-status==1.62.3
grpcio-tools==1.62.3
grpcio==1.65.5
gunicorn==23.0.0
h11==0.14.0
h2==4.2.0
hpack==4.1.0
httpcore==1.0.7
httpx==0.27.0
hyperframe==6.1.0
idna==3.10
importlib-metadata==6.11.0
Jinja2==3.1.5
jsonschema-path==0.3.4
jsonschema-specifications==2024.10.1
jsonschema==4.23.0
kombu==5.5.0
kubernetes==30.1.0
kubernetes_asyncio==30.1.0
lazy-object-proxy==1.10.0
ldap3==2.9.1
lockfile==0.12.2
Mako==1.3.8
markdown-it-py==3.0.0
Markdown==3.7
MarkupSafe==3.0.2
marshmallow-oneofschema==3.1.1
marshmallow-sqlalchemy==0.28.2
marshmallow==3.26.0
methodtools==0.4.7
numpy==1.26.4
oauthlib==3.2.2
opentelemetry-api==1.27.0
opentelemetry-exporter-otlp-proto-common==1.27.0
opentelemetry-exporter-otlp-proto-grpc==1.27.0
opentelemetry-exporter-otlp-proto-http==1.27.0
opentelemetry-exporter-otlp==1.27.0
opentelemetry-sdk==1.27.0
packaging==24.2
pandas-gbq==0.26.1
pandas-stubs==2.2.2.240807
pandas==2.1.4
pathspec==0.12.1
pendulum==3.0.0
proto-plus==1.26.0
protobuf==4.25.6
psutil==6.1.1
psycopg2-binary==2.9.10
py-amqp==5.3.1
pyarrow-hotfix==0.6
pyarrow==16.1.0
pycparser==2