package com.secureiq.SecureIQ.exam.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ExamStatus {
    DRAFT,
    PUBLISHED,
    ACTIVE,
    COMPLETED,
    CANCELLED;

    @JsonCreator
    public static ExamStatus fromString(String value) {
        if (value == null) {
            return null;
        }
        return ExamStatus.valueOf(value.toUpperCase());
    }
}
