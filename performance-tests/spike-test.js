import http from 'k6/http';
import { check } from 'k6';

// Spike test: Sudden traffic surge
export const options = {
  stages: [
    { duration: '1m', target: 50 },    // Baseline
    { duration: '30s', target: 500 },  // Sudden spike
    { duration: '2m', target: 500 },   // Sustained spike
    { duration: '30s', target: 50 },   // Drop back
    { duration: '1m', target: 50 },    // Recovery
  ],
  thresholds: {
    http_req_duration: ['p(95)<1500'],
    http_req_failed: ['rate<0.1'], // Allow 10% errors during spike
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const res = http.get(`${BASE_URL}/actuator/health`);

  check(res, {
    'status is 200': (r) => r.status === 200,
  });
}
