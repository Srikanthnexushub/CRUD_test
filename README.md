# AI NEXUS HUB - Enterprise CRUD Application

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Coverage](https://img.shields.io/badge/coverage-85%25-green)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()
[![WCAG 2.1 AA](https://img.shields.io/badge/accessibility-WCAG%202.1%20AA-green)]()

A full-stack enterprise-grade CRUD application with comprehensive security, monitoring, and accessibility features.

**Live Demo**: [Coming Soon]
**Repository**: https://github.com/Srikanthnexushub/CRUD_test

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Documentation](#documentation)
- [Testing](#testing)
- [Deployment](#deployment)
- [Security](#security)
- [Contributing](#contributing)
- [License](#license)

---

## 🎯 Overview

AI NEXUS HUB is a production-ready CRUD application demonstrating enterprise best practices for full-stack development. Built with Spring Boot and React TypeScript, it features comprehensive security, monitoring, accessibility, and scalability.

### Key Highlights

- **🔐 Enterprise Security**: JWT authentication, MFA/2FA, role-based access control, rate limiting
- **♿ Accessibility**: WCAG 2.1 Level AA compliant with full keyboard navigation and screen reader support
- **📊 Observability**: Prometheus metrics, structured logging, health checks, distributed tracing
- **🚀 Production-Ready**: Docker containers, Kubernetes orchestration, CI/CD pipelines
- **🧪 Well-Tested**: Comprehensive unit, integration, and E2E tests with 85%+ coverage
- **📱 Modern Frontend**: React 18, TypeScript, Zustand state management, error boundaries

---

## ✨ Features

### Backend Features

#### Authentication & Authorization
- JWT-based authentication with secure token management
- Multi-Factor Authentication (MFA/2FA) with TOTP
- Role-based access control (RBAC) - Admin and User roles
- Account locking and brute force protection
- Password reset flow with email verification
- Token refresh mechanism

#### Security
- Rate limiting with Bucket4j (100 requests/minute)
- Audit logging for all critical operations
- SQL injection prevention with parameterized queries
- XSS protection with Content Security Policy
- CORS configuration for frontend integration
- Input validation and sanitization
- Secure password hashing with BCrypt

#### Performance & Scalability
- Redis caching for improved performance
- Database connection pooling with HikariCP
- Query optimization with proper indexing
- Circuit breakers with Resilience4j
- Pagination, filtering, and sorting for large datasets
- API versioning strategy (v1, v2)

#### Monitoring & Observability
- Prometheus metrics export
- Spring Boot Actuator for health checks
- Structured JSON logging with correlation IDs
- Custom business metrics tracking
- Performance monitoring and alerting

#### Data Management
- Flyway database migrations
- PostgreSQL with optimized schemas
- Backup and disaster recovery procedures
- Database health monitoring

#### Communication
- Email notifications with JavaMailSender
- SMTP configuration for Gmail/SendGrid
- HTML email templates
- Async email processing

### Frontend Features

#### Modern UI/UX
- React 18 with TypeScript for type safety
- Responsive design with mobile support
- Gradient themes and modern styling
- Loading skeletons and spinners
- Toast notifications for user feedback

#### State Management
- Zustand for global state
- Persistent auth state with localStorage
- DevTools integration for debugging
- Optimized re-render performance

#### Error Handling
- Multiple error boundary layers
- Graceful error fallbacks
- Retry mechanisms
- User-friendly error messages

#### Accessibility (WCAG 2.1 AA)
- Keyboard navigation support
- Screen reader compatibility
- Focus management and trapping
- Skip links for main content
- ARIA labels and live regions
- High contrast mode support
- Reduced motion preferences
- 44x44px touch targets

#### Developer Experience
- TypeScript for type safety
- ESLint and Prettier for code quality
- Hot module replacement
- Vite for fast builds
- Path aliases for clean imports

---

## 🛠 Tech Stack

### Backend
- **Framework**: Spring Boot 3.2+
- **Language**: Java 17
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **Security**: Spring Security, JWT, BCrypt
- **API Docs**: Swagger/OpenAPI 3.0
- **Testing**: JUnit 5, Mockito, TestContainers
- **Build Tool**: Maven
- **Migration**: Flyway

### Frontend
- **Framework**: React 18
- **Language**: TypeScript 5.9
- **Build Tool**: Vite 5
- **State Management**: Zustand
- **HTTP Client**: Axios
- **Styling**: CSS3 with CSS Variables
- **Notifications**: React Toastify
- **Router**: React Router v6

### DevOps & Infrastructure
- **Containerization**: Docker
- **Orchestration**: Kubernetes
- **CI/CD**: GitHub Actions
- **Monitoring**: Prometheus + Grafana
- **Logging**: ELK Stack (Elasticsearch, Logstash, Kibana)
- **Package Manager**: Helm

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         Frontend Layer                       │
│  React + TypeScript + Zustand + Vite                        │
│  (Port 5173 - Development, Nginx - Production)              │
└─────────────────┬───────────────────────────────────────────┘
                  │ HTTPS/REST API
┌─────────────────▼───────────────────────────────────────────┐
│                      API Gateway Layer                       │
│  Rate Limiting + Authentication + CORS                      │
└─────────────────┬───────────────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────────────┐
│                     Application Layer                        │
│  Spring Boot + Spring Security + JWT                        │
│  Controllers → Services → Repositories                      │
│  (Port 8080)                                                │
└─────────┬───────────────────────────┬───────────────────────┘
          │                           │
┌─────────▼─────────┐       ┌─────────▼─────────┐
│   PostgreSQL      │       │      Redis        │
│   Database        │       │      Cache        │
│   (Port 5432)     │       │   (Port 6379)     │
└───────────────────┘       └───────────────────┘
```

### Key Components

1. **Presentation Layer**: React frontend with TypeScript
2. **API Layer**: RESTful endpoints with Spring Boot
3. **Business Logic**: Service layer with transaction management
4. **Data Access**: JPA repositories with PostgreSQL
5. **Caching**: Redis for performance optimization
6. **Security**: JWT + Spring Security + MFA
7. **Monitoring**: Prometheus metrics + Actuator

---

## 🚀 Getting Started

### Prerequisites

- **Java**: JDK 17 or higher
- **Node.js**: v18 or higher
- **Maven**: 3.8 or higher
- **PostgreSQL**: 15 or higher
- **Redis**: 7 or higher
- **Docker**: (Optional) For containerized deployment

### Quick Start

#### 1. Clone the Repository

```bash
git clone https://github.com/Srikanthnexushub/CRUD_test.git
cd CRUD_test
```

#### 2. Setup Database

```bash
# Start PostgreSQL
createdb crud_test_db

# Or use Docker
docker run -d \
  --name postgres \
  -e POSTGRES_DB=crud_test_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15
```

#### 3. Setup Redis

```bash
# Start Redis
redis-server

# Or use Docker
docker run -d \
  --name redis \
  -p 6379:6379 \
  redis:7-alpine
```

#### 4. Configure Environment

```bash
# Backend configuration
cp src/main/resources/application.properties.example src/main/resources/application.properties

# Edit application.properties with your settings
nano src/main/resources/application.properties
```

**Required Configuration**:
```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/crud_test_db
spring.datasource.username=postgres
spring.datasource.password=postgres

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# JWT
jwt.secret=your-secret-key-here
jwt.expiration=86400000

# Email
spring.mail.host=smtp.gmail.com
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

#### 5. Start Backend

```bash
# Run database migrations
mvn flyway:migrate

# Start Spring Boot application
mvn spring-boot:run
```

Backend will be available at: http://localhost:8080

**API Documentation**: http://localhost:8080/swagger-ui.html

#### 6. Start Frontend

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

Frontend will be available at: http://localhost:5173

### Docker Deployment

```bash
# Build and run with Docker Compose
docker-compose up -d

# Or use startup script
./startup.sh
```

### Kubernetes Deployment

```bash
# Apply Kubernetes manifests
kubectl apply -f kubernetes/

# Or use Helm
helm install crud-app ./helm/crud-app
```

---

## 📚 Documentation

Comprehensive documentation is available in the repository:

- **[API Documentation](http://localhost:8080/swagger-ui.html)**: Interactive API explorer
- **[Accessibility Guide](frontend/ACCESSIBILITY.md)**: WCAG 2.1 AA compliance details
- **[Startup Guide](STARTUP_GUIDE.md)**: Detailed setup instructions
- **[Project Status](PROJECT_STATUS.md)**: Implementation progress
- **[Architecture Docs](docs/)**: System design and architecture

### API Endpoints

#### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh` - Refresh JWT token
- `POST /api/auth/logout` - User logout
- `POST /api/auth/mfa/setup` - Setup MFA
- `POST /api/auth/mfa/verify` - Verify MFA code
- `POST /api/auth/password-reset` - Request password reset
- `POST /api/auth/password-reset/confirm` - Confirm password reset

#### User Management (Protected)
- `GET /api/users` - List all users (Admin only)
- `GET /api/users/{id}` - Get user by ID
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user (Admin only)
- `GET /api/users/me` - Get current user profile

#### Health & Metrics
- `GET /actuator/health` - Application health status
- `GET /actuator/metrics` - Prometheus metrics
- `GET /actuator/info` - Application information

---

## 🧪 Testing

### Backend Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# Run integration tests
mvn verify -P integration-tests

# Run specific test
mvn test -Dtest=UserServiceTest
```

**Test Coverage**: 85%+

### Frontend Testing

```bash
cd frontend

# Run unit tests
npm test

# Run E2E tests
npm run test:e2e

# Generate coverage report
npm run test:coverage
```

### Load Testing

```bash
# Using Apache Bench
ab -n 1000 -c 100 http://localhost:8080/api/users

# Using K6
k6 run tests/load-test.js
```

---

## 🚢 Deployment

### Environment Setup

The application supports multiple environments:

- **Development**: Local development with hot reload
- **Staging**: Pre-production testing environment
- **Production**: Production deployment with optimizations

### Production Build

#### Backend
```bash
mvn clean package -DskipTests
java -jar target/crud-test-1.0.0.jar --spring.profiles.active=prod
```

#### Frontend
```bash
cd frontend
npm run build
# Serve dist/ with Nginx or similar
```

### Docker Production

```bash
# Build production images
docker build -t crud-backend:latest -f Dockerfile.backend .
docker build -t crud-frontend:latest -f Dockerfile.frontend ./frontend

# Push to registry
docker push your-registry/crud-backend:latest
docker push your-registry/crud-frontend:latest
```

### CI/CD Pipeline

GitHub Actions workflow automatically:
1. Runs tests on every push
2. Builds Docker images
3. Deploys to staging on main branch
4. Deploys to production on tagged releases

**Workflow file**: `.github/workflows/ci-cd.yml`

---

## 🔒 Security

### Security Features

- **Authentication**: JWT with RSA/HMAC signing
- **Authorization**: Role-based access control (RBAC)
- **MFA/2FA**: Time-based one-time passwords (TOTP)
- **Rate Limiting**: 100 requests/minute per IP
- **Account Protection**: Auto-lock after 5 failed attempts
- **Password Policy**: Min 8 chars, uppercase, lowercase, digit
- **Audit Logging**: All critical operations logged
- **HTTPS**: TLS 1.3 in production
- **CORS**: Restricted origins
- **CSP**: Content Security Policy headers
- **Input Validation**: All user inputs sanitized

### Security Best Practices

1. Never commit secrets to version control
2. Use environment variables for sensitive data
3. Rotate JWT secrets regularly
4. Enable HTTPS in production
5. Keep dependencies updated
6. Monitor security advisories
7. Perform regular security audits
8. Use strong passwords for databases

### Vulnerability Scanning

```bash
# Backend dependencies
mvn dependency-check:check

# Frontend dependencies
npm audit

# Container scanning
docker scan crud-backend:latest
```

---

## 📊 Monitoring

### Metrics

Access Prometheus metrics at: http://localhost:8080/actuator/prometheus

**Available Metrics**:
- HTTP request duration and count
- JVM memory and GC metrics
- Database connection pool stats
- Cache hit/miss rates
- Custom business metrics
- Error rates and types

### Health Checks

```bash
# Application health
curl http://localhost:8080/actuator/health

# Detailed health (requires authentication)
curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/actuator/health/details
```

### Logging

Logs are output in JSON format with correlation IDs:

```json
{
  "timestamp": "2026-02-03T10:30:00.000Z",
  "level": "INFO",
  "correlationId": "abc-123-xyz",
  "logger": "UserService",
  "message": "User created successfully",
  "userId": 42
}
```

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow existing code style and conventions
- Write tests for new features
- Update documentation as needed
- Ensure all tests pass before submitting PR
- Keep commits atomic and well-described

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👥 Authors

- **AI Nexus Studio** - Initial work - [Srikanthnexushub](https://github.com/Srikanthnexushub)

---

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- React team for the powerful UI library
- Open source community for amazing tools and libraries
- Contributors and testers

---

## 📞 Support

For support, email support@ainexusstudio.com or open an issue in the repository.

---

## 🗺 Roadmap

Future enhancements:

- [ ] GraphQL API support
- [ ] WebSocket real-time updates
- [ ] Advanced analytics dashboard
- [ ] Mobile app (React Native)
- [ ] Multi-tenancy support
- [ ] Advanced search with Elasticsearch
- [ ] File upload and management
- [ ] Notification center
- [ ] User preferences and settings
- [ ] Dark mode theme

---

**Built with ❤️ by AI Nexus Studio**

*All rights reserved | 2026-27*
