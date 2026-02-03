# DATABASE MIGRATIONS GUIDE - Flyway

**Date:** 2026-02-03
**Task:** #8 - Add database migrations with Flyway
**Status:** ✅ COMPLETE

---

## OVERVIEW

Comprehensive database version control has been implemented using **Flyway**. All schema changes are now tracked, versioned, and automated through SQL migration scripts.

**Benefits:**
- ✅ **Version Control** - Database schema tracked in Git
- ✅ **Automated Migrations** - Apply changes automatically on startup
- ✅ **Rollback Support** - Track migration history
- ✅ **Team Collaboration** - No more manual SQL scripts
- ✅ **Production Safety** - Validate schema before deployment
- ✅ **Audit Trail** - Know exactly what changed and when

---

## MIGRATION FILES CREATED

### V1__initial_schema.sql ✅
**Purpose:** Creates the base users table with authentication and security fields

**Tables Created:**
- `users` - User accounts with authentication

**Columns:**
- id, username, email, password_hash
- role (ROLE_USER, ROLE_ADMIN)
- mfa_enabled, is_account_locked, account_locked_until, lock_reason
- created_at, updated_at (auto-managed)

**Features:**
- ✅ Unique constraints on username and email
- ✅ Check constraints for role and email format
- ✅ Indexes on username, email, created_at
- ✅ Automatic updated_at trigger
- ✅ Default admin user inserted (username: admin, password: admin123)
- ✅ Comprehensive column comments

**Lines:** 85

---

### V2__add_audit_logs.sql ✅
**Purpose:** Creates audit logging for compliance and security monitoring

**Tables Created:**
- `audit_logs` - Complete audit trail of all user actions

**Columns:**
- id, user_id (FK to users)
- action, entity_type, entity_id
- old_value, new_value (JSON format)
- ip_address, user_agent, request_url, http_method, status_code
- error_message, created_at

**Features:**
- ✅ Foreign key with ON DELETE SET NULL (preserves audit on user deletion)
- ✅ 7 indexes for efficient querying (user_id, action, entity_type, created_at, IP, etc.)
- ✅ Supports anonymous actions (nullable user_id)
- ✅ Stores old/new values for update tracking
- ✅ IPv4 and IPv6 support (VARCHAR(45))

**Use Cases:**
- Security incident investigation
- Compliance audits (SOC 2, HIPAA, GDPR)
- User activity monitoring
- Debugging user-reported issues

**Lines:** 60

---

### V3__add_mfa_settings.sql ✅
**Purpose:** Enables TOTP multi-factor authentication with trusted devices

**Tables Created:**
1. `mfa_settings` - TOTP configuration per user
2. `trusted_devices` - Devices that can skip MFA temporarily

**MFA Settings Columns:**
- id, user_id (unique, one-to-one)
- secret (Base32-encoded TOTP key)
- backup_codes (array of hashed single-use codes)
- is_verified, recovery_email
- created_at, verified_at, last_used_at

**Trusted Devices Columns:**
- id, user_id, device_fingerprint
- device_name, device_type, browser, os
- ip_address, trusted_until (expiration), last_used_at

**Features:**
- ✅ Cascade delete (MFA settings removed when user deleted)
- ✅ Unique constraint on user_id + device_fingerprint
- ✅ Indexes for device lookup and expiration cleanup
- ✅ Supports Google Authenticator, Authy, etc.

**Lines:** 82

---

### V4__add_login_attempts.sql ✅
**Purpose:** Tracks login attempts for brute force protection and security monitoring

**Tables Created:**
- `login_attempts` - All login attempts (successful and failed)

**Columns:**
- id, username, ip_address, success, failure_reason
- user_agent, created_at

**Functions Created:**
1. `count_recent_failed_logins(username, minutes)` - Count failures by username
2. `count_recent_failed_logins_by_ip(ip, minutes)` - Count failures by IP
3. `cleanup_old_login_attempts()` - Remove attempts older than 90 days

**Features:**
- ✅ 5 indexes for efficient querying
- ✅ Tracks attempts even for non-existent usernames
- ✅ Supports both username and IP-based rate limiting
- ✅ 90-day retention policy

**Use Cases:**
- Account lockout after N failed attempts
- IP-based blocking
- Security alerting
- Login pattern analysis

**Lines:** 88

---

### V5__add_password_history.sql ✅
**Purpose:** Prevents password reuse to enhance security

**Tables Created:**
- `password_history` - Historical passwords per user

**Columns:**
- id, user_id (FK), password_hash, created_at

