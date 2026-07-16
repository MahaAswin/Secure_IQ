package com.secureiq.SecureIQ.question.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Difficulty {
    EASY,
    MEDIUM,
    HARD;

    @JsonCreator
    public static Difficulty fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Difficulty.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
