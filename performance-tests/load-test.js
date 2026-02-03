import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const loginSuccessRate = new Rate('login_success');
const loginDuration = new Trend('login_duration');
const apiErrors = new Counter('api_errors');

// Test configuration
export const options = {
  stages: [
    { duration: '2m', target: 50 },   // Ramp up to 50 users
    { duration: '5m', target: 100 },  // Ramp up to 100 users
    { duration: '10m', target: 100 }, // Stay at 100 users
    { duration: '5m', target: 200 },  // Ramp up to 200 users
    { duration: '10m', target: 200 }, // Stay at 200 users
    { duration: '2m', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'], // 95% under 500ms, 99% under 1s
    http_req_failed: ['rate<0.01'], // Error rate under 1%
    login_success: ['rate>0.99'], // Login success rate over 99%
    login_duration: ['p(95)<800'], // Login under 800ms for 95%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Test data
const users = [
  { username: 'testuser1', password: 'TestPass123!' },
  { username: 'testuser2', password: 'TestPass123!' },
  { username: 'testuser3', password: 'TestPass123!' },
];

export function setup() {
  // Register test users if needed
  console.log('Setting up test users...');
  return { baseUrl: BASE_URL };
}

export default function (data) {
  const user = users[Math.floor(Math.random() * users.length)];

  // Test 1: Login
  const loginStart = Date.now();
  const loginRes = http.post(`${data.baseUrl}/api/auth/login`, JSON.stringify({
    username: user.username,
    password: user.password,
  }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'Login' },
  });

  const loginSuccess = check(loginRes, {
    'login status is 200': (r) => r.status === 200,
    'login returns token': (r) => JSON.parse(r.body).token !== undefined,
  });

  loginSuccessRate.add(loginSuccess);
  loginDuration.add(Date.now() - loginStart);

  if (!loginSuccess) {
    apiErrors.add(1);
    return;
  }

  const token = JSON.parse(loginRes.body).token;

  sleep(1);

  // Test 2: Get Users (admin only)
  const usersRes = http.get(`${data.baseUrl}/api/users`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    tags: { name: 'GetUsers' },
  });

  check(usersRes, {
    'get users status is 200 or 403': (r) => r.status === 200 || r.status === 403,
  });

  sleep(1);

  // Test 3: Search Users with Pagination
  const searchRes = http.get(
    `${data.baseUrl}/api/users/search?page=0&size=20&sortBy=createdAt&sortDirection=DESC`,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      tags: { name: 'SearchUsers' },
    }
  );

  check(searchRes, {
    'search status is 200 or 403': (r) => r.status === 200 || r.status === 403,
    'search response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);

  // Test 4: Get User Statistics
  const statsRes = http.get(`${data.baseUrl}/api/users/statistics`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    tags: { name: 'GetStatistics' },
  });

  check(statsRes, {
    'stats status is 200 or 403': (r) => r.status === 200 || r.status === 403,
    'stats response time < 300ms': (r) => r.timings.duration < 300,
  });

  sleep(2);

  // Test 5: Health Check
  const healthRes = http.get(`${data.baseUrl}/actuator/health`, {
    tags: { name: 'HealthCheck' },
  });

  check(healthRes, {
    'health status is 200': (r) => r.status === 200,
    'health status is UP': (r) => JSON.parse(r.body).status === 'UP',
  });

  sleep(1);
}

export function teardown(data) {
  console.log('Test completed');
}
