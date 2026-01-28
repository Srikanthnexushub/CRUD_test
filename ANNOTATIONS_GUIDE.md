# Spring Annotations & Components Guide

## What are Annotations?

Annotations are **metadata** (extra information) added to Java code using the `@` symbol. They tell Spring and other frameworks how to handle your classes.

**Format**: `@AnnotationName` or `@AnnotationName(parameters)`

## How to Identify Components

### Rule: A class is a Spring Component if it has ANY of these annotations:

1. `@Component` - Generic component
2. `@Service` - Business logic layer
3. `@Repository` - Data access layer
4. `@Controller` - Web controller
5. `@RestController` - REST API controller
6. `@Configuration` - Configuration class
7. `@RestControllerAdvice` - Global exception handler

## Annotations in Your Project

### 1. Main Application - CrudTestApplication.java

```java
@SpringBootApplication  // ← COMPONENT (combines @Configuration + @EnableAutoConfiguration + @ComponentScan)
public class CrudTestApplication {
    // This tells Spring: "This is the main application entry point"
}
```

**What it does**: Enables Spring Boot auto-configuration and component scanning

---

### 2. Configuration - SecurityConfig.java

```java
@Configuration  // ← COMPONENT (configuration class)
public class SecurityConfig {

    @Bean  // ← Method annotation: Creates a managed bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

**What it does**:
- `@Configuration` - Tells Spring this class contains bean definitions
- `@Bean` - Creates a PasswordEncoder object managed by Spring

---

### 3. Controller - UserController.java

```java
@RestController  // ← COMPONENT (REST API controller)
@RequestMapping("/api/users")  // ← Base URL path
@RequiredArgsConstructor  // ← Lombok: Auto-generates constructor
@Slf4j  // ← Lombok: Auto-generates logger
public class UserController {

    @PostMapping("/register")  // ← HTTP POST endpoint
    public ResponseEntity<UserRegistrationResponse> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {
        // @Valid - Validates the request
        // @RequestBody - Reads JSON from request body
    }
}
```

**What it does**:
- `@RestController` - Marks as REST API controller (Spring component)
- `@RequestMapping` - Sets base URL path
- `@PostMapping` - Handles POST requests
- `@Valid` - Triggers validation
- `@RequestBody` - Converts JSON to Java object

---

### 4. Service - UserServiceImpl.java

```java
@Service  // ← COMPONENT (business logic layer)
@RequiredArgsConstructor  // ← Lombok: Auto-generates constructor for final fields
@Slf4j  // ← Lombok: Auto-generates logger
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;  // Injected by Spring
    private final PasswordEncoder passwordEncoder;  // Injected by Spring

    @Override
    @Transactional  // ← Database transaction boundary
    public UserRegistrationResponse registerUser(UserRegistrationRequest request) {
        // Business logic here
    }
}
```

**What it does**:
- `@Service` - Marks as service layer component
- `@Transactional` - Wraps method in database transaction
- `@RequiredArgsConstructor` - Creates constructor for dependency injection

---

### 5. Repository - UserRepository.java

```java
@Repository  // ← COMPONENT (data access layer)
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

**What it does**:
- `@Repository` - Marks as data access component
- Spring automatically implements these methods at runtime!

---

### 6. Exception Handler - GlobalExceptionHandler.java

```java
@RestControllerAdvice  // ← COMPONENT (global exception handler)
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)  // ← Handles specific exception
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(
            UserAlreadyExistsException ex) {
        // Handle exception and return error response
    }
}
```

**What it does**:
- `@RestControllerAdvice` - Global exception handler for all controllers
- `@ExceptionHandler` - Catches specific exception types

---

### 7. Entity - User.java

