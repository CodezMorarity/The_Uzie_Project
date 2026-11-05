package com.vaultx.view;

public enum NavigationSection {
    DASHBOARD("Dashboard", "dashboard"),
    VAULT("Password Vault", "key"),
    GENERATOR("Password Generator", "generator"),
    SECURITY("Security Overview", "shield"),
    SETTINGS("Settings", "settings");

    private final String title;
    private final String icon;
    NavigationSection(String title, String icon) { this.title = title; this.icon = icon; }
    public String title() { return title; }
    public String icon() { return icon; }
}
