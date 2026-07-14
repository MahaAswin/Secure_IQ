package com.secureiq.SecureIQ.exam.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ExamType {
    INTERNAL,
    MODEL,
    MID_SEMESTER,
    END_SEMESTER,
    LAB,
    PRACTICAL,
    QUIZ;

    @JsonCreator
    public static ExamType fromString(String value) {
        if (value == null) {
            return null;
        }
        return ExamType.valueOf(value.toUpperCase());
    }
}