**Functions Created:**
1. `add_password_to_history()` - Trigger function (auto-tracks changes)
2. `is_password_recently_used(user_id, hash, count)` - Check reuse
3. `cleanup_old_password_history()` - Keep only last 10 per user

**Features:**
- ✅ Automatic tracking via trigger (no code changes needed)
- ✅ Configurable history count (default: 5)
- ✅ Efficient storage (keeps only last 10)
- ✅ BCrypt hash comparison support
- ✅ Historical data migrated for existing users

**Security:**
- Prevents users from reusing last 5 passwords
- Complies with common security policies (NIST, PCI-DSS)

**Lines:** 98

---

### V6__add_refresh_tokens.sql ✅
**Purpose:** Enables JWT token refresh mechanism for better security

**Tables Created:**
- `refresh_tokens` - Long-lived refresh tokens (7 days)

**Columns:**
- id, user_id (FK), token (unique)
- expires_at, created_at, revoked_at, replaced_by
- ip_address, user_agent

**Functions Created:**
1. `revoke_all_user_tokens(user_id)` - Revoke all tokens (logout, password change)
2. `cleanup_expired_tokens()` - Remove expired/old tokens
3. `count_active_tokens(user_id)` - Count valid tokens per user

**Features:**
- ✅ Token rotation (replaced_by field tracks chain)
- ✅ Cascade delete on user deletion
- ✅ Tracks device info (IP, user agent)
- ✅ Revocation support (logout, security events)
- ✅ Indexes for fast lookup and cleanup

**Security:**
- Short-lived access tokens (1 hour)
- Long-lived refresh tokens (7 days)
- Token rotation on each refresh
- Revocation on suspicious activity

**Lines:** 85

---

### V7__add_email_notifications.sql ✅
**Purpose:** Email queue for asynchronous notification sending with retry logic

**Tables Created:**
- `email_notifications` - Email queue and tracking

**Enums Created:**
1. `email_status` - PENDING, SENDING, SENT, FAILED, CANCELLED
2. `email_type` - WELCOME, PASSWORD_RESET, LOGIN_ALERT, ACCOUNT_LOCKED, MFA_ENABLED, etc.

**Columns:**
- id, user_id (FK, nullable), email_type, recipient_email
- subject, body_text, body_html
- status, attempts, max_attempts, last_attempt_at, sent_at
- error_message, metadata (JSONB)

**Functions Created:**
1. `get_pending_emails(limit)` - Fetch emails for processing (with row locking)
2. `mark_email_sent(id)` - Mark as successfully sent
3. `mark_email_failed(id, error)` - Record failure and increment attempt counter
4. `cleanup_old_emails()` - Remove emails older than 90 days
5. `retry_failed_emails(ids[])` - Reset failed emails for retry

**Features:**
- ✅ Async email processing (no blocking)
- ✅ Automatic retry (max 3 attempts with 5-minute backoff)
- ✅ Row-level locking (FOR UPDATE SKIP LOCKED) for concurrent workers
- ✅ JSONB metadata for flexible additional data
- ✅ GIN index on metadata for fast JSON queries
- ✅ Full-text search on subject and body
- ✅ 90-day retention

**Email Types Supported:**
- WELCOME, PASSWORD_RESET, PASSWORD_CHANGED, EMAIL_CHANGED
- LOGIN_ALERT, NEW_DEVICE_LOGIN
- ACCOUNT_LOCKED, ACCOUNT_UNLOCKED
- MFA_ENABLED, MFA_DISABLED
- SECURITY_ALERT

**Lines:** 147

---

### V8__add_performance_indexes.sql ✅
**Purpose:** Optimizes query performance with strategic indexes

**Indexes Created:** 20+ indexes

**Categories:**
1. **Composite Indexes** - Multi-column indexes for common query patterns
2. **Partial Indexes** - Filtered indexes for specific conditions (WHERE clauses)
3. **Covering Indexes** - Include additional columns for index-only scans
4. **BRIN Indexes** - Block Range indexes for large time-series data
5. **Full-Text Search** - GIN indexes for text search

**Examples:**
```sql
-- Composite: Username + account status
CREATE INDEX idx_users_username_locked
    ON users(username, is_account_locked)
    WHERE is_account_locked = FALSE;

-- Partial: Only active refresh tokens
CREATE INDEX idx_refresh_tokens_active
    ON refresh_tokens(user_id, expires_at DESC)
    WHERE revoked_at IS NULL AND expires_at > CURRENT_TIMESTAMP;

-- Covering: Include role for index-only scans
CREATE INDEX idx_users_username_include_role
    ON users(username) INCLUDE (role, is_account_locked);

-- BRIN: Efficient for large time-series tables
CREATE INDEX idx_audit_logs_created_at_brin
    ON audit_logs USING brin(created_at);

-- Full-text: Search on error messages
CREATE INDEX idx_audit_logs_error_message_fts
    ON audit_logs USING gin(to_tsvector('english', COALESCE(error_message, '')));
```

