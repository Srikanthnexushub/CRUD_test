package org.example.specification;

import jakarta.persistence.criteria.Predicate;
import org.example.dto.UserFilterRequest;
import org.example.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification for dynamic user filtering.
 * Enables complex queries without writing custom repository methods.
 */
public class UserSpecification {

    /**
     * Build specification from filter request.
     *
     * @param filter the filter criteria
     * @return specification for querying
     */
    public static Specification<User> withFilters(UserFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Username filter (partial match, case-insensitive)
            if (filter.getUsername() != null && !filter.getUsername().trim().isEmpty()) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("username")),
                                "%" + filter.getUsername().toLowerCase() + "%"
                        )
                );
            }

            // Email filter (partial match, case-insensitive)
            if (filter.getEmail() != null && !filter.getEmail().trim().isEmpty()) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("email")),
                                "%" + filter.getEmail().toLowerCase() + "%"
                        )
                );
            }

            // Role filter (exact match)
            if (filter.getRole() != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), filter.getRole()));
            }

            // MFA enabled filter
            if (filter.getMfaEnabled() != null) {
                predicates.add(criteriaBuilder.equal(root.get("mfaEnabled"), filter.getMfaEnabled()));
            }

            // Account locked filter
            if (filter.getIsAccountLocked() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isAccountLocked"), filter.getIsAccountLocked()));
            }

            // Created after filter
            if (filter.getCreatedAfter() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedAfter())
                );
            }

            // Created before filter
            if (filter.getCreatedBefore() != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedBefore())
                );
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filter for active users (not locked, MFA not required).
     */
    public static Specification<User> activeUsers() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("isAccountLocked"), false);
    }

    /**
     * Filter for locked users.
     */
    public static Specification<User> lockedUsers() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("isAccountLocked"), true);
    }

    /**
     * Filter for users with MFA enabled.
     */
    public static Specification<User> mfaEnabledUsers() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("mfaEnabled"), true);
    }

    /**
     * Filter by username (exact match).
     */
    public static Specification<User> hasUsername(String username) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("username"), username);
    }

    /**
     * Filter by email (exact match).
     */
    public static Specification<User> hasEmail(String email) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("email"), email);
    }

    /**
     * Filter by role.
     */
    public static Specification<User> hasRole(org.example.entity.Role role) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("role"), role);
    }
}
