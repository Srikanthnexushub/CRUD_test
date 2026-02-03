# Security Policy

## Reporting a Vulnerability

We take the security of this project seriously. If you discover a security vulnerability, please follow these steps:

1. **DO NOT** open a public issue
2. Email security concerns to: security@ainexusstudio.com
3. Include detailed information about the vulnerability
4. Allow up to 48 hours for an initial response

## Security Best Practices

### 1. Credential Management

**❌ NEVER commit credentials to version control**

```bash
# Bad - Do not do this!
JWT_SECRET=my-secret-key
SMTP_PASSWORD=mypassword123
```

**✅ Use environment variables**

```bash
# Good - Use environment variables
export JWT_SECRET=$(openssl rand -base64 32)
export SMTP_PASSWORD="${SECURE_PASSWORD}"
```

### 2. Required Security Configuration

Before deploying to production:

#### Generate Secure JWT Secret
```bash
# Generate a cryptographically secure random key
openssl rand -base64 32
# Set as environment variable
export JWT_SECRET="<generated-key>"
```

#### Change Default Admin Password
```bash
# Never use default passwords in production!
export ADMIN_PASSWORD="<strong-unique-password>"
```

#### Configure SMTP Securely
```bash
# For Gmail, use App-Specific Passwords (not your Gmail password)
# https://support.google.com/accounts/answer/185833
export SMTP_USERNAME="your-email@gmail.com"
export SMTP_PASSWORD="<app-specific-password>"
```

#### Database Credentials
```bash
# Use strong, unique passwords
export DB_PASSWORD="<strong-unique-password>"
export REDIS_PASSWORD="<strong-unique-password>"
```

### 3. Files That Should NEVER Be Committed

The following files must NEVER be committed to version control:

- `.env` - Contains actual credentials
- `.env.local` - Local environment overrides
- `*.pem`, `*.key` - Private keys and certificates
- `*.p12`, `*.jks` - Keystores
- `application-secrets.properties` - Secret configuration
- `serviceAccountKey.json` - Cloud service account keys

**Note**: `.env.example` is safe to commit as it contains only placeholders.

### 4. Secure Deployment Checklist

Before deploying to production:

- [ ] All default passwords changed
- [ ] JWT secret is a strong random value (min 256 bits)
- [ ] Database credentials are secure and unique
- [ ] SMTP credentials use app-specific passwords
- [ ] Redis has password protection enabled
- [ ] HTTPS/TLS is enabled
- [ ] CORS is configured for production domains only
- [ ] Rate limiting is enabled
- [ ] Admin endpoints are properly protected
- [ ] Actuator endpoints require authentication
- [ ] Error messages don't expose sensitive information
- [ ] Database backups are encrypted
- [ ] Audit logging is enabled

### 5. GitGuardian Alert Resolution

If GitGuardian detects exposed secrets:

1. **Immediately revoke the exposed credentials**
   - Change passwords
   - Regenerate API keys
   - Rotate JWT secrets

2. **Remove secrets from Git history**
   ```bash
   # Use BFG Repo-Cleaner or git-filter-repo
   git filter-repo --path .env --invert-paths
   ```

3. **Update environment variables**
   ```bash
   # Set new secure values
   export JWT_SECRET=$(openssl rand -base64 32)
   export SMTP_PASSWORD="<new-app-password>"
   ```

4. **Force push cleaned history** (⚠️ Only if repository is private and you're the sole contributor)
   ```bash
   git push --force
   ```

### 6. Production Environment Variables

Use a secrets management system in production:

#### Option 1: Cloud Provider Secrets Manager
- AWS Secrets Manager
- Google Cloud Secret Manager
- Azure Key Vault

#### Option 2: Kubernetes Secrets
```bash
kubectl create secret generic app-secrets \
  --from-literal=JWT_SECRET='<your-secret>' \
  --from-literal=DB_PASSWORD='<your-password>' \
  --from-literal=SMTP_PASSWORD='<your-password>'
```

#### Option 3: Docker Secrets
```bash
echo "<your-secret>" | docker secret create jwt_secret -
echo "<your-password>" | docker secret create db_password -
```

### 7. Security Headers

The application includes security headers:

- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 1; mode=block`
- `Strict-Transport-Security: max-age=31536000`
- `Content-Security-Policy` (CSP)

### 8. Authentication & Authorization

- JWT tokens with secure signing (HS256/RS256)
- Password hashing with BCrypt (strength 12)
- MFA/2FA support with TOTP
- Account locking after failed attempts
- IP-based rate limiting
- Session management and timeout

### 9. Data Protection

- Passwords are never stored in plain text
- All passwords are hashed with BCrypt
- Sensitive data is encrypted at rest
- TLS/HTTPS for data in transit
- Database connections are encrypted
- Redis connection uses password auth

### 10. Security Monitoring

Enable monitoring for:

- Failed login attempts
- Rate limit violations
- Unusual access patterns
- API endpoint abuse
- Database query errors
- Authentication errors

### 11. Regular Security Updates

```bash
# Update dependencies regularly
mvn versions:display-dependency-updates
npm audit fix

# Check for vulnerabilities
mvn dependency-check:check
npm audit
```

### 12. Security Testing

#### Automated Security Scanning
```bash
# OWASP Dependency Check
mvn dependency-check:check

# NPM Audit
cd frontend && npm audit

# Container Scanning
docker scan <image-name>
```

#### Manual Security Testing
- [ ] SQL Injection testing
- [ ] XSS testing
- [ ] CSRF testing
- [ ] Authentication bypass attempts
- [ ] Authorization bypass attempts
- [ ] Rate limit testing
- [ ] Input validation testing

## Secure Configuration Examples

### Application Properties (Production)
```properties
# Use environment variables for ALL sensitive values
jwt.secret=${JWT_SECRET}
spring.datasource.password=${DB_PASSWORD}
spring.mail.password=${SMTP_PASSWORD}
spring.data.redis.password=${REDIS_PASSWORD}
```

### Docker Compose (Production)
```yaml
services:
  app:
    environment:
      - JWT_SECRET=${JWT_SECRET}
      - DB_PASSWORD=${DB_PASSWORD}
      - SMTP_PASSWORD=${SMTP_PASSWORD}
    # Never hardcode secrets in docker-compose.yml!
```

### Kubernetes (Production)
```yaml
env:
  - name: JWT_SECRET
    valueFrom:
      secretKeyRef:
        name: app-secrets
        key: jwt-secret
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: app-secrets
        key: db-password
```

## Quick Security Audit

Run this command to check for common security issues:

```bash
# Check for exposed secrets in git history
git log --all --full-history --source --find-object=<file-with-secret>

# Search for potential secrets in codebase
grep -r "password\|secret\|key\|token" --include="*.properties" --include="*.yml" .

# Check .env files are not committed
git ls-files | grep "^\.env$"
```

## Security Contact

For security concerns, contact:
- Email: security@ainexusstudio.com
- Security Advisory: https://github.com/Srikanthnexushub/CRUD_test/security/advisories

## Security Updates

We will notify users of security updates through:
- GitHub Security Advisories
- Email notifications to repository watchers
- Detailed CHANGELOG entries

## Compliance

This application implements security measures aligned with:
- OWASP Top 10
- CWE Top 25
- NIST Cybersecurity Framework
- GDPR data protection requirements
- SOC 2 security controls

---

**Last Updated**: February 2026

**Remember**: Security is an ongoing process, not a one-time setup. Regularly review and update security measures.
