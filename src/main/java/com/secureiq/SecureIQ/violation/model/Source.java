package com.secureiq.SecureIQ.violation.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Source {
    ELECTRON,
    AI_ENGINE,
    SYSTEM,
    FACULTY;

    @JsonCreator
    public static Source fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Source.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
