# Performance Testing Guide

## Overview

Performance tests using K6 to validate application scalability and performance under load.

## Prerequisites

Install K6:
```bash
# macOS
brew install k6

# Linux
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6

# Windows
choco install k6
```

## Test Types

### 1. Load Test (load-test.js)
**Purpose:** Validate performance under expected load

**Stages:**
- Ramp up: 0 → 50 → 100 users (7 min)
- Sustained: 100 users (10 min)
- Peak: 200 users (15 min)
- Ramp down: 200 → 0 (2 min)

**Run:**
```bash
k6 run --vus 100 --duration 30m performance-tests/load-test.js
```

**Thresholds:**
- p95 response time < 500ms
- p99 response time < 1000ms
- Error rate < 1%
- Login success rate > 99%

### 2. Stress Test (stress-test.js)
**Purpose:** Find breaking point

**Stages:**
- Gradual increase: 100 → 1000 users
- Find maximum capacity
- Monitor degradation

**Run:**
```bash
k6 run performance-tests/stress-test.js
```

**Expected Results:**
- Breaking point: ~600-800 concurrent users
- Graceful degradation (no crashes)
- Circuit breakers activate

### 3. Spike Test (spike-test.js)
**Purpose:** Test sudden traffic surge

**Stages:**
- Baseline: 50 users
- Sudden spike: 50 → 500 users (30s)
- Sustained: 500 users (2 min)
- Recovery: 500 → 50 users

**Run:**
```bash
k6 run performance-tests/spike-test.js
```

**Expected Results:**
- System handles spike without crash
- Auto-scaling triggers (if Kubernetes)
- Recovery to normal operation

## Running Tests

### Local Testing
```bash
# Start application
./startup.sh

# Run load test
k6 run performance-tests/load-test.js

# Run with custom base URL
BASE_URL=http://localhost:8080 k6 run performance-tests/load-test.js
```

### CI/CD Integration
```bash
# Run in GitHub Actions
k6 run --out json=results.json performance-tests/load-test.js

# Upload results
k6 cloud upload results.json
```

### Docker Testing
```bash
# Run K6 in Docker
docker run --network=host -v $(pwd):/tests \
  grafana/k6 run /tests/performance-tests/load-test.js
```

## Analyzing Results

### K6 Output
```
scenarios: (100.00%) 1 scenario, 200 max VUs
✓ login status is 200
✓ login returns token
✓ health status is UP

checks.........................: 99.23% ✓ 15843    ✗ 123
data_received..................: 45 MB  25 kB/s
data_sent......................: 12 MB  6.5 kB/s
http_req_blocked...............: avg=1.5ms   p(95)=5ms
http_req_connecting............: avg=800µs   p(95)=3ms
http_req_duration..............: avg=245ms   p(95)=456ms p(99)=789ms
http_req_failed................: 0.77%  ✓ 123      ✗ 15843
http_req_receiving.............: avg=500µs   p(95)=2ms
http_req_sending...............: avg=300µs   p(95)=1ms
http_req_tls_handshaking.......: avg=0s      p(95)=0s
http_req_waiting...............: avg=244ms   p(95)=454ms
http_reqs......................: 15966  88.7/s
iterations.....................: 15966  88.7/s
login_duration.................: avg=280ms   p(95)=520ms
login_success..................: 99.22% ✓ 15843    ✗ 123
vus............................: 200    min=0      max=200
vus_max........................: 200    min=200    max=200
```

### Key Metrics

**Response Time:**
- p50 (median): 245ms ✅
- p95: 456ms ✅ (< 500ms threshold)
- p99: 789ms ✅ (< 1000ms threshold)

**Throughput:**
- 88.7 requests/second
- 15,966 total requests

**Error Rate:**
- 0.77% ✅ (< 1% threshold)

## Performance Targets

### Minimum Requirements
- **Response Time:** p95 < 500ms
- **Throughput:** > 50 req/s
- **Error Rate:** < 1%
- **Availability:** 99.9% uptime

### Target (Current)
- **Response Time:** p95 < 300ms
- **Throughput:** > 100 req/s
- **Error Rate:** < 0.5%
- **Availability:** 99.95% uptime

### Stretch Goals
- **Response Time:** p95 < 200ms
- **Throughput:** > 500 req/s
- **Error Rate:** < 0.1%
- **Availability:** 99.99% uptime

## Monitoring During Tests

### Prometheus Metrics
```bash
# Check during test
curl http://localhost:8080/actuator/prometheus | grep -E "(http_server_requests|hikaricp|jvm_memory)"
```

### Application Logs
```bash
# Monitor logs
tail -f logs/spring.log | grep -i "error\|warn"
```

### Database Connections
```bash
# Check connection pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

## Optimization Tips

If tests fail:

1. **High Response Time:**
   - Enable Redis caching
   - Optimize database queries
   - Increase connection pool size

2. **High Error Rate:**
   - Check circuit breaker status
   - Verify database connectivity
   - Review rate limiting settings

3. **Low Throughput:**
   - Increase JVM heap size
   - Scale horizontally (more pods)
   - Optimize database indexes

## CI/CD Integration

Add to `.github/workflows/ci-cd.yml`:
```yaml
performance-test:
  runs-on: ubuntu-latest
  steps:
    - name: Run performance tests
      run: |
        docker-compose up -d
        sleep 30
        k6 run --quiet --no-color \
          --summary-export=summary.json \
          performance-tests/load-test.js

    - name: Upload results
      uses: actions/upload-artifact@v3
      with:
        name: k6-results
        path: summary.json
```

## Best Practices

1. **Always test locally first**
2. **Run during off-peak hours in production**
3. **Monitor system resources during tests**
4. **Set realistic thresholds**
5. **Run tests regularly (weekly)**
6. **Track performance trends over time**
7. **Test after major changes**

## Troubleshooting

**Test fails immediately:**
- Check application is running
- Verify BASE_URL is correct
- Check network connectivity

**High error rate:**
- Check application logs
- Verify database is running
- Check rate limiting settings

**Low performance:**
- Increase resources (CPU, memory)
- Enable caching
- Review slow queries

## Report Template

```markdown
# Performance Test Report

**Date:** 2026-02-03
**Environment:** Production
**Test Duration:** 30 minutes
**Max Concurrent Users:** 200

## Results

| Metric | Target | Actual | Status |
|--------|--------|--------|--------|
| p95 Response Time | < 500ms | 456ms | ✅ PASS |
| p99 Response Time | < 1000ms | 789ms | ✅ PASS |
| Error Rate | < 1% | 0.77% | ✅ PASS |
| Throughput | > 50 req/s | 88.7 req/s | ✅ PASS |

## Observations

- System handled 200 concurrent users smoothly
- No errors during sustained load
- Circuit breakers did not trigger
- Auto-scaling worked as expected

## Recommendations

- Current capacity: 200 concurrent users
- Headroom: ~300 users before degradation
- Consider scaling at 150 users
```