```java
@Entity  // ← NOT a Spring component, but a JPA entity (database table mapping)
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "username"),
    @UniqueConstraint(columnNames = "email")
})
@Data  // ← Lombok: Auto-generates getters, setters, toString, equals, hashCode
@NoArgsConstructor  // ← Lombok: Generates no-argument constructor
@AllArgsConstructor  // ← Lombok: Generates all-arguments constructor
public class User {

    @Id  // ← Primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // ← Auto-increment
    private Long id;

    @Column(nullable = false, unique = true, length = 50)  // ← Database column
    private String username;

    @CreationTimestamp  // ← Auto-set creation time
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

**What it does**:
- `@Entity` - Maps class to database table (NOT a Spring component)
- `@Table` - Specifies table name and constraints
- `@Id` - Marks primary key field
- `@Column` - Defines column properties
- Lombok annotations - Reduce boilerplate code

---

### 8. DTO - UserRegistrationRequest.java

```java
@Data  // ← Lombok only (NOT a Spring component)
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {

    @NotBlank(message = "Username is required")  // ← Validation
    @Size(min = 3, max = 50, message = "...")  // ← Validation
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "...")  // ← Validation
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")  // ← Validation
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "...")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "...")
    private String password;
}
```

**What it does**:
- Validation annotations - Enforce input rules
- `@NotBlank` - Field cannot be null or empty
- `@Email` - Must be valid email format
- `@Size` - Length constraints
- `@Pattern` - Regex validation

---

## Summary Table

| File | Component? | Main Annotation | Purpose |
|------|-----------|----------------|---------|
| CrudTestApplication.java | ✅ YES | `@SpringBootApplication` | Main application |
| SecurityConfig.java | ✅ YES | `@Configuration` | Bean definitions |
| UserController.java | ✅ YES | `@RestController` | REST API endpoints |
| UserServiceImpl.java | ✅ YES | `@Service` | Business logic |
| UserRepository.java | ✅ YES | `@Repository` | Data access |
| GlobalExceptionHandler.java | ✅ YES | `@RestControllerAdvice` | Exception handling |
| User.java | ❌ NO | `@Entity` | JPA entity (database) |
| UserRegistrationRequest.java | ❌ NO | `@Data` | DTO with validation |
| UserRegistrationResponse.java | ❌ NO | `@Data` | DTO for responses |
| ErrorResponse.java | ❌ NO | `@Data` | DTO for errors |
| UserAlreadyExistsException.java | ❌ NO | (none) | Custom exception |
| UserService.java | ❌ NO | (none) | Interface |

## How Spring Manages Components

### Component Lifecycle

1. **Component Scanning**: Spring scans packages for classes with component annotations
2. **Bean Creation**: Spring creates instances (beans) of these classes
3. **Dependency Injection**: Spring injects required dependencies
4. **Management**: Spring manages the lifecycle and dependencies

### Dependency Injection Example

```java
@Service
public class UserServiceImpl {
    private final UserRepository userRepository;  // Dependency
    private final PasswordEncoder passwordEncoder;  // Dependency

    // @RequiredArgsConstructor generates this constructor:
    public UserServiceImpl(UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
}
```

Spring automatically:
1. Creates UserRepository bean
2. Creates PasswordEncoder bean (from SecurityConfig)
3. Creates UserServiceImpl bean
4. Injects dependencies into UserServiceImpl

## Annotation Categories

### 1. Spring Component Annotations (Make class a Spring-managed bean)
- `@Component`, `@Service`, `@Repository`, `@Controller`, `@RestController`
- `@Configuration`, `@RestControllerAdvice`, `@SpringBootApplication`

### 2. JPA/Database Annotations (Database mapping)
- `@Entity`, `@Table`, `@Id`, `@Column`, `@GeneratedValue`
- `@CreationTimestamp`, `@UpdateTimestamp`, `@UniqueConstraint`

### 3. Validation Annotations (Input validation)
- `@NotBlank`, `@NotNull`, `@Email`, `@Size`, `@Pattern`, `@Valid`

### 4. Web Annotations (HTTP handling)
- `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`
- `@RequestBody`, `@PathVariable`, `@RequestParam`, `@ResponseStatus`

### 5. Lombok Annotations (Code generation)
- `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`
- `@Getter`, `@Setter`, `@ToString`, `@Slf4j`

### 6. Spring Behavior Annotations (Add functionality)
- `@Transactional`, `@Bean`, `@Value`, `@Autowired`
- `@ExceptionHandler`, `@ResponseStatus`

## Quick Identification Guide

**Is it a Component?** → Look for these at the class level:
```
@Component
@Service
@Repository
@Controller
@RestController
@Configuration
@RestControllerAdvice
@SpringBootApplication
```

**Is it a JPA Entity?** → Look for:
```
@Entity
```

**Does it have validation?** → Look for:
```
@NotBlank, @Email, @Size, @Pattern (on fields)
@Valid (on method parameters)
```

**Is it a REST endpoint?** → Look for:
```
@PostMapping, @GetMapping, etc. (on methods)
```
