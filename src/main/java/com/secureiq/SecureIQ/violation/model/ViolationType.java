package com.secureiq.SecureIQ.violation.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ViolationType {
    TAB_SWITCH,
    WINDOW_BLUR,
    MULTIPLE_PERSON,
    NO_FACE,
    PHONE_DETECTED,
    HEAD_POSE,
    EYE_MOVEMENT,
    COPY_PASTE,
    SCREENSHOT_ATTEMPT,
    DEVTOOLS_OPENED,
    MULTIPLE_MONITOR,
    UNKNOWN;

    @JsonCreator
    public static ViolationType fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ViolationType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ViolationType.UNKNOWN;
        }
    }
}
