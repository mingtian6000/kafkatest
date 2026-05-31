```
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Gauge, Rate, Trend } from 'k6/metrics';

// 自定义指标
const myCounter = new Counter('my_counter');
const myGauge = new Gauge('my_gauge');
const myRate = new Rate('my_rate');
const myTrend = new Trend('my_trend');

export const options = {
  vus: 10,
  duration: '30s',
  
  // 输出到 Prometheus 远程写入
  ext: {
    loadimpact: {
      // Prometheus 远程写入配置
      prometheus: {
        remoteWriteURL: 'http://YOUR_PROMETHEUS_ENDPOINT:9090/api/v1/write',
        // 如果 Prometheus 需要认证
        headers: {
          'Authorization': 'Bearer YOUR_TOKEN',
        },
        // 标签配置
        staticLabels: {
          test_name: 'k6_performance_test',
          environment: 'production',
        }
      }
    }
  },
  
  // 或者使用 k6 的 Prometheus 远程写入输出
  // 需要 k6 版本 0.41.0+
  output: 'prometheus-remote-write',
  prometheusRemoteWrite: {
    url: 'http://YOUR_PROMETHEUS_ENDPOINT:9090/api/v1/write',
    // 认证配置
    headers: {
      'Authorization': 'Bearer YOUR_TOKEN',
    },
  }
};

export default function () {
  const res = http.get('https://httpbin.test.k6.io/get');
  
  // 更新指标
  myCounter.add(1);
  myGauge.add(Math.random() * 100);
  myRate.add(res.status === 200);
  myTrend.add(res.timings.duration);
  
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
  
  sleep(1);
}
