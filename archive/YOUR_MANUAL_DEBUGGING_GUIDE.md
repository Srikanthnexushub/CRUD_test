# 🎓 YOUR MANUAL DEBUGGING GUIDE
## Step-by-Step Instructions for Debugging Repository Queries

**Status:** ✅ Backend is running and debugging is WORKING!
**Your Backend PID:** 81282

---

## 🚀 QUICK START (3 Steps)

### Step 1: Open Your IDE

**IntelliJ IDEA / VS Code / Eclipse**

### Step 2: Open This File
```
src/main/java/org/example/aspect/RepositoryLoggingAspect.java
```

### Step 3: Set ONE Breakpoint
**Line 117** - This line catches EVERY database query!

```java
Object result = joinPoint.proceed(); // ← CLICK HERE TO SET BREAKPOINT
```

**That's it!** Now every time a database query runs, your breakpoint will pause execution.

---

## 📱 METHOD 1: Debug with IDE (Recommended)

### For IntelliJ IDEA:

#### A. Start Backend in Debug Mode

**Option 1 - From IDE:**
1. Open `src/main/java/org/example/CrudTestApplication.java`
2. Right-click anywhere in the file
3. Select **"Debug 'CrudTestApplication.main()'"**
4. Wait for green message: "Started CrudTestApplication"

**Option 2 - Attach to Running Backend:**
1. Menu: **Run → Attach to Process**
2. Select process: `java -jar CRUD_test-1.0-SNAPSHOT.jar` (PID: 81282)

#### B. Set Breakpoint

1. Open `src/main/java/org/example/aspect/RepositoryLoggingAspect.java`
2. Find **line 117**: `Object result = joinPoint.proceed();`
3. Click in the left gutter (next to line number)
4. You'll see a red circle ⭕ - breakpoint is set!

#### C. Test It

1. Open terminal and run:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin1234","deviceFingerprint":"test"}'
```

2. **Your IDE will PAUSE!**

3. **Inspect Variables:** (Bottom panel → Variables tab)
   - `joinPoint.getSignature().getName()` → Shows method name (e.g., "findByUsername")
   - `joinPoint.getArgs()` → Shows parameters (e.g., ["admin"])

4. **Press F8** (or click "Step Over" button) to execute query

5. **Inspect Result:**
   - `result` → Shows what the query returned (e.g., Optional[User(id=8)])
   - `executionTime` → Shows how long it took (e.g., 3ms)

6. **Press F9** (or click "Resume" button) to continue

#### D. Advanced: Filter Specific Queries

1. Right-click on your breakpoint (red circle)
2. Select **"More"** or **"Edit Breakpoint"**
3. Add condition:
   ```java
   joinPoint.getSignature().getName().equals("findByUsername")
   ```
4. Now it only breaks on `findByUsername()` queries!

**Other useful filters:**
```java
// Only break on UserRepository queries
joinPoint.getSignature().getDeclaringTypeName().contains("UserRepository")

// Only break on slow queries (> 100ms)
// Note: Can't check execution time BEFORE query runs, use logs for this

// Only break on save/insert operations
joinPoint.getSignature().getName().startsWith("save")
```

---

## 📝 METHOD 2: Debug with Logs (No IDE Needed)

### A. Watch Logs in Real-Time

**Open Terminal and run:**
```bash
cd /Users/ainexusstudio/Documents/GitHub/CRUD_test
tail -f backend-debug.log | grep --color -E "(REPO-CALL|REPO-RETURN|REPO-PERF|REPO-SLOW|REPO-ERROR)"
```

**What you'll see:**
```
📥 [REPO-CALL] UserRepository.findByUsername called with 1 parameters
   └─ Param[0] (String): "admin"
📤 [REPO-RETURN] UserRepository.findByUsername returned: Optional[User(id=8)]
⚡ [REPO-PERF] UserRepository.findByUsername took 3ms → Optional[User(id=8)]
```

### B. Test It

**In another terminal window, run:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin1234","deviceFingerprint":"test"}'
```

**Watch the first terminal - you'll see all the queries!**

### C. Search Logs for Specific Issues

**Find all login queries:**
```bash
grep "findByUsername" backend-debug.log
```

**Find slow queries (> 1000ms):**
```bash
grep "🐌" backend-debug.log
# or
grep "REPO-SLOW" backend-debug.log
```

**Find errors:**
```bash
grep "❌" backend-debug.log
# or
grep "REPO-ERROR" backend-debug.log
```

**Find queries for specific user:**
```bash
grep -B 2 "admin" backend-debug.log | grep "REPO-CALL"
```

**Find today's queries:**
```bash
grep "$(date +%Y-%m-%d)" backend-debug.log | grep "REPO-PERF"
```

---

## 🎯 METHOD 3: Debug Specific Scenarios

### Scenario 1: Debug Why Login is Failing

