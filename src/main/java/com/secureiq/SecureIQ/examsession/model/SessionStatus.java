package com.secureiq.SecureIQ.examsession.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SessionStatus {
    SCHEDULED,
    LIVE,
    COMPLETED,
    CANCELLED;

    @JsonCreator
    public static SessionStatus fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return SessionStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
