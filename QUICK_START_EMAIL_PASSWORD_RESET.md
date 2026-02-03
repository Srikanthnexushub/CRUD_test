# Quick Start: Email & Password Reset

**Last Updated:** 2026-02-03
**Status:** Production Ready

## 📧 Email Configuration (5 Minutes)

### Step 1: Configure SMTP

**For Gmail:**
```bash
# Set environment variables
export SMTP_USERNAME=your-email@gmail.com
export SMTP_PASSWORD=xxxx-xxxx-xxxx-xxxx  # App password from Google

# Or update application.properties
spring.mail.username=your-email@gmail.com
spring.mail.password=xxxx-xxxx-xxxx-xxxx
```

**Get Gmail App Password:**
1. Go to https://myaccount.google.com/apppasswords
2. Select "Mail" and "Other (Custom name)"
3. Copy the 16-character password
4. Use this as SMTP_PASSWORD

### Step 2: Test Email

```bash
# Start application
./startup.sh

# Test email sending (replace with your email)
curl -X POST http://localhost:8080/api/v1/admin/test-email \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"recipient":"your-email@example.com"}'
```

### Step 3: Verify Email Templates

Check these templates work:
- Welcome email: Sent on user registration
- Password reset: Sent when user requests reset
- Account locked: Sent when account is locked
- MFA enabled: Sent when 2FA is activated

---

## 🔐 Password Reset Flow (3 Steps)

### User Perspective

**Step 1: Request Reset**
```bash
curl -X POST http://localhost:8080/api/v1/password-reset/initiate \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com"}'
```

**Step 2: Check Email**
- User receives email with reset link
- Link format: `http://localhost:8080/reset-password?token=abc123...`
- Valid for 30 minutes

**Step 3: Reset Password**
```bash
curl -X POST http://localhost:8080/api/v1/password-reset/reset \
  -H "Content-Type: application/json" \
  -d '{
    "token":"abc123...",
    "newPassword":"NewSecure123!",
    "confirmPassword":"NewSecure123!"
  }'
```

---

## 🛡️ Security Features Enabled

### 1. Security Headers (Automatic)
All responses include 12+ security headers:
- Content-Security-Policy
- X-Frame-Options: DENY
- Strict-Transport-Security
- X-Content-Type-Options: nosniff
- And more...

### 2. Input Sanitization (Automatic)
Blocks malicious input:
- SQL Injection
- XSS attacks
- Path Traversal
- Command Injection

**Test it:**
```bash
# This will be blocked with HTTP 400
curl "http://localhost:8080/api/users?query=' OR '1'='1"
```

### 3. Request Throttling (Automatic)
- 1000 requests/minute per IP
- Returns HTTP 429 when exceeded

---

## 📊 Monitoring

### Check Email Service Health
```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus | grep email

# Circuit breaker state
curl http://localhost:8080/actuator/health
```

### Check Password Reset Tokens
```bash
# In PostgreSQL
psql -U postgres -d crud_test_db

SELECT * FROM password_reset_tokens
WHERE expires_at > NOW()
ORDER BY created_at DESC
LIMIT 10;
```

### View Audit Logs
```bash
# Check recent password reset attempts
SELECT * FROM audit_logs
WHERE action IN ('PASSWORD_RESET_REQUESTED', 'PASSWORD_RESET_COMPLETED')
ORDER BY timestamp DESC
LIMIT 20;
```

---

## 🔧 Configuration Reference

### Email Settings
```properties
# application.properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${SMTP_USERNAME}
spring.mail.password=${SMTP_PASSWORD}
app.mail.from=noreply@example.com
app.mail.enabled=true
```

### Password Reset Settings
```properties
# Token expiration (default: 30 minutes)
app.security.password-reset.token-expiration-minutes=30

# Rate limit (default: 3 requests/hour)
app.security.password-reset.max-requests-per-hour=3
```

### Security Filter Settings
```properties
# Enable/disable features
app.rate-limit.enabled=true
app.features.email-notifications-enabled=true
```

---

## 🚨 Troubleshooting

### Email Not Sending

**Check 1: SMTP Credentials**
```bash
echo $SMTP_USERNAME
echo $SMTP_PASSWORD
```

**Check 2: Circuit Breaker State**
```bash
curl http://localhost:8080/actuator/health | jq .components.circuitBreakers
```

**Check 3: Application Logs**
```bash
tail -f logs/spring.log | grep -i email
```

**Check 4: Email Enabled**
```bash
# Verify in application.properties
app.mail.enabled=true
```

### Password Reset Not Working

**Issue: Token Invalid**
- Check token hasn't expired (30 minutes)
- Verify token hasn't been used already
- Check database: `SELECT * FROM password_reset_tokens WHERE token='...'`

**Issue: Rate Limited**
- Wait 1 hour before trying again
- Check audit logs for rate limit entries
- Reduce `max-requests-per-hour` if needed

**Issue: Email Not Received**
- Check spam folder
- Verify email address is correct
- Check email service logs

### Security Filter Blocking Valid Requests

**Issue: Input Sanitization False Positive**
```bash
# Temporarily disable for testing (NOT for production)
# Add to application.properties
app.security.input-sanitization.enabled=false
```

**Solution:**
- Refine regex patterns in `InputSanitizationFilter.java`
- Add whitelist for specific patterns
- Review logs to identify false positives

