# Full-Stack CRUD Application with JWT Authentication

A simplified full-stack application with Spring Boot backend and React frontend, focusing on core CRUD operations with JWT authentication.

## Features

- User registration and login
- JWT-based authentication
- Full CRUD operations for user management
- Role-based access control (ADMIN/USER)
- Protected routes on frontend
- Secure password hashing with BCrypt
- Input validation
- React dashboard with user management
- Toast notifications for user feedback

## Architecture

**Full-Stack Layered Architecture**

### Backend Structure
```
src/main/java/org/example/
├── CrudTestApplication.java (Main app)
├── controller/
│   ├── AuthController.java (Login/Authentication)
│   └── UserController.java (User CRUD operations)
├── service/
│   ├── UserService.java (Interface)
│   └── UserServiceImpl.java (Business logic)
├── entity/User.java (JPA entity with roles)
├── repository/UserRepository.java (Data access)
├── dto/
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   └── UserUpdateRequest.java
├── security/JwtUtil.java (JWT token handling)
├── config/
│   ├── SecurityConfig.java (Spring Security config)
│   ├── CorsConfig.java (CORS configuration)
│   └── InitialDataLoader.java (Sample data)
└── exception/
    ├── GlobalExceptionHandler.java
    └── Custom exceptions
```

### Frontend Structure
```
frontend/src/
├── components/
│   ├── LoginForm.jsx (Login page)
│   ├── RegistrationForm.jsx (Registration page)
│   ├── UserDashboard.jsx (Main dashboard)
│   ├── UserList.jsx (User list component)
│   ├── UserEditModal.jsx (Edit user modal)
│   └── ProtectedRoute.jsx (Route protection)
├── contexts/
│   └── AuthContext.jsx (Authentication state)
├── services/
│   └── api.js (API service layer)
└── App.jsx (Main app component)
```

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Node.js 16+ and npm
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

### Backend Setup

1. Clean and build the project:

```bash
mvn clean install
```

2. Run the backend application:

```bash
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

### Frontend Setup

1. Navigate to the frontend directory:

```bash
cd frontend
```

2. Install dependencies:

```bash
npm install
```

3. Start the development server:

```bash
npm run dev
```

The frontend will start on `http://localhost:5173`

## API Endpoints

### Authentication

#### Register User
**Endpoint**: `POST /api/auth/register`

**Request Body**:
```json
{
  "username": "johndoe",
  "email": "john.doe@example.com",
  "password": "SecurePass123"
}
```

**Success Response** (HTTP 200):
```json
{
  "message": "User registered successfully"
}
```

#### Login
**Endpoint**: `POST /api/auth/login`

**Request Body**:
```json
{
  "username": "johndoe",
  "password": "SecurePass123"
}
```

**Success Response** (HTTP 200):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "johndoe",
  "role": "USER"
}
```

### User Management (Protected - Requires JWT Token)

#### Get All Users
**Endpoint**: `GET /api/users`

**Headers**:
```
Authorization: Bearer {jwt_token}
```

**Success Response** (HTTP 200):
```json
[
  {
    "id": 1,
    "username": "johndoe",
    "email": "john.doe@example.com",
    "role": "USER",
    "createdAt": "2026-01-28T15:30:00"
  }
]
```

#### Get User by ID
**Endpoint**: `GET /api/users/{id}`

**Headers**:
```
Authorization: Bearer {jwt_token}
```

#### Update User
**Endpoint**: `PUT /api/users/{id}`

**Headers**:
```
Authorization: Bearer {jwt_token}
```

**Request Body**:
```json
{
  "username": "johndoe_updated",
  "email": "john.updated@example.com"
}
```

#### Delete User
**Endpoint**: `DELETE /api/users/{id}`

**Headers**:
```
Authorization: Bearer {jwt_token}
```

**Note**: Only ADMIN users can delete other users.

## Using the Application

### Frontend Usage

1. Open your browser and navigate to `http://localhost:5173`
2. You'll be redirected to the login page
3. Click "Register" to create a new account
4. After registration, login with your credentials
5. You'll see the User Dashboard with a list of all users
6. Admin users can edit and delete other users

### Testing with cURL

#### Test Registration

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john.doe@example.com",
    "password": "SecurePass123"
  }'
```

#### Test Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "SecurePass123"
  }'
```

Save the returned token for authenticated requests.

#### Test Get All Users

```bash
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Test Update User

```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe_updated",
    "email": "john.updated@example.com"
  }'
```

#### Test Delete User

```bash
curl -X DELETE http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

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

### Backend
- Spring Boot 3.2.2
- Spring Security with JWT
- Spring Data JPA
- Spring Validation
- BCrypt password encryption
- PostgreSQL database
- Lombok
- Maven

### Frontend
- React 18
- Vite
- React Router DOM
- Axios
- React Toastify
- CSS3 for styling

## Default Users

The application comes with a default admin user (via InitialDataLoader):

- **Username**: admin
- **Password**: admin123
- **Role**: ADMIN

You can use this account to test admin functionality like deleting users.

## Project Structure

This is a simplified version focusing on core CRUD operations. Advanced features (MFA, Threat Intelligence, Audit Logs, etc.) have been moved to the `archive/` directory.

See `SIMPLIFICATION_SUMMARY.md` for details on what was kept and what was archived.

## Learning Path

This application is perfect for learning:

1. **Spring Boot Basics**: REST API development, dependency injection, layered architecture
2. **Spring Security**: JWT authentication, password encoding, role-based access control
3. **Spring Data JPA**: Entity relationships, repositories, queries
4. **React Fundamentals**: Components, hooks, state management, routing
5. **Full-Stack Integration**: Connecting React frontend with Spring Boot backend
6. **CRUD Operations**: Complete implementation of Create, Read, Update, Delete

## Next Steps

Once you understand this basic CRUD application, you can:

1. Add more complex features from the archive
2. Implement advanced validation
3. Add pagination and sorting
4. Create more complex entity relationships
5. Add file upload functionality
6. Implement search and filtering
7. Add email verification
8. Create a more sophisticated UI with a component library