**Steps:**
1. Set breakpoint on line 117 in `RepositoryLoggingAspect.java`
2. Add condition: `joinPoint.getSignature().getName().equals("findByUsername")`
3. Attempt login
4. When breakpoint hits, check:
   - `joinPoint.getArgs()[0]` - Is the username correct?
5. Press F8 to execute query
6. Check `result`:
   - `Optional[User(...)]` - User found ✅
   - `Optional.empty` - User NOT found ❌ (wrong username?)

**Or use logs:**
```bash
tail -f backend-debug.log | grep "findByUsername"
# Then attempt login
# Check if it returns Optional[User(id=X)] or Optional.empty
```

### Scenario 2: Debug Slow Queries

**Using Logs (Easiest):**
```bash
# Watch for slow queries in real-time
tail -f backend-debug.log | grep -E "(REPO-SLOW|REPO-PERF)" | grep -v "MessageBroker"
```

**What to look for:**
```
⚡ [REPO-PERF] UserRepository.findByUsername took 3ms → GOOD!
⏱️ [REPO-PERF] AuditLogRepository.searchAuditLogs took 650ms → WATCH THIS
🐌 [REPO-SLOW] AuditLogRepository.searchAuditLogs took 1350ms (SLOW!) → FIX THIS!
```

**If you see a slow query:**
1. Note the repository name and method (e.g., `AuditLogRepository.searchAuditLogs`)
2. Open that repository file
3. Find the method (search for "searchAuditLogs")
4. Check what columns are being queried
5. Add database indexes:
   ```sql
   CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);
   CREATE INDEX idx_audit_username ON audit_logs(username);
   ```

### Scenario 3: Debug Duplicate Key Errors

**From the logs, I already found one for you!**
```
❌ [REPO-ERROR] CrudRepository.save threw exception: duplicate key value violates unique constraint "idx_ip_cache_address"
```

**This shows:**
- What query failed: `CrudRepository.save`
- The error: Duplicate key in `idx_ip_cache_address`
- The entity: `IPReputationCache`

**Fix:** Check if you're trying to insert an IP that already exists in the cache.

### Scenario 4: Debug Missing Data

**Problem:** User says "I can't see my data"

**Solution:**
```bash
# Watch all SELECT queries
tail -f backend-debug.log | grep -E "REPO-CALL.*find"

# Then perform the action that should show data
# Check if the query is even being called
# Check what parameters are being passed
# Check what's being returned
```

**Example:**
```
📥 [REPO-CALL] UserSessionRepository.findByUserIdAndIsActiveTrue called with 1 parameters
   └─ Param[0] (Long): 8
📤 [REPO-RETURN] UserSessionRepository.findByUserIdAndIsActiveTrue returned: ArrayList[6 elements]
```

If it returns `ArrayList[0 elements]`, the data doesn't exist in the database!

---

## 📊 Understanding the Logs

### Log Symbol Guide:

| Symbol | Meaning | Action |
|--------|---------|--------|
| 📥 | Query is about to run | Shows method name + parameters |
| 📤 | Query finished successfully | Shows what was returned |
| ⚡ | Fast query (< 500ms) | ✅ Good! |
| ⏱️ | Moderate query (500-1000ms) | ⚠️ Monitor this |
| 🐌 | Slow query (> 1000ms) | ❌ Needs optimization! |
| ❌ | Query failed with error | 🐛 Bug found! |

### Reading Parameter Types:

```
Param[0] (String): "admin"          ← Text parameter
Param[0] (Long): 8                  ← Number (user ID)
Param[0] (LocalDateTime): 2026-...  ← Date/time
Param[0] (Pageable): page=0, size=20 ← Pagination
Param[0] (User): id=8               ← Entity object
```

### Reading Return Values:

```
Optional[User(id=8)]                ← Found one user
Optional.empty                      ← Found nothing
ArrayList[13 elements]              ← Found 13 items
Page[20 elements, page 1/10]        ← Page 1 of 10 (20 items shown)
```

---

## 🔧 Common Tasks

### Task 1: See All Queries During Login

```bash
# Start watching
tail -f backend-debug.log | grep "REPO-CALL"

# In another terminal, login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin1234","deviceFingerprint":"test"}'

# You'll see ALL queries executed during login!
```

### Task 2: Count How Many Queries Are Executed

```bash
# During a specific time period
grep "REPO-CALL" backend-debug.log | wc -l

# For a specific repository
grep "UserRepository" backend-debug.log | grep "REPO-CALL" | wc -l
```

### Task 3: Find the Slowest Query

```bash
grep "REPO-PERF" backend-debug.log | sort -t' ' -k9 -n | tail -n 10
```

### Task 4: Export Logs for Analysis

