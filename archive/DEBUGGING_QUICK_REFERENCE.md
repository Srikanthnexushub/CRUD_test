# Repository Debugging - Quick Reference Card

**Print this page and keep it at your desk!**

---

## 🎯 ONE BREAKPOINT TO RULE THEM ALL

**File:** `RepositoryLoggingAspect.java`
**Line 117:** Set breakpoint here to intercept **ALL** repository queries

```java
Object result = joinPoint.proceed(); // ← BREAKPOINT HERE
```

**What you'll see:**
- Method name: `UserRepository.findByUsername`
- Parameters: `["admin"]`
- Execution time: `15ms`
- Result: `Optional[User(id=1)]`

---

## ⚡ Performance Thresholds

| Time | Symbol | Action |
|------|--------|--------|
| < 500ms | ⚡ | Good |
| 500-1000ms | ⏱️ | Monitor |
| > 1000ms | 🐌 | **FIX NOW!** |

---

## 🔥 Top 5 Breakpoint Spots

### 1. Login Query (Most Common)
**File:** `UserRepository.java` - Line 28
**Method:** `findByUsername()`
**Use:** Debug authentication

### 2. Audit Search (Slowest Query)
**File:** `AuditLogRepository.java` - Line 140
**Method:** `searchAuditLogs()`
**Use:** Optimize slow searches

### 3. Session Validation (Every Request)
**File:** `UserSessionRepository.java` - Line 37
**Method:** `findBySessionTokenAndIsActiveTrue()`
**Use:** Debug JWT validation

### 4. Email Queue (Scheduled)
**File:** `EmailNotificationRepository.java` - Line 39
**Method:** `findPendingEmails()`
**Use:** Debug email sending

### 5. Threat Assessment
**File:** `ThreatAssessmentRepository.java` - Line 39
**Method:** `findHighRiskAssessments()`
**Use:** Debug security alerts

---

## 📝 Enable Debug Logging

Add to `application.properties`:

```properties
# SQL with parameters
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Repository layer
logging.level.org.example.repository=DEBUG
logging.level.org.example.aspect=DEBUG

# Performance stats
spring.jpa.properties.hibernate.generate_statistics=true
```

---

## 🐛 Common Issues & Fixes

### ❌ Issue: No logs appearing
```bash
✅ Fix: Check application.properties has DEBUG enabled
✅ Rebuild: mvn clean package -DskipTests
```

### ❌ Issue: Query is slow (> 1000ms)
```bash
✅ Check: Database indexes exist
✅ Run: EXPLAIN ANALYZE <your-query>
✅ Add: Index on commonly queried columns
```

### ❌ Issue: N+1 query problem
```bash
✅ Symptom: 100 queries instead of 1
✅ Fix: Use @EntityGraph or JOIN FETCH
```

---

## 🔍 Log Output Examples

### Good Query (Fast)
```
⚡ [REPO-PERF] UserRepository.findByUsername took 15ms → Optional[User(id=1)]
```

### Warning Query (Moderate)
```
⏱️ [REPO-PERF] AuditLogRepository.findAll took 650ms → Page[50 elements]
```

### Bad Query (Slow - Fix This!)
```
🐌 [REPO-SLOW] AuditLogRepository.searchAuditLogs took 1350ms (SLOW!) → Page[...]
```

---

## 📊 Debugging Workflow

```
1. Enable DEBUG logging
   ↓
2. Run your action (login, search, etc.)
   ↓
3. Check console for 📥 [REPO-CALL] log
   ↓
4. Look at Hibernate SQL query
   ↓
5. Check ⚡/⏱️/🐌 performance indicator
   ↓
6. If slow (🐌), set breakpoint and investigate
```

---

## 🎓 Pro Tips

✅ **Tip 1:** Watch for repeated queries (N+1 problem)
✅ **Tip 2:** Check execution time BEFORE and AFTER index
✅ **Tip 3:** Use `EXPLAIN ANALYZE` in PostgreSQL
✅ **Tip 4:** Monitor scheduled tasks (email, session cleanup)
✅ **Tip 5:** Set breakpoint in Aspect to catch ALL queries

---

## 🆘 Emergency Debugging

**Something is broken and you don't know where?**

1. Set breakpoint: `RepositoryLoggingAspect.java` line 117
2. Run your action
3. When breakpoint hits, check method name
4. Read repository file comments for that method
5. Follow debugging guide for that scenario

---

## 📚 Full Documentation

See `REPOSITORY_DEBUGGING_GUIDE.md` for:
- Detailed scenarios
- All 12 repositories documented
- Performance tuning tips
- Troubleshooting guide

---

**Version:** 2.0 | **Updated:** January 29, 2026 | **Status:** ✅ Production-Ready
