package com.vaultx.model;

public record AppSettings(int autoLockMinutes, int clipboardClearSeconds, Theme theme) {
    public enum Theme { DARK, LIGHT, SYSTEM }

    public AppSettings {
        if (autoLockMinutes < 0 || autoLockMinutes > 120) throw new IllegalArgumentException("Auto-lock must be 0–120 minutes.");
        if (clipboardClearSeconds < 0 || clipboardClearSeconds > 300) throw new IllegalArgumentException("Clipboard timeout must be 0–300 seconds.");
        if (theme == null) throw new IllegalArgumentException("Theme is required.");
    }

    public static AppSettings defaults() {
        return new AppSettings(5, 30, Theme.DARK);
    }
}
