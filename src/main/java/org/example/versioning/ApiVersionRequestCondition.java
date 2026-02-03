package org.example.versioning;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

/**
 * Request condition for API versioning.
 */
public class ApiVersionRequestCondition implements RequestCondition<ApiVersionRequestCondition> {

    private final int version;

    public ApiVersionRequestCondition(int version) {
        this.version = version;
    }

    @Override
    public ApiVersionRequestCondition combine(ApiVersionRequestCondition other) {
        // Method-level annotation overrides class-level
        return new ApiVersionRequestCondition(other.version);
    }

    @Override
    public ApiVersionRequestCondition getMatchingCondition(HttpServletRequest request) {
        // Always match - version is already in URL path
        return this;
    }

    @Override
    public int compareTo(ApiVersionRequestCondition other, HttpServletRequest request) {
        // Higher version number has higher priority
        return Integer.compare(other.version, this.version);
    }

    public int getVersion() {
        return version;
    }
}