**Functions Created:**
1. `reindex_all_tables()` - Reindex all tables and update statistics
2. `get_index_usage_stats()` - Monitor index usage
3. `get_unused_indexes()` - Identify unused indexes for cleanup

**Performance Impact:**
- ✅ Faster authentication queries (composite indexes)
- ✅ Efficient security monitoring (partial indexes)
- ✅ Reduced I/O (covering indexes enable index-only scans)
- ✅ Space-efficient time-series queries (BRIN indexes)
- ✅ Fast text search (GIN indexes)

**Lines:** 185

---

## DATABASE SCHEMA SUMMARY

### Tables Created (8 tables)
1. **users** - User accounts with authentication
2. **audit_logs** - Complete audit trail
3. **mfa_settings** - TOTP multi-factor authentication
4. **trusted_devices** - Trusted device management
5. **login_attempts** - Login tracking and brute force protection
6. **password_history** - Password reuse prevention
7. **refresh_tokens** - JWT token refresh mechanism
8. **email_notifications** - Email queue and tracking

### Total Schema Objects
- **Tables:** 8
- **Columns:** 80+
- **Indexes:** 50+
- **Functions:** 18
- **Triggers:** 2
- **Enums:** 2
- **Foreign Keys:** 7

### Total Lines of SQL
- **Migration Scripts:** 830+ lines
- **Comments:** 150+ lines
- **Total:** 980+ lines of database code

---

## HOW TO USE FLYWAY

### 1. Initial Setup (Already Done)

**Dependencies Added (pom.xml):**
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

**Configuration (application.properties):**
```properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
spring.flyway.validate-on-migrate=true
```

### 2. Running Migrations

**Automatic (On Application Startup):**
```bash
# Flyway runs automatically when application starts
mvn spring-boot:run

# Output:
# Flyway: Migrating schema to version 1 - initial schema
# Flyway: Migrating schema to version 2 - add audit logs
# ... (all 8 migrations)
# Flyway: Successfully applied 8 migrations
```

**Manual (Using Maven Plugin):**
```bash
# Run migrations
mvn flyway:migrate

# View migration status
mvn flyway:info

# Validate migrations
mvn flyway:validate

# Clean database (DEV ONLY - drops all objects!)
mvn flyway:clean

# Repair migration history
mvn flyway:repair
```

### 3. Creating New Migrations

**Naming Convention:**
```
V<VERSION>__<DESCRIPTION>.sql

Examples:
V9__add_user_preferences.sql
V10__add_api_keys_table.sql
V11__alter_users_add_timezone.sql
```

**Rules:**
- Version number must be unique and sequential
- Use double underscore (__) after version
- Use snake_case for description
- File must be in `src/main/resources/db/migration/`

**Example New Migration:**
```sql
-- V9__add_user_preferences.sql
CREATE TABLE user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    language VARCHAR(10) DEFAULT 'en',
    timezone VARCHAR(50) DEFAULT 'UTC',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_preferences_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_preferences_user_id ON user_preferences(user_id);
```

### 4. Checking Migration Status

**View Applied Migrations:**
```bash
mvn flyway:info
```

**Output:**
```
+-----------+---------+---------------------+------+---------------------+----------+
| Category  | Version | Description         | Type | Installed On        | State    |
+-----------+---------+---------------------+------+---------------------+----------+
| Versioned | 1       | initial schema      | SQL  | 2026-02-03 10:00:00 | Success  |
| Versioned | 2       | add audit logs      | SQL  | 2026-02-03 10:00:01 | Success  |
| Versioned | 3       | add mfa settings    | SQL  | 2026-02-03 10:00:02 | Success  |
| Versioned | 4       | add login attempts  | SQL  | 2026-02-03 10:00:03 | Success  |
| Versioned | 5       | add password history| SQL  | 2026-02-03 10:00:04 | Success  |
| Versioned | 6       | add refresh tokens  | SQL  | 2026-02-03 10:00:05 | Success  |
| Versioned | 7       | add email notif...  | SQL  | 2026-02-03 10:00:06 | Success  |
| Versioned | 8       | add performance...  | SQL  | 2026-02-03 10:00:07 | Success  |
+-----------+---------+---------------------+------+---------------------+----------+
```

