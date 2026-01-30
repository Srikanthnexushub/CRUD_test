# Repository Layer Debugging Guide

**Last Updated:** January 29, 2026

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Debugging Tools](#debugging-tools)
4. [Breakpoint Locations](#breakpoint-locations)
5. [Performance Monitoring](#performance-monitoring)
6. [Common Scenarios](#common-scenarios)
7. [Troubleshooting](#troubleshooting)

---

## 🎯 Overview

This guide provides comprehensive debugging capabilities for the repository layer, enabling you to:

- **Debug SQL queries** with detailed parameter logging
- **Monitor query performance** with automatic timing
- **Set strategic breakpoints** for key operations
- **Track slow queries** (> 500ms warning, > 1000ms alert)
- **Inspect query results** before they reach the service layer

---

## 🚀 Quick Start

### Step 1: Enable Debug Logging

Ensure these properties are in `application.properties`:

```properties
# SQL Query Logging
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
logging.level.org.hibernate.type.descriptor.sql.BasicExtractor=TRACE

# Repository Layer Logging
logging.level.org.example.repository=DEBUG
logging.level.org.example.aspect=DEBUG

# Performance Statistics
spring.jpa.properties.hibernate.generate_statistics=true
```

### Step 2: Run Your Application

Start the Spring Boot application:

```bash
mvn clean package -DskipTests
java -jar target/CRUD_test-1.0-SNAPSHOT.jar
```

### Step 3: Watch the Logs

You'll see detailed logging like this:

```
📥 [REPO-CALL] UserRepository.findByUsername called with 1 parameters
   └─ Param[0] (String): "admin"
Hibernate: SELECT * FROM users WHERE username = ?
⚡ [REPO-PERF] UserRepository.findByUsername took 15ms → Optional[User(id=1)]
```

---

## 🛠️ Debugging Tools

### 1. RepositoryLoggingAspect (AOP)

**Location:** `/src/main/java/org/example/aspect/RepositoryLoggingAspect.java`

This aspect automatically logs:
- **Method calls** with parameters
- **Execution time** for each query
- **Return values** (formatted by type)
- **Exceptions** with stack traces

#### Key Features:

```java
// 📥 BEFORE: Logs method name + parameters
@Before("repositoryMethods()")
public void beforeRepositoryMethod(JoinPoint joinPoint)

// 📤 AFTER: Logs return value
@AfterReturning(pointcut = "repositoryMethods()", returning = "result")
public void afterRepositoryMethodReturning(JoinPoint joinPoint, Object result)

// ❌ ERROR: Logs exceptions
@AfterThrowing(pointcut = "repositoryMethods()", throwing = "exception")
public void afterRepositoryMethodThrowing(JoinPoint joinPoint, Throwable exception)

// ⚡ AROUND: Measures execution time
@Around("repositoryMethods()")
public Object aroundRepositoryMethod(ProceedingJoinPoint joinPoint)
```

#### Performance Thresholds:

| Time Range | Log Level | Symbol | Meaning |
|------------|-----------|--------|---------|
| < 500ms    | DEBUG     | ⚡      | Fast query |
| 500-1000ms | INFO      | ⏱️      | Moderate query |
| > 1000ms   | WARN      | 🐌     | Slow query (investigate!) |

### 2. Breakpoint Comments in Repositories

All 12 repositories have detailed comments indicating:
- **Where to set breakpoints** for specific scenarios
- **What SQL to expect** for custom queries
- **Performance considerations** for slow queries

Example:

```java
// BREAKPOINT: **CRITICAL** Debug complex native SQL query
// Watch for: CAST operations, NULL checks, dynamic filtering
// Performance: May be slow on large datasets - check execution time
@Query(value = "SELECT * FROM audit_logs a WHERE ...")
Page<AuditLog> searchAuditLogs(...);
```

### 3. Enhanced Hibernate Logging

With the configuration enabled, you'll see:

```sql
-- SQL Query
Hibernate:
    select
        user0_.id as id1_14_,
        user0_.username as username2_14_,
        user0_.email as email3_14_,
        ...
    from
        users user0_
    where
        user0_.username=?

-- Parameter Binding
2026-01-29 10:30:15.123 TRACE --- binding parameter [1] as [VARCHAR] - [admin]

-- Result Extraction
2026-01-29 10:30:15.145 TRACE --- extracted value ([id1_14_] : [BIGINT]) - [1]
```

---

## 📍 Breakpoint Locations

### Strategic Breakpoint in RepositoryLoggingAspect

**Most Powerful:** Set ONE breakpoint here to intercept ALL repository calls:

**File:** `RepositoryLoggingAspect.java`

| Line | Method | Purpose | When to Use |
|------|--------|---------|-------------|
| **117** | `aroundRepositoryMethod()` | Breaks BEFORE query execution | Inspect parameters before SQL runs |
| **123** | `aroundRepositoryMethod()` | Breaks AFTER query execution | Inspect results and execution time |
| **53** | `beforeRepositoryMethod()` | Logs method call details | View parameters in formatted logs |
| **80** | `afterRepositoryMethodReturning()` | Logs return values | View query results |
| **99** | `afterRepositoryMethodThrowing()` | Catches query errors | Debug SQL exceptions |

### Repository-Specific Breakpoints

#### High-Priority Repositories (Set breakpoints here first):

**1. UserRepository** - Authentication & Login
- Line 28: `findByUsername()` - Debug login queries
- Line 30: `findByEmail()` - Debug email lookups

**2. AuditLogRepository** - Security Monitoring
- Line 140-145: `searchAuditLogs()` - Complex native SQL (slowest query)
- Line 174: `countFailedLoginAttempts()` - Brute force detection

**3. ThreatAssessmentRepository** - Threat Intelligence
- Line 39: `findHighRiskAssessments()` - High-risk threat detection
- Line 47: `countRecentAssessments()` - Count aggregation

**4. UserSessionRepository** - Session Management
- Line 37: `findBySessionTokenAndIsActiveTrue()` - Every authenticated request
- Line 57: `findExpiredSessions()` - Scheduled cleanup (runs every minute)

**5. EmailNotificationRepository** - Email Queue
- Line 39: `findPendingEmails()` - Email queue processing (runs every 5 minutes)
- Line 43: `findFailedEmailsForRetry()` - Retry logic

---

## 📊 Performance Monitoring

### Automatic Performance Tracking

The `RepositoryLoggingAspect` automatically tracks execution time:

```
⚡ [REPO-PERF] UserRepository.findByUsername took 15ms → Optional[User(id=1)]
⏱️ [REPO-PERF] AuditLogRepository.searchAuditLogs took 650ms → Page[50 elements, page 1/10, total 500]
🐌 [REPO-SLOW] AuditLogRepository.searchAuditLogs took 1250ms (SLOW!) → Page[...]
```

### Hibernate Statistics

Enable statistics to track:
- Query execution counts
- Cache hit/miss ratios
- Connection pool usage

**Enable in application.properties:**
```properties
spring.jpa.properties.hibernate.generate_statistics=true
logging.level.org.hibernate.stat=DEBUG
```

**View statistics in logs:**
```
Statistics: 120 queries, 45ms avg, 5400ms total, 80% cache hit rate
```

### Identifying Slow Queries

**Watch for these patterns:**

1. **N+1 Query Problem:**
   ```
   ⚡ [REPO-PERF] UserRepository.findAll took 10ms → List[100 elements]
   ⚡ [REPO-PERF] ThreatAssessmentRepository.findByUser took 5ms → List[5 elements]
   ⚡ [REPO-PERF] ThreatAssessmentRepository.findByUser took 5ms → List[5 elements]
   ... (repeated 100 times)
   ```
   **Solution:** Use `@EntityGraph` or JOIN FETCH

2. **Missing Index:**
   ```
   🐌 [REPO-SLOW] AuditLogRepository.searchByUsernameOrAction took 1500ms (SLOW!)
   ```
   **Solution:** Add database index on `username` and `action` columns

3. **Large Result Sets:**
   ```
   ⏱️ [REPO-PERF] AuditLogRepository.findAll took 800ms → List[10000 elements]
   ```
   **Solution:** Use pagination (`Pageable`)

---

## 🔧 Common Debugging Scenarios

### Scenario 1: Debug Failed Login

**Goal:** Understand why a user can't log in

**Steps:**

1. Set breakpoint in `RepositoryLoggingAspect.java` line 117
2. Attempt login
3. When breakpoint hits, inspect:
   - Method: `UserRepository.findByUsername()`
   - Parameters: `[Param[0] (String): "username"]`
4. Step through to line 123
5. Inspect return value: `Optional.empty` or `Optional[User(id=X)]`

**Expected Log Output:**
```
📥 [REPO-CALL] UserRepository.findByUsername called with 1 parameters
   └─ Param[0] (String): "admin"
Hibernate: SELECT ... FROM users WHERE username = ?
⚡ [REPO-PERF] UserRepository.findByUsername took 12ms → Optional[User(id=1)]
```

### Scenario 2: Debug Slow Audit Log Search

**Goal:** Optimize slow search query

**Steps:**

1. Open `AuditLogRepository.java`
2. Find line 140: `searchAuditLogs()` method
3. Read comment: "BREAKPOINT: **CRITICAL** Debug complex native SQL"
4. Set breakpoint in `RepositoryLoggingAspect.java` line 117
5. Trigger audit log search
6. When breakpoint hits, inspect parameters:
   - `userId`, `eventType`, `status`, `startDate`, `endDate`, `searchTerm`
7. Check execution time after query completes
8. If > 1000ms, check database indexes

**Expected Log Output:**
```
📥 [REPO-CALL] AuditLogRepository.searchAuditLogs called with 7 parameters
   └─ Param[0] (Long): 1
   └─ Param[1] (String): "LOGIN_FAILURE"
   └─ Param[2] (String): "failed"
   └─ Param[3] (LocalDateTime): 2026-01-01T00:00:00
   └─ Param[4] (LocalDateTime): 2026-01-29T23:59:59
   └─ Param[5] (String): "admin"
   └─ Param[6] (Pageable): page=0, size=20, sort=timestamp: DESC
Hibernate: SELECT * FROM audit_logs a WHERE ... (native SQL)
🐌 [REPO-SLOW] AuditLogRepository.searchAuditLogs took 1350ms (SLOW!) → Page[20 elements, page 1/25, total 500]
```

### Scenario 3: Debug MFA Verification

**Goal:** Understand MFA flow during login

**Steps:**

1. Set breakpoint in `RepositoryLoggingAspect.java` line 117
2. Enable filter: Method name contains "MFA" or "TrustedDevice"
3. Attempt login with MFA enabled
4. Watch sequence of queries:
   - `MFASettingsRepository.findByUser()` - Get TOTP secret
   - `TrustedDeviceRepository.findByUserAndDeviceFingerprintHashAndIsActiveTrue()` - Check trusted device
   - `BackupCodeRepository.findByUserAndIsUsedFalse()` - Get backup codes (if needed)

**Expected Log Sequence:**
```
📥 [REPO-CALL] MFASettingsRepository.findByUser called with 1 parameters
⚡ [REPO-PERF] MFASettingsRepository.findByUser took 8ms → Optional[MFASettings(id=1)]

📥 [REPO-CALL] TrustedDeviceRepository.findByUserAndDeviceFingerprintHashAndIsActiveTrue called with 2 parameters
⚡ [REPO-PERF] TrustedDeviceRepository.findByUserAndDeviceFingerprintHashAndIsActiveTrue took 5ms → Optional.empty

📥 [REPO-CALL] BackupCodeRepository.countByUserAndIsUsedFalse called with 1 parameters
⚡ [REPO-PERF] BackupCodeRepository.countByUserAndIsUsedFalse took 3ms → 8
```

### Scenario 4: Debug Email Queue Processing

**Goal:** Understand why emails aren't sending

**Steps:**

1. Open `EmailNotificationRepository.java`
2. Find line 39: `findPendingEmails()` - Runs every 5 minutes
3. Set breakpoint in `RepositoryLoggingAspect.java` line 117
4. Wait for scheduled task to run
5. Inspect:
   - How many emails are PENDING?
   - What's the execution time?
6. Check `EmailService` logs for sending errors

**Expected Log Output:**
```
📥 [REPO-CALL] EmailNotificationRepository.findPendingEmails called with 2 parameters
   └─ Param[0] (LocalDateTime): 2026-01-29T10:30:00
   └─ Param[1] (Pageable): page=0, size=50, sort=UNSORTED
Hibernate: SELECT ... FROM email_notifications WHERE status = 'PENDING' AND ...
⚡ [REPO-PERF] EmailNotificationRepository.findPendingEmails took 25ms → List[12 elements]
```

### Scenario 5: Debug Rate Limit Blocking

**Goal:** Understand why a user is getting 429 errors

**Steps:**

1. Set breakpoint in `RepositoryLoggingAspect.java` line 117
2. Filter for `RateLimitLogRepository` or `RateLimitWhitelistRepository`
3. Make several rapid requests
4. Watch for:
   - `RateLimitWhitelistRepository.existsByIpAddressAndIsActiveTrue()` - Whitelist check (every request)
   - `RateLimitLogRepository.findByIpAddressAndCreatedAtAfter()` - Recent requests check
5. Check rate limit configuration

**Expected Log Output:**
```
📥 [REPO-CALL] RateLimitWhitelistRepository.existsByIpAddressAndIsActiveTrue called with 1 parameters
   └─ Param[0] (String): "127.0.0.1"
⚡ [REPO-PERF] RateLimitWhitelistRepository.existsByIpAddressAndIsActiveTrue took 3ms → false

📥 [REPO-CALL] RateLimitLogRepository.findByIpAddressAndCreatedAtAfter called with 2 parameters
   └─ Param[0] (String): "127.0.0.1"
   └─ Param[1] (LocalDateTime): 2026-01-29T10:29:00
⚡ [REPO-PERF] RateLimitLogRepository.findByIpAddressAndCreatedAtAfter took 8ms → List[6 elements]
```

---

## 🐛 Troubleshooting

### Issue 1: No Logs Appearing

**Symptom:** No repository logs in console

**Diagnosis:**
1. Check `application.properties` has debug logging enabled
2. Ensure `RepositoryLoggingAspect` bean is created
3. Verify `spring-boot-starter-aop` dependency is in `pom.xml`

**Solution:**
```bash
# Check if AOP is working
grep "RepositoryLoggingAspect" logs/application.log

# Rebuild and restart
mvn clean package -DskipTests
java -jar target/CRUD_test-1.0-SNAPSHOT.jar
```

### Issue 2: Breakpoints Not Hitting

**Symptom:** Breakpoint in `RepositoryLoggingAspect` never triggers

**Diagnosis:**
1. Verify debugger is attached to JVM
2. Check if repository method is actually being called
3. Ensure breakpoint is on correct line (117, 123, etc.)

**Solution:**
```bash
# Run in debug mode
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar target/CRUD_test-1.0-SNAPSHOT.jar

# Attach debugger to port 5005
```

### Issue 3: SQL Not Showing Parameters

**Symptom:** See `?` in SQL but no parameter values

**Diagnosis:**
Parameter binding logging is not enabled at TRACE level

**Solution:**
```properties
# Add to application.properties
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

### Issue 4: Performance Numbers Seem Wrong

**Symptom:** Execution time is unexpectedly high/low

**Diagnosis:**
1. Check if query is hitting cache (first execution is always slower)
2. Verify database indexes exist
3. Check connection pool settings

**Solution:**
```bash
# Check database indexes
psql -d crud_test_db -c "\d+ users"

# Check query plan
psql -d crud_test_db -c "EXPLAIN ANALYZE SELECT * FROM users WHERE username = 'admin';"
```

---

## 📚 Reference

### All Repository Files with Debug Comments

1. ✅ **UserRepository** - Simple derived queries
2. ✅ **AuditLogRepository** - Complex native SQL queries (SLOWEST)
3. ✅ **ThreatAssessmentRepository** - JPQL with date ranges
4. ✅ **RateLimitLogRepository** - Aggregation queries
5. ✅ **EmailNotificationRepository** - Queue processing
6. ✅ **UserSessionRepository** - Session lifecycle
7. ✅ **MFASettingsRepository** - MFA configuration
8. ✅ **BackupCodeRepository** - MFA backup codes
9. ✅ **TrustedDeviceRepository** - Device trust management
10. ✅ **IPReputationCacheRepository** - Threat intelligence caching
11. ✅ **RateLimitWhitelistRepository** - Rate limit bypass
12. ✅ **NotificationPreferenceRepository** - Email preferences

### Key Configuration Properties

```properties
# SQL Logging
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
logging.level.org.hibernate.type.descriptor.sql.BasicExtractor=TRACE

# Repository Aspect Logging
logging.level.org.example.aspect=DEBUG
logging.level.org.example.repository=DEBUG

# Performance Statistics
spring.jpa.properties.hibernate.generate_statistics=true
logging.level.org.hibernate.stat=DEBUG
```

### Log Output Legend

| Symbol | Meaning | Log Level |
|--------|---------|-----------|
| 📥 | Repository method call (BEFORE) | DEBUG |
| 📤 | Repository method return (AFTER) | DEBUG |
| ❌ | Repository method exception (ERROR) | ERROR |
| ⚡ | Fast query (< 500ms) | DEBUG |
| ⏱️ | Moderate query (500-1000ms) | INFO |
| 🐌 | Slow query (> 1000ms) | WARN |

---

## 🎓 Best Practices

1. **Start with RepositoryLoggingAspect breakpoint** - Line 117 intercepts ALL queries
2. **Enable logging BEFORE debugging** - Logs provide context for breakpoints
3. **Watch for slow queries** - Anything > 1000ms needs investigation
4. **Check database indexes** - Most slow queries need indexes
5. **Use pagination** - Don't fetch 10,000 rows without `Pageable`
6. **Monitor scheduled tasks** - Email queue, session cleanup, cache cleanup
7. **Profile in production** - Use sampling to avoid performance impact

---

## 📞 Support

For questions or issues with debugging:

1. Check this guide first
2. Review repository file comments
3. Enable debug logging and check logs
4. Use breakpoints in `RepositoryLoggingAspect`
5. Check database query plan with `EXPLAIN ANALYZE`

---

**Updated:** January 29, 2026
**Version:** 2.0 (Phase 2 - Enterprise Security Bundle)
**Status:** ✅ Production-Ready
