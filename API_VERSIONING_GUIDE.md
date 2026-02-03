# API Versioning Strategy Guide

## Overview

This application implements **URL-based API versioning** for backward compatibility and controlled evolution of the API.

## Versioning Approach

### URL-Based Versioning

All API endpoints include a version number in the URL path:

```
/api/v1/auth/login  (Version 1)
/api/v2/auth/login  (Version 2)
/api/v3/users       (Version 3 - future)
```

### Why URL-Based Versioning?

1. **Explicit and Clear:** Version is visible in the URL
2. **Easy to Cache:** Different URLs = different cache entries
3. **Simple to Test:** Easy to test multiple versions simultaneously
4. **Browser-Friendly:** Works with browser bookmarks and history
5. **Widely Adopted:** Used by major APIs (Stripe, Twitter, GitHub)

## Implementation

### @ApiVersion Annotation

Use the `@ApiVersion` annotation on controllers or methods:

```java
@RestController
@RequestMapping("/users")
@ApiVersion(1)  // Version 1
public class UserController {
    // Endpoints will be mapped to /api/v1/users/*
}

@RestController
@RequestMapping("/users")
@ApiVersion(2)  // Version 2
public class UserControllerV2 {
    // Endpoints will be mapped to /api/v2/users/*
}
```

### Method-Level Versioning

You can also version individual methods:

```java
@RestController
@RequestMapping("/users")
@ApiVersion(1)
public class UserController {

    @GetMapping
    public List<User> getAllUsers() {
        // Mapped to /api/v1/users
    }

    @GetMapping("/search")
    @ApiVersion(2)  // Override class-level version
    public PageResponse<User> searchUsers() {
        // Mapped to /api/v2/users/search
    }
}
```

## Current API Versions

### Version 1 (v1) - Current Stable

**Endpoints:**
- `POST /api/v1/auth/login` - Basic login
- `POST /api/v1/auth/register` - User registration
- `GET /api/v1/users` - List users
- `GET /api/v1/users/{id}` - Get user
- `PUT /api/v1/users/{id}` - Update user
- `DELETE /api/v1/users/{id}` - Delete user

**Features:**
- JWT authentication
- Basic CRUD operations
- Role-based access control

### Version 2 (v2) - Enhanced

**Endpoints:**
- `POST /api/v2/auth/login` - Login with automatic refresh token
- `POST /api/v2/auth/register` - Enhanced registration with validation
- `POST /api/v2/auth/refresh` - Refresh access token
- `POST /api/v2/auth/logout` - Logout with token revocation

**Enhancements:**
- Automatic refresh token issuance on login
- Improved error responses with correlation IDs
- Enhanced security headers
- Better response format with success flags

### Version 3 (v3) - Future (Planned)

**Planned Features:**
- GraphQL support
- WebSocket real-time updates
- Pagination improvements
- Advanced filtering with query DSL

## Version Lifecycle

### 1. Active Versions

- **v1:** Stable, maintained, supported
- **v2:** Latest, recommended for new integrations

### 2. Deprecated Versions

When deprecating a version:

```java
@ApiVersion(value = 1, deprecated = true,
            deprecationMessage = "Use v2 instead. v1 will be removed on 2027-01-01")
```

### 3. Sunset Period

- **Announcement:** 6 months before removal
- **Warning Headers:** Add deprecation warnings to responses
- **Migration Guide:** Provide detailed migration documentation
- **Support:** Continue bug fixes during sunset period

## Best Practices

### 1. Breaking Changes Require New Version

Create a new version when:
- Removing fields from responses
- Changing field types
- Changing authentication mechanism
- Modifying error response format
- Changing HTTP status codes

### 2. Non-Breaking Changes Don't Require New Version

Safe changes in existing version:
- Adding new optional fields
- Adding new endpoints
- Adding new query parameters (optional)
- Improving error messages
- Performance optimizations

### 3. Version Naming

