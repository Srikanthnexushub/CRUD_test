import http from 'k6/http';
import { check, sleep } from 'k6';

// Stress test: Find breaking point
export const options = {
  stages: [
    { duration: '2m', target: 100 },   // Ramp up
    { duration: '5m', target: 200 },   // Moderate load
    { duration: '5m', target: 400 },   // Heavy load
    { duration: '5m', target: 600 },   // Very heavy load
    { duration: '5m', target: 800 },   // Extreme load
    { duration: '5m', target: 1000 },  // Breaking point
    { duration: '2m', target: 0 },     // Recovery
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // Allow higher latency for stress test
    http_req_failed: ['rate<0.05'], // Allow 5% error rate
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
    username: 'admin',
    password: 'admin123',
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  check(loginRes, {
    'login successful': (r) => r.status === 200,
  });

  if (loginRes.status === 200) {
    const token = JSON.parse(loginRes.body).token;

    http.get(`${BASE_URL}/api/users`, {
      headers: { 'Authorization': `Bearer ${token}` },
    });
  }

  sleep(0.5);
}
