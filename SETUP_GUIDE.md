# Quick Setup Guide

## 1. Database Setup (Required)

### Install PostgreSQL

**macOS** (using Homebrew):
```bash
brew install postgresql@15
brew services start postgresql@15
```

**Windows**:
Download installer from https://www.postgresql.org/download/windows/

**Linux** (Ubuntu/Debian):
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
```

### Create Database

```bash
# Login to PostgreSQL (default user is 'postgres')
psql -U postgres

# In PostgreSQL prompt, create database
CREATE DATABASE crud_test_db;

# Exit
\q
```

### Update Database Credentials

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD
```

## 2. Build Project

```bash
# Navigate to project directory
cd /Users/ainexusstudio/Documents/GitHub/CRUD_test

# Clean and install dependencies
mvn clean install
```

## 3. Run Application

```bash
mvn spring-boot:run
```

Wait for the message: `Started CrudTestApplication in X seconds`

## 4. Test the API

Open a new terminal and run:

```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Password123"
  }'
```

Expected response (HTTP 201):
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "createdAt": "2026-01-28T...",
  "message": "User registered successfully"
}
```

## 5. Verify Database

```bash
psql -U postgres -d crud_test_db

# List tables
\dt

# View users
SELECT * FROM users;

# Exit
\q
```

## Common Issues

### Issue: "Connection refused" error

**Solution**: Make sure PostgreSQL is running
```bash
# macOS
brew services start postgresql@15

# Linux
sudo systemctl start postgresql
```

### Issue: "Authentication failed for user"

**Solution**: Update credentials in `application.properties` or reset PostgreSQL password
```bash
psql -U postgres
ALTER USER postgres PASSWORD 'newpassword';
```

### Issue: "Port 8080 already in use"

**Solution**: Change port in `application.properties`
```properties
server.port=8081
```

### Issue: Maven build fails

**Solution**: Ensure Java 17+ is installed
```bash
java -version
# Should show version 17 or higher
```

## Next Steps

1. Test duplicate username/email validation
2. Test password validation rules
3. Test invalid email format
4. View application logs for debugging
5. Explore additional features (login, update profile, etc.)

## Useful Commands

```bash
# Package as JAR
mvn package

# Run JAR directly
java -jar target/CRUD_test-1.0-SNAPSHOT.jar

# View logs in real-time
tail -f logs/spring.log  # if logging to file

# Stop application
Ctrl + C
```
