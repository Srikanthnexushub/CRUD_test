package org.example.enums;

/**
 * Enum representing audit action types for standardized logging.
 */
public enum AuditAction {
    // Authentication Actions
    LOGIN("User login"),
    LOGIN_FAILED("Failed login attempt"),
    LOGOUT("User logout"),
    REGISTER("User registration"),

    // User Management Actions
    CREATE_USER("User created"),
    UPDATE_USER("User updated"),
    DELETE_USER("User deleted"),
    VIEW_USER("User viewed"),
    VIEW_ALL_USERS("All users viewed"),

    // Password Actions
    CHANGE_PASSWORD("Password changed"),
    PASSWORD_RESET_REQUESTED("Password reset requested"),
    PASSWORD_RESET_REQUEST_RATE_LIMITED("Password reset request rate limited"),
    PASSWORD_RESET_COMPLETED("Password reset completed"),
    RESET_PASSWORD_REQUEST("Password reset requested"),
    RESET_PASSWORD_COMPLETE("Password reset completed"),

    // Account Security Actions
    ACCOUNT_LOCKED("Account locked"),
    ACCOUNT_UNLOCKED("Account unlocked"),

    // MFA Actions
    MFA_ENABLED("MFA enabled"),
    MFA_DISABLED("MFA disabled"),
    MFA_VERIFIED("MFA code verified"),
    MFA_FAILED("MFA verification failed"),
    MFA_BACKUP_CODE_USED("MFA backup code used"),
    MFA_BACKUP_CODES_REGENERATED("MFA backup codes regenerated"),

    // Trusted Device Actions
    DEVICE_TRUSTED("Device trusted"),
    DEVICE_REVOKED("Trusted device revoked"),
    ALL_DEVICES_REVOKED("All trusted devices revoked"),

    // Authorization Actions
    UNAUTHORIZED_ACCESS("Unauthorized access attempt"),
    PERMISSION_DENIED("Permission denied"),

    // Session Actions
    SESSION_CREATED("Session created"),
    SESSION_EXPIRED("Session expired"),
    SESSION_INVALIDATED("Session invalidated"),

    // Data Actions
    DATA_EXPORT("Data exported"),
    DATA_IMPORT("Data imported"),

    // System Actions
    SYSTEM_ERROR("System error occurred"),
    CONFIGURATION_CHANGED("Configuration changed"),

    // Security Events
    SUSPICIOUS_ACTIVITY("Suspicious activity detected"),
    BRUTE_FORCE_ATTEMPT("Brute force attempt detected"),
    IP_BLOCKED("IP address blocked");

    private final String description;

    AuditAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
