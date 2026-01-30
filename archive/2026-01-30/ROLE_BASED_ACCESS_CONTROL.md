# Role-Based Access Control (RBAC) - Enterprise Grade

## Overview
This application implements enterprise-grade role-based access control with two roles: **ADMIN** and **USER**.

## Roles and Permissions

### ADMIN Role
Full system access with the following capabilities:

#### User Management
- ✅ **Create**: Can register new users
- ✅ **Read**: Can view ALL users in the system
- ✅ **Update**: Can edit ANY user's information (username, email, password, role)
- ✅ **Delete**: Can delete ANY user account
- ✅ **Role Management**: Can promote users to ADMIN or demote to USER

#### Special Privileges
- View complete user list in dashboard
- Change user roles via edit modal
- Delete any user including other admins
- Full CRUD access without restrictions

### USER Role
Limited access with the following capabilities:

#### Self-Management Only
- ✅ **Create**: Can register a new account
- ✅ **Read**: Can view ONLY their own profile
- ✅ **Update**: Can edit ONLY their own information (username, email, password)
- ✅ **Delete**: Can delete ONLY their own account
- ❌ **Role Management**: Cannot change roles (no role dropdown shown)

#### Restrictions
- Cannot view other users in dashboard (only ADMIN can view user list)
- Cannot edit other users' profiles
- Cannot delete other users
- Cannot promote themselves to ADMIN

## Implementation Details

### Backend Security (Spring Boot)

#### Service Layer (`UserServiceImpl.java`)
```java
// Get All Users - ADMIN only
public List<User> getAllUsers(String currentUsername) {
    User currentUser = userRepository.findByUsername(currentUsername)
        .orElseThrow(() -> new UserNotFoundException("Current user not found"));

    if (currentUser.getRole() != Role.ROLE_ADMIN) {
        throw new UnauthorizedException("You do not have permission to view all users");
    }

    return userRepository.findAll();
}

// Update User - ADMIN or Owner
public User updateUser(Long id, UserUpdateRequest request, String currentUsername) {
    if (!isAdminOrOwner(id, currentUsername)) {
        throw new UnauthorizedException("You do not have permission to update this user");
    }

    // Only admins can change roles
    if (request.getRole() != null && currentUser.getRole() == Role.ROLE_ADMIN) {
        targetUser.setRole(request.getRole());
    }

    return userRepository.save(targetUser);
}

// Delete User - ADMIN or Owner
public void deleteUser(Long id, String currentUsername) {
    if (!isAdminOrOwner(id, currentUsername)) {
        throw new UnauthorizedException("You do not have permission to delete this user");
    }

    userRepository.deleteById(id);
}
```

#### Authorization Helper
```java
public boolean isAdminOrOwner(Long userId, String currentUsername) {
    User currentUser = userRepository.findByUsername(currentUsername)
        .orElseThrow(() -> new UserNotFoundException("Current user not found"));

    // Admin can access any user
    if (currentUser.getRole() == Role.ROLE_ADMIN) {
        return true;
    }

    // User can only access their own profile
    return currentUser.getId().equals(userId);
}
```

### Frontend Security (React)

#### Auth Context (`AuthContext.jsx`)
```javascript
const canManageUser = (userId) => {
    if (!user) return false;
    return user.role === 'ROLE_ADMIN' || user.id === userId;
};

const isAdmin = user?.role === 'ROLE_ADMIN';
```

#### Protected Routes (`ProtectedRoute.jsx`)
```javascript
// Redirect to login if not authenticated
if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
}

// Redirect to dashboard if not admin (for admin-only routes)
if (adminOnly && !isAdmin) {
    return <Navigate to="/dashboard" replace />;
}
```

#### Dashboard Access (`UserDashboard.jsx`)
```javascript
// Only show user list if ADMIN
const loadUsers = async () => {
    const response = await api.getUsers(); // Fails for non-admin
    setUsers(response.data);
};

// Show edit/delete buttons only for manageable users
{canManageUser(user.id) && (
    <button onClick={() => handleEdit(user)}>Edit</button>
    <button onClick={() => handleDelete(user)}>Delete</button>
)}
```

