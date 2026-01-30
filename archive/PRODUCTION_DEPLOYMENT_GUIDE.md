# Production Deployment Guide
## Fortune 100 Enterprise Standards

**Last Updated:** January 29, 2026

---

## ⚠️ CRITICAL PRE-DEPLOYMENT CHECKLIST

### 1. 🔒 Security Configuration

#### Change All Default Passwords
```bash
# Generate secure JWT secret (64 bytes)
openssl rand -base64 64

# Set environment variable
export JWT_SECRET="<generated-secret-here>"

# Change admin password
export ADMIN_PASSWORD="<strong-password-min-16-chars>"
```

#### Configure Database
```bash
# Production database credentials
export DATABASE_URL="jdbc:postgresql://prod-db-host:5432/prod_database"
export DATABASE_USERNAME="prod_user"
export DATABASE_PASSWORD="<strong-password>"
```

#### Configure SMTP (for emails)
```bash
# Gmail example
export SMTP_USERNAME="notifications@yourcompany.com"
export SMTP_PASSWORD="<gmail-app-password>"

# Or use SendGrid, AWS SES, etc.
```

#### Optional: Threat Intelligence
```bash
# Get API key from https://www.abuseipdb.com/account/api
export ABUSEIPDB_API_KEY="<your-api-key>"
```

---

### 2. 🔐 Enable HTTPS/TLS

**REQUIRED for production!**

#### Option A: Nginx Reverse Proxy
```nginx
server {
    listen 443 ssl http2;
    server_name api.yourcompany.com;

    ssl_certificate /path/to/fullchain.pem;
    ssl_certificate_key /path/to/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

#### Option B: Spring Boot SSL
```properties
# application-prod.properties
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
```

---

### 3. 📊 Database Setup

#### Production Database Configuration

```bash
# Install PostgreSQL 15+ on production server
sudo apt-get install postgresql-15

# Create production database
sudo -u postgres psql
CREATE DATABASE prod_crud_db;
CREATE USER prod_user WITH ENCRYPTED PASSWORD 'strong-password';
GRANT ALL PRIVILEGES ON DATABASE prod_crud_db TO prod_user;
\q

# Update spring.datasource.url to point to production DB
```

#### Enable Database Encryption at Rest
```bash
# PostgreSQL encryption (example for AWS RDS)
# Enable encryption when creating RDS instance
# Use AWS KMS for key management
```

#### Configure Automated Backups
```bash
# Daily backups at 2 AM
crontab -e
0 2 * * * pg_dump -U prod_user prod_crud_db > /backups/db_$(date +\%Y\%m\%d).sql
```

---

### 4. 🐳 Docker Deployment (Recommended)

#### Build Docker Image
```dockerfile
# Dockerfile (already provided)
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/CRUD_test-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# Build and tag
docker build -t crud-app:v2.0 .

# Run with environment variables
docker run -d \
  -p 8080:8080 \
  -e JWT_SECRET="$(openssl rand -base64 64)" \
  -e DATABASE_URL="jdbc:postgresql://db-host:5432/prod_db" \
  -e DATABASE_USERNAME="prod_user" \
  -e DATABASE_PASSWORD="strong-password" \
  -e SMTP_USERNAME="notifications@company.com" \
  -e SMTP_PASSWORD="smtp-password" \
  --name crud-backend \
  crud-app:v2.0
```

---

### 5. ☸️ Kubernetes Deployment (For Scale)

#### Create Kubernetes Secrets
```bash
kubectl create secret generic app-secrets \
  --from-literal=jwt-secret=$(openssl rand -base64 64) \
  --from-literal=database-password=strong-password \
  --from-literal=smtp-password=smtp-password
```

#### Deployment Manifest
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: crud-backend
spec:
  replicas: 3  # Auto-scale
  selector:
    matchLabels:
      app: crud-backend
  template:
    metadata:
      labels:
        app: crud-backend
    spec:
      containers:
      - name: backend
        image: crud-app:v2.0
        ports:
        - containerPort: 8080
        env:
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: jwt-secret
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: database-password
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
```

---

### 6. 📈 Monitoring Setup

#### Prometheus + Grafana

```bash
# Add Micrometer Prometheus dependency (already in pom.xml)
# Expose metrics endpoint
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.metrics.export.prometheus.enabled=true

# Prometheus scrape config
scrape_configs:
  - job_name: 'crud-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

#### Application Performance Monitoring (APM)

```bash
# New Relic
java -javaagent:/path/to/newrelic.jar -jar app.jar

# Datadog
java -javaagent:/path/to/dd-java-agent.jar -jar app.jar

# AWS CloudWatch
# Use AWS CloudWatch agent
```

---

### 7. 🔥 Firewall & Security

#### Configure Firewall Rules
```bash
# Allow only necessary ports
sudo ufw allow 443/tcp  # HTTPS
sudo ufw allow 22/tcp   # SSH (from specific IPs only)
sudo ufw deny 8080/tcp  # Block direct backend access
sudo ufw enable
```

#### IP Whitelisting (Optional)
```bash
# Restrict admin dashboard to company IPs
# Add to rate_limit_whitelist table or configure in application
```

---

### 8. 🧪 Pre-Production Testing

#### Load Testing with JMeter
```bash
# Install JMeter
sudo apt-get install jmeter

# Run load test (1000 concurrent users)
jmeter -n -t load-test.jmx -l results.jtl

# Analyze results
# Target: < 500ms response time, 0% error rate
```

#### Security Testing
```bash
# OWASP ZAP scan
docker run -t owasp/zap2docker-stable zap-baseline.py \
  -t https://your-api.com

# Nmap scan
nmap -sV -sC your-api.com