```bash
# Export today's queries to CSV
grep "REPO-PERF" backend-debug.log | \
  grep "$(date +%Y-%m-%d)" | \
  awk '{print $1","$2","$9","$13}' > queries.csv

# Open in Excel or Google Sheets
```

---

## 🎓 Practice Exercises

### Exercise 1: Debug a Login Query

**Goal:** See exactly what happens during login

**Steps:**
1. Open terminal
2. Run: `tail -f backend-debug.log | grep -E "(REPO|findByUsername)"`
3. In another terminal:
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"Admin1234","deviceFingerprint":"test"}'
   ```
4. **Count:** How many queries were executed? (Answer: ~8-10)
5. **Find:** Which query checked the username? (Answer: `findByUsername`)
6. **Check:** How long did it take? (Answer: ~2-5ms)

### Exercise 2: Find a Slow Query

**Goal:** Identify and fix a slow query

**Steps:**
1. Open terminal
2. Run: `tail -f backend-debug.log | grep "REPO-SLOW"`
3. Use the application and watch for any slow queries
4. If you see `🐌 [REPO-SLOW]`, note the method name
5. Open that repository file
6. Check if indexes exist on queried columns

### Exercise 3: Debug with Breakpoint

**Goal:** Pause execution and inspect variables

**Steps:**
1. Open IntelliJ IDEA
2. Open `RepositoryLoggingAspect.java`
3. Set breakpoint on line 117
4. Debug the application
5. Trigger a login
6. When breakpoint hits:
   - Check variable `joinPoint.getSignature().getName()`
   - Check variable `joinPoint.getArgs()`
7. Press F8 to continue
8. Check variable `result`
9. Check variable `executionTime`

---

## 📚 Reference Files

**Read these for more details:**

1. **REPOSITORY_DEBUGGING_GUIDE.md** - Full comprehensive guide (500+ lines)
2. **DEBUGGING_QUICK_REFERENCE.md** - One-page cheat sheet (print it!)
3. **YOUR_MANUAL_DEBUGGING_GUIDE.md** - This file (step-by-step)

**Repository Files (with breakpoint comments):**

All 12 repository files now have debugging comments:
- `UserRepository.java` - Line 28: `findByUsername()`
- `AuditLogRepository.java` - Line 140: `searchAuditLogs()` (slowest)
- `ThreatAssessmentRepository.java` - Line 39: `findHighRiskAssessments()`
- `UserSessionRepository.java` - Line 37: `findBySessionTokenAndIsActiveTrue()`
- `EmailNotificationRepository.java` - Line 39: `findPendingEmails()`
- And 7 more...

---

## 🆘 Troubleshooting

### Problem: Breakpoint Not Hitting

**Solution 1:** Make sure you're in Debug mode
- IntelliJ: Look for debug icon (bug) in top-right
- Should say "Debugging - CrudTestApplication"

**Solution 2:** Check if breakpoint is enabled
- Red circle should be solid ⭕, not greyed out
- Right-click → "Enable"

**Solution 3:** Rebuild and restart
```bash
mvn clean package -DskipTests
# Then restart in debug mode
```

### Problem: No Logs Showing

**Solution 1:** Check log level
```bash
grep "logging.level.org.example.aspect" src/main/resources/application.properties
# Should show: logging.level.org.example.aspect=DEBUG
```

**Solution 2:** Check if backend is running
```bash
lsof -i:8080
# Should show java process
```

**Solution 3:** Check log file exists
```bash
ls -lh backend-debug.log
# Should show file with recent timestamp
```

### Problem: Too Many Logs

**Solution:** Filter for specific repository
```bash
tail -f backend-debug.log | grep "UserRepository"
```

**Or filter out WebSocket queries:**
```bash
tail -f backend-debug.log | grep "REPO" | grep -v "MessageBroker"
```

---

## ✅ Summary

**You now have 3 ways to debug:**

1. **IDE Breakpoints** - Most powerful, pause execution
   - Set breakpoint on line 117 in `RepositoryLoggingAspect.java`
   - Inspect variables when paused
   - Step through code

2. **Live Logs** - Easiest, no setup needed
   - `tail -f backend-debug.log | grep "REPO"`
   - See all queries in real-time
   - Search with grep

3. **Repository Comments** - Quick reference
   - Open any repository file
   - Read the debugging comments
   - Know where to set breakpoints

**Start with Method 2 (Logs)** - It's the easiest!

---

## 🎉 You're Ready to Debug!

**Try it now:**
```bash
# Terminal 1: Watch logs
tail -f backend-debug.log | grep "REPO"

# Terminal 2: Test login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin1234","deviceFingerprint":"test"}'

# See the magic happen! ✨
```

---

**Questions?** Check `REPOSITORY_DEBUGGING_GUIDE.md` for detailed scenarios!

**Status:** ✅ Your backend is running (PID: 81282)
**Log File:** `backend-debug.log`
**Ready to debug!** 🚀