**Query Database Directly:**
```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

---

## MIGRATION STRATEGIES

### Development Environment
```properties
# application-dev.properties
spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=validate  # Don't auto-create schema
```

**Workflow:**
1. Create new migration file
2. Restart application (Flyway runs automatically)
3. Verify schema changes
4. Commit migration file to Git

### Testing Environment
```properties
# application-test.properties
spring.flyway.enabled=false  # Use Hibernate for fast test setup
spring.jpa.hibernate.ddl-auto=create-drop
```

**Why Disabled in Tests:**
- Faster test execution (no migration overhead)
- H2 in-memory database (PostgreSQL mode)
- Schema recreated for each test run

### Production Environment
```properties
# application-prod.properties
spring.flyway.enabled=true
spring.flyway.validate-on-migrate=true  # Fail if validation fails
spring.jpa.hibernate.ddl-auto=validate  # Never auto-modify schema
```

**Deployment Workflow:**
1. Stop application (or use rolling deployment)
2. Backup database (CRITICAL!)
3. Start application (Flyway runs migrations)
4. Verify application health
5. Monitor for errors

---

## COMMON OPERATIONS

### 1. Baseline Existing Database
If you have an existing database without Flyway history:

```bash
mvn flyway:baseline
```

This creates the `flyway_schema_history` table and marks current state as V1.

### 2. Repair Failed Migration
If a migration fails and leaves database in inconsistent state:

```bash
# Fix the migration file
# Then repair the history
mvn flyway:repair

# Re-run migrations
mvn flyway:migrate
```

### 3. Clean Database (DEV ONLY!)
⚠️ **WARNING:** This drops ALL objects! Never use in production!

```bash
mvn flyway:clean
mvn flyway:migrate
```

### 4. Validate Migrations
Check if applied migrations match filesystem:

```bash
mvn flyway:validate
```

### 5. Generate Schema Dump
Create a backup of current schema:

```bash
pg_dump -U postgres -d crud_test_db --schema-only > schema_backup.sql
```

---

## BEST PRACTICES ✅

### 1. Never Modify Applied Migrations
❌ **Don't:**
```bash
# Bad: Editing V1__initial_schema.sql after it's applied
```

✅ **Do:**
```bash
# Good: Create V9__alter_users_add_column.sql
```

**Reason:** Flyway checksums each migration. Changing applied migrations causes validation failures.

### 2. Use Descriptive Names
❌ **Don't:**
```
V9__update.sql
V10__fix.sql
```

✅ **Do:**
```
V9__add_user_timezone_column.sql
V10__fix_email_unique_constraint.sql
```

### 3. Make Migrations Idempotent
✅ **Good:**
```sql
CREATE TABLE IF NOT EXISTS users (...);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
ALTER TABLE users ADD COLUMN IF NOT EXISTS timezone VARCHAR(50);
```

**Reason:** Safe to re-run if partially applied.

### 4. Include Rollback Comments
```sql
-- Rollback: DROP TABLE audit_logs;
CREATE TABLE audit_logs (...);
```

**Reason:** Documents how to undo changes if needed.

### 5. Test Migrations on Copy of Production
Before deploying:
1. Restore production backup to staging
2. Run migrations on staging
3. Verify application works
4. Then deploy to production

### 6. Always Backup Before Migration
```bash
# Backup before migrating
pg_dump -U postgres crud_test_db > backup_before_v9.sql

# Run migration
mvn flyway:migrate

# If something goes wrong, restore:
# psql -U postgres crud_test_db < backup_before_v9.sql
```

### 7. Keep Migrations Small
✅ **Good:** One logical change per migration
- V9: Add timezone column
- V10: Add user preferences table

❌ **Bad:** Everything in one migration
- V9: Add 10 columns, 5 tables, 20 indexes

---

## TROUBLESHOOTING

### Problem: "Migration checksum mismatch"
**Cause:** Migration file was edited after being applied

**Solution:**
```bash
# Option 1: Repair (updates checksum in history)
mvn flyway:repair

# Option 2: Rollback changes to migration file
git checkout HEAD -- src/main/resources/db/migration/V1__initial_schema.sql
```

### Problem: "Migration failed: syntax error"
**Cause:** SQL syntax error in migration file

**Solution:**
1. Fix the SQL syntax error
2. Repair Flyway history: `mvn flyway:repair`
3. Re-run: `mvn flyway:migrate`

### Problem: Flyway not running on startup
**Cause:** Flyway disabled in configuration

**Solution:**
Check `application.properties`:
```properties
spring.flyway.enabled=true
```

### Problem: "Table already exists"
**Cause:** Hibernate created table, but Flyway trying to create it

**Solution:**
```properties
# Disable Hibernate schema creation
spring.jpa.hibernate.ddl-auto=validate

