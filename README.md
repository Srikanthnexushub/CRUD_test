# Spring Boot User Registration API

A production-ready Spring Boot REST API with user registration functionality, PostgreSQL database, and secure password storage using BCrypt.

## Features

- RESTful user registration endpoint
- Secure password hashing with BCrypt (strength 12)
- Input validation (email format, password strength, username pattern)
- Duplicate prevention (username and email uniqueness)
- Centralized exception handling
- Proper HTTP status codes (201, 400, 409, 500)
- Transaction management
- Audit timestamps (createdAt, updatedAt)

## Architecture

**Layered Architecture**: Controller → Service → Repository → Entity → Database

```
org.example/
├── CrudTestApplication.java (Main app)
├── config/SecurityConfig.java (Password encoder)
├── dto/UserRegistrationRequest.java (Input validation)
├── dto/UserRegistrationResponse.java (API response)
├── entity/User.java (JPA entity)
├── repository/UserRepository.java (Data access)
├── service/UserService.java (Interface)
├── service/UserServiceImpl.java (Business logic)
├── controller/UserController.java (REST endpoint)
└── exception/
    ├── GlobalExceptionHandler.java
    ├── UserAlreadyExistsException.java
    └── ErrorResponse.java
```

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL database server

## Database Setup

1. Install PostgreSQL if not already installed
2. Create the database:

```sql
CREATE DATABASE crud_test_db;
```

3. Update database credentials in `src/main/resources/application.properties`:

```properties
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## Build and Run

1. Clean and build the project:

```bash
mvn clean install
```

2. Run the application:

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Register User

**Endpoint**: `POST /api/users/register`

**Request Body**:
```json
{
  "username": "johndoe",
  "email": "john.doe@example.com",
  "password": "SecurePass123"
}
```

**Validation Rules**:
- **Username**: 3-50 characters, alphanumeric + underscore only
- **Email**: Valid email format, max 100 characters
- **Password**: Minimum 8 characters, must contain uppercase, lowercase, and digit

**Success Response** (HTTP 201):
```json
{
  "id": 1,
  "username": "johndoe",
  "email": "john.doe@example.com",
  "createdAt": "2026-01-28T15:30:00",
  "message": "User registered successfully"
}
```

**Error Responses**:

- **HTTP 400 (Validation Failed)**:
```json
{
  "timestamp": "2026-01-28T15:30:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation error",
  "details": [
    "password: Password must contain at least one uppercase letter, one lowercase letter, and one digit"
  ]
}
```

- **HTTP 409 (Conflict)**:
```json
{
  "timestamp": "2026-01-28T15:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Username already exists"
}
```

## Testing with cURL

### Test 1: Successful Registration

```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john.doe@example.com",
    "password": "SecurePass123"
  }'
```

Expected: HTTP 201, user details returned

### Test 2: Duplicate Username

```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "different@example.com",
    "password": "SecurePass123"
  }'
```

Expected: HTTP 409, error message "Username already exists"

### Test 3: Invalid Email

```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "janedoe",
    "email": "invalid-email",
    "password": "SecurePass123"
  }'
```

Expected: HTTP 400, validation error "Invalid email format"

### Test 4: Weak Password

```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "weak"
  }'
```

Expected: HTTP 400, password validation errors

## Database Verification

Connect to PostgreSQL and verify:

```sql
SELECT id, username, email, password_hash, created_at FROM users;
```

The password_hash should start with `$2a$` or `$2b$` (BCrypt format).

## Configuration

Edit `src/main/resources/application.properties` to customize:

- Server port
- Database connection details
- JPA/Hibernate settings
- Logging levels

## Security Considerations

- Passwords are hashed with BCrypt (never stored in plain text)
- DTO separation prevents entity exposure
- Database-level unique constraints on username and email
- Input validation at multiple layers
- No sensitive data in API responses or logs

## Technology Stack

- Spring Boot 3.2.2
- Spring Data JPA
- Spring Validation
- Spring Security Crypto (BCrypt)
- PostgreSQL
- Lombok
- Maven