---

## 📖 API Quick Reference

### Password Reset Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/password-reset/initiate` | Request password reset | No |
| GET | `/api/v1/password-reset/validate` | Validate reset token | No |
| POST | `/api/v1/password-reset/reset` | Complete password reset | No |

### Example Requests

**Initiate Reset:**
```json
POST /api/v1/password-reset/initiate
{
  "email": "user@example.com"
}
```

**Validate Token:**
```
GET /api/v1/password-reset/validate?token=abc123...
```

**Reset Password:**
```json
POST /api/v1/password-reset/reset
{
  "token": "abc123...",
  "newPassword": "NewSecure123!",
  "confirmPassword": "NewSecure123!"
}
```

---

## 🎯 Testing Checklist

### Email Service
- [ ] SMTP credentials configured
- [ ] Test email sent successfully
- [ ] Email templates render correctly
- [ ] Circuit breaker configured
- [ ] Async processing working

### Password Reset
- [ ] Can initiate password reset
- [ ] Email received with reset link
- [ ] Token validation works
- [ ] Can reset password successfully
- [ ] Token expires after 30 minutes
- [ ] Rate limiting enforced
- [ ] Audit logs created

### Security
- [ ] Security headers present in responses
- [ ] SQL injection attempts blocked
- [ ] XSS attempts blocked
- [ ] Request throttling active
- [ ] OWASP ZAP scan passed

---

## 🚀 Production Deployment

### Pre-Deployment

1. **Configure SMTP for production:**
   ```bash
   export SMTP_HOST=smtp.yourprovider.com
   export SMTP_PORT=587
   export SMTP_USERNAME=production@yourcompany.com
   export SMTP_PASSWORD=secure-production-password
   export MAIL_FROM_ADDRESS=noreply@yourcompany.com
   ```

2. **Apply database migration:**
   ```bash
   mvn flyway:migrate -Pprod
   ```

3. **Run security scan:**
   ```bash
   docker run -t owasp/zap2docker-stable zap-baseline.py \
     -t http://your-staging-url
   ```

4. **Test password reset flow:**
   - Test with real email addresses
   - Verify email delivery
   - Test token expiration
   - Test rate limiting

5. **Configure monitoring:**
   - Set up Prometheus alerts
   - Configure email delivery monitoring
   - Set up error alerting

### Post-Deployment

1. **Monitor email delivery:**
   ```bash
   # Check circuit breaker state
   curl https://your-domain/actuator/health

   # Check email metrics
   curl https://your-domain/actuator/prometheus | grep email
   ```

2. **Review security logs:**
   ```bash
   # Check for blocked attacks
   SELECT * FROM audit_logs
   WHERE action = 'SUSPICIOUS_ACTIVITY'
   ORDER BY timestamp DESC;
   ```

3. **Performance validation:**
   - Run K6 load tests
   - Monitor response times
   - Check circuit breaker activation

---

## 📚 Additional Resources

**Documentation:**
- `EMAIL_AND_SECURITY_FEATURES.md` - Comprehensive guide
- `IMPLEMENTATION_STATUS_UPDATE.md` - Sprint summary
- `FINAL_IMPLEMENTATION_SUMMARY.md` - Overall project status

**Configuration Files:**
- `application.properties` - Main configuration
- `application-resilience4j.properties` - Circuit breaker config
- `logback-spring.xml` - Logging configuration

**Database:**
- `V9__add_password_reset_tokens.sql` - Migration script
- Schema: `password_reset_tokens` table

**Templates:**
- `src/main/resources/templates/*.html` - Email templates

---

## 💡 Tips & Best Practices

### Email
- Use app passwords, not account passwords
- Test email delivery in staging first
- Monitor circuit breaker state
- Set up SPF/DKIM records for production
- Use professional email addresses (no-reply@)

### Password Reset
- Keep token expiration short (30 minutes)
- Rate limit aggressively (3/hour)
- Never reveal if email exists (user enumeration)
- Log all reset attempts
- Use strong random tokens (UUID)

### Security
- Review CSP policy for your frontend
- Enable HSTS in production
- Run regular security scans
- Monitor for suspicious activity
- Keep dependencies updated

---

## ✅ Success Criteria

You've successfully configured everything when:
- ✅ Test email sends successfully
- ✅ Password reset flow completes end-to-end
- ✅ Security headers present in all responses
- ✅ Input sanitization blocks malicious input
- ✅ Request throttling returns 429 when exceeded
- ✅ Audit logs capture all events
- ✅ Circuit breaker protects email service
- ✅ All tests pass

---

## 🆘 Support

**Found an issue?**
1. Check logs: `tail -f logs/spring.log`
2. Check database: `psql -U postgres -d crud_test_db`
3. Check metrics: `curl http://localhost:8080/actuator/prometheus`
4. Review documentation: `docs/EMAIL_AND_SECURITY_FEATURES.md`

**Common Issues:**
- SMTP authentication failure → Check credentials
- Circuit breaker open → Check email service health
- Token expired → Tokens valid for 30 minutes only
- Rate limited → Wait 1 hour or adjust settings
- Input blocked → Review sanitization filters

---

**Version:** 1.0
**Status:** Production Ready
**Last Updated:** 2026-02-03

🎉 **Congratulations!** You've successfully configured enterprise-grade email notifications, secure password reset, and advanced security features!