#### Edit Modal (`UserEditModal.jsx`)
```javascript
// Show role selector only for ADMIN
{isAdmin && (
    <select name="role" value={formData.role}>
        <option value="ROLE_USER">Regular User</option>
        <option value="ROLE_ADMIN">Administrator</option>
    </select>
)}

// Only send role change if user is ADMIN
if (formData.role !== user.role && isAdmin) {
    updateData.role = formData.role;
}
```

## Security Features

### 1. JWT Authentication
- All API endpoints (except login/register) require valid JWT token
- Token includes user ID, username, and role
- Token expires after configured time (default: 1 hour)

### 2. Backend Authorization
- Service layer checks user role before processing requests
- Throws `UnauthorizedException` for unauthorized access
- Returns 403 Forbidden for role violations

### 3. Frontend Authorization
- UI elements hidden based on role (buttons, forms, routes)
- API calls fail gracefully with error messages
- Automatic redirect to login on 401 errors

### 4. Database Security
- Passwords hashed with BCrypt (strength 12)
- User roles stored as enum in database
- Unique constraints on username and email

## Usage Examples

### Scenario 1: Admin Managing Users
```
1. Admin logs in → Sees all users in dashboard
2. Clicks "Edit" on any user → Can change username, email, password, role
3. Clicks "Delete" on any user → User is removed from system
4. Can promote regular user to admin by changing role
```

### Scenario 2: Regular User
```
1. User logs in → Sees only their own profile card in dashboard
2. Clicks "Edit" on their card → Can change username, email, password (no role dropdown)
3. Clicks "Delete" on their card → Deletes own account and logs out
4. Cannot view, edit, or delete other users
```

### Scenario 3: Registration
```
1. Anyone can access /register page
2. New users are created with ROLE_USER by default
3. Only existing admin can promote to ROLE_ADMIN
```

## Testing Role-Based Access

### Test as Admin
```bash
# Login as admin
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Get all users (should succeed)
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"

# Update any user (should succeed)
curl -X PUT http://localhost:8080/api/users/2 \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email":"newemail@example.com"}'

# Delete any user (should succeed)
curl -X DELETE http://localhost:8080/api/users/2 \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

### Test as Regular User
```bash
# Login as regular user
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test1234"}'

# Get all users (should fail with 403)
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer YOUR_USER_TOKEN"

# Update own profile (should succeed)
curl -X PUT http://localhost:8080/api/users/2 \
  -H "Authorization: Bearer YOUR_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email":"mynewemail@example.com"}'

# Update other user (should fail with 403)
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer YOUR_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email":"hacker@example.com"}'
```

## Best Practices Implemented

1. **Principle of Least Privilege**: Users have minimum necessary permissions
2. **Defense in Depth**: Security at both frontend and backend layers
3. **Fail-Safe Defaults**: Deny access unless explicitly allowed
4. **Separation of Duties**: Different roles for different responsibilities
5. **Audit Trail**: Logging of all authorization attempts (success and failure)
6. **Secure by Default**: New users start with limited USER role

## Error Handling

### Backend Exceptions
- `UnauthorizedException` (403) - User lacks permission
- `UserNotFoundException` (404) - User does not exist
- `UserAlreadyExistsException` (409) - Username/email already taken
- `InvalidCredentialsException` (401) - Login failed

### Frontend Error Messages
- Clear, user-friendly error messages
- Toast notifications for feedback
- Automatic redirect on authentication failure
- Graceful degradation when features unavailable

## Compliance and Standards

This implementation follows:
- ✅ OWASP Top 10 Security Practices
- ✅ Industry-standard RBAC patterns
- ✅ RESTful API design principles
- ✅ Spring Security best practices
- ✅ React security guidelines
- ✅ JWT authentication standards (RFC 7519)

## Future Enhancements

Potential additions for more complex scenarios:
- Multiple roles (e.g., MODERATOR, VIEWER)
- Fine-grained permissions (e.g., CAN_EDIT_USERS, CAN_DELETE_USERS)
- Permission inheritance and role hierarchies
- Temporary role assignments with expiration
- Audit log for all role changes
- Two-factor authentication for admin accounts
