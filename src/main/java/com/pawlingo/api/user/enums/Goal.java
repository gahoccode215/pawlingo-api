package com.pawlingo.api.user.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Goal {
    BEGINNER("beginner"),
    TEST_PREP("test-prep"),
    PROFESSIONAL("professional"),
    FOR_CHILD("for-child");

    private final String value;

    Goal(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Goal fromValue(String value) {
        for (Goal goal : values()) {
            if (goal.value.equalsIgnoreCase(value)) {
                return goal;
            }
        }
        throw new IllegalArgumentException("Unknown goal: " + value);
    }
}
