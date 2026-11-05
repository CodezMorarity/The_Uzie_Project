package com.vaultx.model;

public enum StrengthLevel {
    WEAK("Weak"), MEDIUM("Medium"), STRONG("Strong"), VERY_STRONG("Very Strong");
    private final String label;
    StrengthLevel(String label) { this.label = label; }
    public String label() { return label; }
}
