```

curl -s -H "Authorization: Bearer $(gcloud auth print-access-token)" \
  "https://dataflow.googleapis.com/v1b3/projects/YOUR_PROJECT/locations/YOUR_REGION/settings" \
  | jq .
