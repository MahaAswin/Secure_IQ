package com.secureiq.SecureIQ.examattempt.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AttemptStatus {
    NOT_STARTED,
    IN_PROGRESS,
    SUBMITTED,
    AUTO_SUBMITTED,
    TERMINATED;

    @JsonCreator
    public static AttemptStatus fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return AttemptStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
