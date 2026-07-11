package com.secureiq.SecureIQ.user.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Role {
    ADMIN,
    FACULTY,
    HOD,
    STUDENT;

    @JsonCreator
    public static Role fromString(String value) {
        if (value == null) {
            return null;
        }
        return Role.valueOf(value.toUpperCase());
    }
}
