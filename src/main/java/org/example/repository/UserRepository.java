package org.example.repository;

import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity operations with dynamic filtering support.
 *
 * Extends JpaSpecificationExecutor for complex queries with Specification API.
 *
 * DEBUGGING GUIDE:
 * ----------------
 * To debug queries in this repository:
 * 1. Set breakpoints in RepositoryLoggingAspect.java (lines 53, 80, 99, 117)
 * 2. Enable SQL logging in application.properties: logging.level.org.hibernate.SQL=DEBUG
 * 3. Watch console for query output with parameters
 *
 * BREAKPOINT LOCATIONS:
 * - Line 28: findByUsername() - Debug login authentication queries
 * - Line 30: findByEmail() - Debug email-based user lookups
 * - Line 24: existsByUsername() - Debug username uniqueness checks (registration)
 * - Line 26: existsByEmail() - Debug email uniqueness checks (registration)
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    // BREAKPOINT: Set in RepositoryLoggingAspect to debug username existence checks
    boolean existsByUsername(String username);

    // BREAKPOINT: Set in RepositoryLoggingAspect to debug email existence checks
    boolean existsByEmail(String email);

    // BREAKPOINT: Set in RepositoryLoggingAspect to debug login queries
    Optional<User> findByUsername(String username);

    // BREAKPOINT: Set in RepositoryLoggingAspect to debug email-based lookups
    Optional<User> findByEmail(String email);
}