- **Major Version:** Use for breaking changes (v1 → v2)
- **No Minor Versions:** Keep it simple with integers
- **Internal Versioning:** Use Git tags/branches for internal versions

### 4. Documentation

- Document each version separately in Swagger/OpenAPI
- Provide migration guides between versions
- Include changelog for each version

## Migration Example: V1 to V2

### V1 Login Response
```json
{
  "token": "eyJhbGci...",
  "type": "Bearer",
  "id": 1,
  "username": "admin",
  "email": "admin@example.com",
  "role": "ROLE_ADMIN"
}
```

### V2 Login Response
```json
{
  "token": "eyJhbGci...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "type": "Bearer",
  "id": 1,
  "username": "admin",
  "email": "admin@example.com",
  "role": "ROLE_ADMIN"
}
```

**Key Change:** V2 automatically includes `refreshToken` field

## Testing Multiple Versions

```bash
# Test V1 endpoint
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Test V2 endpoint (with refresh token)
curl -X POST http://localhost:8080/api/v2/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

## Swagger/OpenAPI Integration

Each version appears as a separate tag in Swagger UI:

- **Authentication V1** - Basic authentication
- **Authentication V2** - Enhanced authentication with refresh tokens
- **User Management V1** - Basic user operations
- **User Management V2** - Advanced user operations with pagination

Access Swagger UI: `http://localhost:8080/swagger-ui.html`

## Header-Based Versioning (Alternative)

If needed, we can also support header-based versioning:

```bash
curl -H "API-Version: 2" http://localhost:8080/api/auth/login
```

## Content Negotiation (Alternative)

Using Accept header for versioning:

```bash
curl -H "Accept: application/vnd.myapi.v2+json" \
  http://localhost:8080/api/auth/login
```

## Deprecation Workflow

### Step 1: Announce Deprecation (T-180 days)
- Add deprecation notice to API documentation
- Send email to API consumers
- Log deprecation warnings

### Step 2: Add Warning Headers (T-90 days)
```http
Deprecation: true
Sunset: 2027-01-01
Link: <https://api.example.com/docs/migration/v1-to-v2>; rel="deprecation"
```

### Step 3: Final Warning (T-30 days)
- Send final reminder emails
- Increase logging of deprecated version usage

### Step 4: Remove Version (T-0)
- Remove deprecated version from codebase
- Return 410 Gone for old endpoints
- Redirect to migration guide

## Error Handling Across Versions

### V1 Error Response
```json
{
  "timestamp": "2026-02-03T10:15:30",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid username or password",
  "path": "/api/v1/auth/login"
}
```

### V2 Error Response (Enhanced)
```json
{
  "success": false,
  "timestamp": "2026-02-03T10:15:30",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid username or password",
  "path": "/api/v2/auth/login",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "details": []
}
```

## Monitoring Version Usage

Track metrics for each version:

```java
// In CustomMetrics.java
public void recordApiVersionUsage(int version, String endpoint) {
    Counter.builder("api.version.usage")
            .tag("version", "v" + version)
            .tag("endpoint", endpoint)
            .register(meterRegistry)
            .increment();
}
```

View in Prometheus:
```promql
api_version_usage{version="v1"}
api_version_usage{version="v2"}
```

## Version Configuration

Configure version settings in `application.properties`:

```properties
# API Versioning
api.version.current=2
api.version.minimum-supported=1
api.version.deprecated=
api.version.sunset-date=

# Version Specific Settings
api.v1.enabled=true
api.v2.enabled=true
```

## Summary

- **Current Stable:** v1 (all core features)
- **Latest:** v2 (enhanced authentication with refresh tokens)
- **Recommended:** Use v2 for new integrations
- **Support:** Both v1 and v2 are fully supported
- **Migration:** Straightforward path from v1 to v2

---

**Last Updated:** 2026-02-03
**Current Version:** 2
**Supported Versions:** 1, 2
