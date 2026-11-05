package com.vaultx.service;

import com.vaultx.model.PasswordStrength;
import com.vaultx.model.StrengthLevel;

import java.util.ArrayList;
import java.util.List;

public final class PasswordStrengthService {
    private static final String[] COMMON = {
            "password", "password1", "123456", "12345678", "qwerty", "qwerty123", "letmein",
            "welcome", "admin", "iloveyou", "abc123", "monkey", "dragon", "vaultx"
    };
    private static final String[] SEQUENCES = {"123456", "abcdef", "qwerty", "asdfgh", "zxcvbn", "password"};

    public PasswordStrength evaluate(char[] password) {
        if (password == null || password.length == 0) {
            return new PasswordStrength(StrengthLevel.WEAK, 0, List.of("Use a longer password with more character variety."));
        }

        boolean lower = false, upper = false, digit = false, symbol = false;
        for (char c : password) {
            lower |= Character.isLowerCase(c);
            upper |= Character.isUpperCase(c);
            digit |= Character.isDigit(c);
            symbol |= !Character.isLetterOrDigit(c);
        }
        int variety = (lower ? 1 : 0) + (upper ? 1 : 0) + (digit ? 1 : 0) + (symbol ? 1 : 0);
        int score = (password.length >= 8 ? 1 : 0) + (password.length >= 12 ? 2 : 0)
                + (password.length >= 16 ? 2 : 0) + variety;
        boolean common = matchesAny(password, COMMON);
        boolean sequence = containsAnySequence(password);
        boolean highlyRepeated = repeatedCharacterRatio(password) >= 0.60;
        if (common) score -= 5;
        if (sequence) score -= 2;
        if (highlyRepeated) score -= 2;

        StrengthLevel level;
        if (common || password.length < 8 || score <= 2) level = StrengthLevel.WEAK;
        else if (score <= 5 || sequence || highlyRepeated) level = StrengthLevel.MEDIUM;
        else if (score <= 8 || password.length < 16 || variety < 4) level = StrengthLevel.STRONG;
        else level = StrengthLevel.VERY_STRONG;

        List<String> feedback = new ArrayList<>();
        if (password.length < 12) feedback.add("Use at least 12 characters.");
        if (variety < 3) feedback.add("Mix more character types.");
        if (common) feedback.add("Avoid common passwords or familiar words.");
        if (sequence) feedback.add("Avoid predictable sequences.");
        if (highlyRepeated) feedback.add("Avoid repeating the same character.");
        if (feedback.isEmpty()) feedback.add("Good length and character variety.");
        feedback.add("Educational estimate only; this is not a formal security audit.");
        return new PasswordStrength(level, Math.max(0, score), feedback);
    }

    public boolean meetsMasterPasswordPolicy(char[] password) {
        if (password == null || password.length < 12) return false;
        StrengthLevel level = evaluate(password).level();
        return level == StrengthLevel.STRONG || level == StrengthLevel.VERY_STRONG;
    }

    private static boolean matchesAny(char[] value, String[] candidates) {
        for (String candidate : candidates) {
            if (candidate.length() != value.length) continue;
            boolean same = true;
            for (int i = 0; i < value.length; i++) {
                if (Character.toLowerCase(value[i]) != candidate.charAt(i)) { same = false; break; }
            }
            if (same) return true;
        }
        return false;
    }

    private static boolean containsAnySequence(char[] password) {
        for (String sequence : SEQUENCES) {
            for (int start = 0; start <= password.length - sequence.length(); start++) {
                boolean match = true;
                for (int i = 0; i < sequence.length(); i++) {
                    if (Character.toLowerCase(password[start + i]) != sequence.charAt(i)) { match = false; break; }
                }
                if (match) return true;
            }
        }
        return false;
    }

    private static double repeatedCharacterRatio(char[] password) {
        if (password.length < 2) return 1.0;
        int repeated = 0;
        for (int i = 1; i < password.length; i++) if (password[i] == password[i - 1]) repeated++;
        return (double) repeated / (password.length - 1);
    }
}