# SQL injection test (manual)
# XSS test (manual)
```

#### Penetration Testing
```bash
# Hire third-party security firm
# Or use Metasploit, Burp Suite
```

---

### 9. 📋 Deployment Steps

#### Step-by-Step Production Deployment

```bash
# 1. Build application
mvn clean package -DskipTests

# 2. Transfer JAR to production server
scp target/CRUD_test-1.0-SNAPSHOT.jar user@prod-server:/app/

# 3. SSH into production server
ssh user@prod-server

# 4. Set environment variables
export JWT_SECRET="<secure-secret>"
export DATABASE_URL="jdbc:postgresql://db:5432/prod_db"
export DATABASE_USERNAME="prod_user"
export DATABASE_PASSWORD="<db-password>"
export SMTP_USERNAME="<email>"
export SMTP_PASSWORD="<smtp-password>"

# 5. Run application with nohup
nohup java -jar /app/CRUD_test-1.0-SNAPSHOT.jar > app.log 2>&1 &

# 6. Verify health
curl https://your-api.com/actuator/health

# 7. Monitor logs
tail -f app.log
```

---

### 10. 🔄 CI/CD Pipeline (Recommended)

#### GitHub Actions Example
```yaml
name: Deploy to Production

on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'

    - name: Build with Maven
      run: mvn clean package -DskipTests

    - name: Run tests
      run: mvn test

    - name: Build Docker image
      run: docker build -t crud-app:${{ github.sha }} .

    - name: Push to registry
      run: docker push crud-app:${{ github.sha }}

    - name: Deploy to Kubernetes
      run: kubectl set image deployment/crud-backend backend=crud-app:${{ github.sha }}
```

---

### 11. 🛡️ Post-Deployment Security

#### Security Hardening Checklist

- [x] Change all default passwords
- [x] Enable HTTPS/TLS
- [x] Configure firewall
- [x] Enable rate limiting
- [x] Configure CORS properly
- [x] Disable unnecessary endpoints
- [x] Set up monitoring and alerts
- [x] Configure automated backups
- [x] Enable audit logging
- [x] Implement log rotation
- [x] Set up incident response plan
- [x] Configure DDoS protection (Cloudflare, AWS Shield)

---

### 12. 📊 Monitoring & Alerts

#### Set Up Alerts

```yaml
# Prometheus Alert Rules
groups:
- name: backend_alerts
  rules:
  - alert: HighErrorRate
    expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
    annotations:
      summary: "High error rate detected"

  - alert: HighMemoryUsage
    expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.9
    annotations:
      summary: "High memory usage"

  - alert: DatabaseConnectionPoolExhausted
    expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
    annotations:
      summary: "Database connection pool near limit"
```

---

### 13. 🔐 Compliance & Audit

#### Enable Compliance Logging

```properties
# Log all audit events to separate file
logging.file.name=audit.log
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# Rotate logs daily
logging.logback.rollingpolicy.max-file-size=100MB
logging.logback.rollingpolicy.max-history=365
```

#### GDPR Compliance
- User data export endpoint
- User data deletion endpoint
- Cookie consent management
- Privacy policy compliance

---

### 14. 🚀 Go-Live Checklist

#### Final Verification Before Launch

- [ ] All environment variables set
- [ ] HTTPS enabled and working
- [ ] Database backups configured
- [ ] Monitoring and alerts set up
- [ ] Load testing completed (1000+ users)
- [ ] Security testing completed
- [ ] Penetration testing passed
- [ ] Admin passwords changed
- [ ] JWT secret rotated
- [ ] SMTP configured and tested
- [ ] Rate limiting tested
- [ ] MFA tested end-to-end
- [ ] Threat intelligence verified
- [ ] Email notifications working
- [ ] Audit logging enabled
- [ ] Log rotation configured
- [ ] Health checks passing
- [ ] Disaster recovery plan documented
- [ ] Incident response plan ready
- [ ] Support team trained

---

### 15. 📞 Support & Maintenance

#### Ongoing Maintenance Tasks

**Daily:**
- Monitor error logs
- Check health endpoints
- Review security alerts

**Weekly:**
- Review audit logs
- Check backup integrity
- Update dependencies

**Monthly:**
- Security patches
- Performance review
- Capacity planning

**Quarterly:**
- Penetration testing
- Disaster recovery drill
- Security audit

---

## 🎯 PRODUCTION DEPLOYMENT SUMMARY

### Minimum Requirements

1. **JWT_SECRET** - Generate with `openssl rand -base64 64`
2. **ADMIN_PASSWORD** - Strong password (16+ chars)
3. **DATABASE_PASSWORD** - Strong password
4. **HTTPS/TLS** - SSL certificate configured
5. **Backups** - Automated daily backups
6. **Monitoring** - Prometheus + Grafana or equivalent

### Recommended Additions

7. **SMTP Configuration** - For email notifications
8. **AbuseIPDB API Key** - For enhanced threat intelligence
9. **Load Balancer** - For high availability
10. **Redis** - For distributed rate limiting
11. **CDN** - For static assets (Cloudflare)
12. **WAF** - Web Application Firewall

---

## 🏆 PRODUCTION-READY CERTIFICATION

**Your application is certified PRODUCTION-READY when:**

✅ All security configurations are set
✅ HTTPS is enabled
✅ Monitoring is active
✅ Backups are automated
✅ Load testing passed (1000+ users)
✅ Security testing passed
✅ All default passwords changed
✅ Health checks passing

---

**Deployment Date:** _____________
**Deployed By:** _____________
**Production URL:** _____________
**Monitoring Dashboard:** _____________

---

**Good luck with your deployment!** 🚀

For support, refer to the comprehensive documentation in the project repository.
