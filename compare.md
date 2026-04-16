```
apiVersion: v1
kind: ConfigMap
metadata:
  name: wait-app
  namespace: cassandra
data:
  Wait.java: |
    public class Wait {
        public static void main(String[] args) throws Exception {
            // 使用CountDownLatch无限等待
            new java.util.concurrent.CountDownLatch(1).await();
        }
    }
---
apiVersion: v1
kind: Pod
metadata:
  name: bridge-test
  namespace: cassandra
spec:
  containers:
  - name: bridge
    image: eclipse-temurin:21-jdk-alpine
    command: ["java"]
    args: ["/app/Wait.java"]
    volumeMounts:
    - name: app-volume
      mountPath: /app
  volumes:
  - name: app-volume
    configMap:
      name: wait-app
