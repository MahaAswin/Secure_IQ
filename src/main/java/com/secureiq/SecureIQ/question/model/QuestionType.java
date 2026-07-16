package com.secureiq.SecureIQ.question.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum QuestionType {
    MCQ,
    TRUE_FALSE,
    SHORT_ANSWER,
    DESCRIPTIVE;

    @JsonCreator
    public static QuestionType fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return QuestionType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
