package ai.nova.platform.deployment.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EnvironmentType {
    DEVELOPMENT,
    TESTING,
    QA,
    STAGING,
    PRODUCTION,
    CUSTOM,
    DR;

    @JsonCreator
    public static EnvironmentType fromJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if ("TEST".equalsIgnoreCase(value.trim())) {
            return TESTING;
        }
        return valueOf(value.trim().toUpperCase());
    }
}
