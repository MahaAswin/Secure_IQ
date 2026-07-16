package com.secureiq.SecureIQ.violation.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    @JsonCreator
    public static Severity fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Severity.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
