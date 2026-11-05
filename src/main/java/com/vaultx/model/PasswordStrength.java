package com.vaultx.model;

import java.util.List;

public record PasswordStrength(StrengthLevel level, int score, List<String> feedback) {
    public PasswordStrength {
        feedback = List.copyOf(feedback);
    }
}
