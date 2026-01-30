# Database Schema - CRUD Application

## Database Information
- **Type**: H2 (In-Memory)
- **JDBC URL**: jdbc:h2:mem:crud_test_db
- **Strategy**: create-drop (recreated on each restart)
- **Dialect**: H2Dialect

---

## Tables

### 1. USERS Table

**Table Name**: `users`

#### Columns:

| Column Name            | Data Type      | Constraints                    | Description                           |
|------------------------|----------------|--------------------------------|---------------------------------------|
| id                     | BIGINT         | PRIMARY KEY, AUTO_INCREMENT    | Unique user identifier                |
| username               | VARCHAR(50)    | NOT NULL, UNIQUE               | User's login name                     |
| email                  | VARCHAR(100)   | NOT NULL, UNIQUE               | User's email address                  |
| password_hash          | VARCHAR(60)    | NOT NULL                       | BCrypt hashed password                |
| role                   | VARCHAR(20)    | NOT NULL                       | User role (ROLE_USER, ROLE_ADMIN)     |
| mfa_enabled            | BOOLEAN        | NOT NULL, DEFAULT false        | Multi-factor authentication flag      |
| is_account_locked      | BOOLEAN        | NOT NULL, DEFAULT false        | Account lock status                   |
| account_locked_until   | TIMESTAMP      | NULLABLE                       | Lock expiration date                  |
| lock_reason            | VARCHAR(500)   | NULLABLE                       | Reason for account lock               |
| created_at             | TIMESTAMP      | NOT NULL                       | Account creation timestamp            |
| updated_at             | TIMESTAMP      | NOT NULL                       | Last update timestamp                 |

#### Indexes:
- PRIMARY KEY on `id`
- UNIQUE INDEX on `username`
- UNIQUE INDEX on `email`

#### Constraints:
```sql
ALTER TABLE users
  ADD CONSTRAINT UK_username UNIQUE (username);

ALTER TABLE users
  ADD CONSTRAINT UK_email UNIQUE (email);

ALTER TABLE users
  ADD CONSTRAINT CK_role CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN'));
```

---

## Current Data

### USERS Table (2 rows)

```
+----+-----------+------------------------+-------------------------------------------------------------+-----------+-------------+-------------------+----------------------+-------------+----------------------------+----------------------------+
| ID | USERNAME  | EMAIL                  | PASSWORD_HASH                                               | ROLE      | MFA_ENABLED | IS_ACCOUNT_LOCKED | ACCOUNT_LOCKED_UNTIL | LOCK_REASON | CREATED_AT                 | UPDATED_AT                 |
+----+-----------+------------------------+-------------------------------------------------------------+-----------+-------------+-------------------+----------------------+-------------+----------------------------+----------------------------+
| 1  | admin     | admin@crudtest.com     | $2a$12$vH/0MPZIKo/JcWyN3BqpaONGjAQ3F4.qHSx3iyZand3wGHU2/cAAS | ROLE_ADMIN| false       | false             | NULL                 | NULL        | 2026-01-30T16:36:40.175504 | 2026-01-30T16:36:40.175543 |
| 2  | srikanth  | srikanth@example.com   | $2a$12$XAUnpG0IE9zbHGRi1WW1XOoBlpfpKaxOLG725TPLyCGyXeZiUT2AG | ROLE_ADMIN| false       | false             | NULL                 | NULL        | 2026-01-30T16:56:07.677718 | 2026-01-30T16:56:10.005459 |
+----+-----------+------------------------+-------------------------------------------------------------+-----------+-------------+-------------------+----------------------+-------------+----------------------------+----------------------------+
```

**Summary:**
- Total Users: **2**
- Admin Users: **2** (admin, srikanth)
- Regular Users: **0**
- Locked Accounts: **0**
- MFA Enabled: **0**

---

## Entity Relationship

```
┌─────────────────────────────────┐
│          USERS                  │
├─────────────────────────────────┤
│ PK  id                          │
│ UK  username                    │
│ UK  email                       │
│     password_hash               │
│     role (ENUM)                 │
│     mfa_enabled                 │
│     is_account_locked           │
│     account_locked_until        │
│     lock_reason                 │
│     created_at                  │
│     updated_at                  │
└─────────────────────────────────┘
```

---

## Role Enum

**Location**: `org/example/entity/Role.java`

```java
public enum Role {
    ROLE_USER,    // Regular user with limited access
    ROLE_ADMIN    // Administrator with full access
}
```

---

## Password Security

- **Algorithm**: BCrypt
- **Strength**: 12 rounds
- **Format**: `$2a$12$[salt][hash]`
- **Length**: 60 characters

**Example BCrypt Hash Structure:**
```
$2a$12$XAUnpG0IE9zbHGRi1WW1XOoBlpfpKaxOLG725TPLyCGyXeZiUT2AG
 │   │   │                                                    │
 │   │   │                                                    └─ Hash (31 chars)
 │   │   └─ Salt (22 chars)
 │   └─ Cost factor (12 = 2^12 = 4,096 iterations)
 └─ Algorithm identifier (2a = BCrypt)
```

---

## SQL Queries for Reference

### View All Users
```sql
SELECT id, username, email, role, created_at
FROM users
ORDER BY created_at DESC;
```

### Count Users by Role
```sql
SELECT role, COUNT(*) as count
FROM users
GROUP BY role;
```

### Find Locked Accounts
```sql
SELECT username, email, lock_reason, account_locked_until
FROM users
WHERE is_account_locked = true;
```

### Find MFA Enabled Accounts
```sql
SELECT username, email, created_at
FROM users
WHERE mfa_enabled = true;
```

### Find Admin Users
```sql
SELECT username, email, created_at
FROM users
WHERE role = 'ROLE_ADMIN';
```

---

## Database Access

### H2 Console
- **URL**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:mem:crud_test_db`
- **Username**: `sa`
- **Password**: (leave blank)

### Via REST API
```bash
# Login to get JWT token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

# Query all users
curl -s -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

---

## Database Lifecycle

### On Application Start:
1. H2 creates in-memory database
2. Hibernate executes `create` DDL (creates tables)
3. `InitialDataLoader` runs and creates default admin user
4. Application ready to accept requests

### During Runtime:
- CRUD operations modify database
- Transactions ensure data integrity
- Hibernate manages entity lifecycle

### On Application Stop:
1. Hibernate executes `drop` DDL (destroys tables)
2. All data is lost (in-memory database)
3. Next startup creates fresh database

---

## Migration to Persistent Database

To switch from H2 in-memory to persistent PostgreSQL:

1. Update `pom.xml`:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

2. Update `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/crud_db
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

3. Create PostgreSQL database:
```sql
CREATE DATABASE crud_db;
```

---

## Notes

- **Data Persistence**: Current setup uses in-memory database. Data is lost on restart.
- **Production**: Switch to PostgreSQL/MySQL for production deployments.
- **Backups**: Not applicable for in-memory database. Implement for production.
- **Migrations**: Use Flyway or Liquibase for production schema versioning.
- **Indexes**: Current setup has indexes on username and email for performance.
- **Audit**: Consider adding audit fields (created_by, updated_by) for production.
