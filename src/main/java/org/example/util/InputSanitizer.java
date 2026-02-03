package org.example.util;

import org.springframework.web.util.HtmlUtils;

import java.util.regex.Pattern;

/**
 * Input Sanitization Utility
 * Provides methods to sanitize and validate user input
 */
public class InputSanitizer {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_-]{3,50}$"
    );

    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9]+$"
    );

    /**
     * Sanitize HTML input to prevent XSS attacks
     */
    public static String sanitizeHtml(String input) {
        if (input == null) {
            return null;
        }
        return HtmlUtils.htmlEscape(input);
    }

    /**
     * Sanitize SQL input to prevent SQL injection
     */
    public static String sanitizeSql(String input) {
        if (input == null) {
            return null;
        }
        // Remove common SQL injection characters
        return input.replaceAll("[;'\"\\-\\-/\\*\\*/]", "");
    }

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate username format
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * Validate alphanumeric string
     */
    public static boolean isAlphanumeric(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return ALPHANUMERIC_PATTERN.matcher(input).matches();
    }

    /**
     * Remove special characters
     */
    public static String removeSpecialCharacters(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("[^a-zA-Z0-9\\s]", "");
    }

    /**
     * Truncate string to maximum length
     */
    public static String truncate(String input, int maxLength) {
        if (input == null) {
            return null;
        }
        if (input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, maxLength);
    }

    /**
     * Sanitize file name to prevent path traversal
     */
    public static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        // Remove path separators and special characters
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Check if string contains only safe characters
     */
    public static boolean isSafeString(String input) {
        if (input == null || input.isEmpty()) {
            return true;
        }

        // Check for dangerous patterns
        String[] dangerousPatterns = {
            "<script", "javascript:", "onerror=", "onload=",
            "eval(", "exec(", "../", "..\\",
            "DROP TABLE", "DELETE FROM", "INSERT INTO", "UPDATE ",
            "'; --", "' OR '1'='1", "'; DROP TABLE"
        };

        String lowerInput = input.toLowerCase();
        for (String pattern : dangerousPatterns) {
            if (lowerInput.contains(pattern.toLowerCase())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Normalize whitespace
     */
    public static String normalizeWhitespace(String input) {
        if (input == null) {
            return null;
        }
        return input.trim().replaceAll("\\s+", " ");
    }
}