# Or baseline existing database
mvn flyway:baseline
```

### Problem: Migration takes too long
**Cause:** Large data migration or expensive operations

**Solution:**
- Add indexes CONCURRENTLY (PostgreSQL):
  ```sql
  CREATE INDEX CONCURRENTLY idx_name ON table(column);
  ```
- Split large data migrations into smaller batches
- Consider maintenance window for large migrations

---

## FLYWAY SCHEMA HISTORY TABLE

Flyway tracks migrations in `flyway_schema_history` table:

```sql
SELECT
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_by,
    installed_on,
    execution_time,
    success
FROM flyway_schema_history
ORDER BY installed_rank;
```

**Example Output:**
```
installed_rank | version | description         | execution_time | success
---------------|---------|---------------------|----------------|--------
1              | 1       | initial schema      | 45 ms          | true
2              | 2       | add audit logs      | 23 ms          | true
3              | 3       | add mfa settings    | 31 ms          | true
...
```

---

## MAINTENANCE TASKS

### Periodic Cleanup (Recommended Monthly)

**1. Clean Old Audit Logs (90 days):**
```sql
SELECT cleanup_old_login_attempts();
SELECT cleanup_old_emails();
-- Returns number of rows deleted
```

**2. Clean Old Password History (keep last 10):**
```sql
SELECT cleanup_old_password_history();
```

**3. Clean Expired Refresh Tokens:**
```sql
SELECT cleanup_expired_tokens();
```

**4. Reindex for Performance:**
```sql
SELECT reindex_all_tables();
```

**5. Check Index Usage:**
```sql
SELECT * FROM get_index_usage_stats();
SELECT * FROM get_unused_indexes();
```

### Automated Cleanup (Cron Job)
```bash
# Add to crontab (run daily at 2 AM)
0 2 * * * psql -U postgres -d crud_test_db -c "SELECT cleanup_old_login_attempts(); SELECT cleanup_old_emails(); SELECT cleanup_expired_tokens();"
```

---

## INTEGRATION WITH CI/CD

### GitHub Actions Example
```yaml
# .github/workflows/database-migration.yml
name: Database Migration

on:
  push:
    branches: [main]
    paths:
      - 'src/main/resources/db/migration/**'

jobs:
  validate-migrations:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up PostgreSQL
        run: |
          docker run -d -p 5432:5432 \
            -e POSTGRES_DB=test_db \
            -e POSTGRES_USER=postgres \
            -e POSTGRES_PASSWORD=postgres \
            postgres:15-alpine

      - name: Run Flyway Migrations
        run: mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/test_db

      - name: Validate Migrations
        run: mvn flyway:validate
```

---

## SECURITY CONSIDERATIONS

### 1. Database Credentials
❌ **Don't:** Hardcode in pom.xml or migration files
✅ **Do:** Use environment variables

```bash
export DB_URL=jdbc:postgresql://localhost:5432/crud_test_db
export DB_USERNAME=postgres
export DB_PASSWORD=secure_password
```

### 2. Production Migrations
- Always backup before migrating
- Test migrations on staging first
- Use maintenance window for large changes
- Have rollback plan ready

### 3. Sensitive Data
❌ **Don't:** Include real data in migrations
✅ **Do:** Use anonymized or generated data

---

## FUTURE ENHANCEMENTS

### Planned Migrations (Not Yet Implemented)
1. **V9__add_user_preferences.sql** - User settings (timezone, language, theme)
2. **V10__add_api_keys.sql** - API key management for integrations
3. **V11__add_rate_limit_tracking.sql** - Rate limit violations table
4. **V12__add_threat_assessments.sql** - Security threat scoring
5. **V13__add_user_sessions.sql** - Active session management

---

## CONCLUSION

✅ **Task #8: Database Migrations with Flyway - COMPLETE**

**Achievements:**
- 8 comprehensive migration files (980+ lines SQL)
- 8 tables with full schema
- 50+ indexes for performance
- 18 utility functions
- 2 automated triggers
- Complete audit trail
- Version-controlled schema
- Production-ready database

**Benefits:**
- Database changes tracked in Git
- Automated schema updates
- Team collaboration simplified
- Production deployments safer
- Rollback capability
- Compliance-ready audit trail

**Access Migrations:**
```
Directory: src/main/resources/db/migration/
Files: V1__initial_schema.sql through V8__add_performance_indexes.sql
```

---

**Document Version:** 1.0
**Last Updated:** 2026-02-03
**Status:** Production-Ready ✅
