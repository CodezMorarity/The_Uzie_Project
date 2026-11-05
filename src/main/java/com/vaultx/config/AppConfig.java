package com.vaultx.config;

import java.nio.file.Path;
import java.util.Map;

public final class AppConfig {
    public static final String APP_NAME = "VaultX";
    public static final String APP_VERSION = "1.0.0";
    public static final int KDF_ITERATIONS = 600_000;
    public static final int KDF_SALT_BYTES = 16;
    public static final int DERIVED_KEY_BYTES = 64;
    public static final int VAULT_KEY_BYTES = 32;
    public static final int GCM_NONCE_BYTES = 12;
    public static final int GCM_TAG_BITS = 128;
    public static final int DATABASE_SCHEMA_VERSION = 1;
    public static final int MIN_MASTER_PASSWORD_LENGTH = 12;

    private AppConfig() {
    }

    public static Path applicationDataDirectory() {
        String os = System.getProperty("os.name", "").toLowerCase();
        Path home = Path.of(System.getProperty("user.home"));
        if (os.contains("win")) {
            String local = System.getenv("LOCALAPPDATA");
            return (local == null || local.isBlank() ? home.resolve("AppData/Local") : Path.of(local))
                    .resolve(APP_NAME);
        }
        if (os.contains("mac")) {
            return home.resolve("Library/Application Support").resolve(APP_NAME);
        }
        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        Path base = xdgDataHome == null || xdgDataHome.isBlank()
                ? home.resolve(".local/share")
                : Path.of(xdgDataHome);
        return base.resolve("vaultx");
    }

    public static Map<String, String> defaultSettings() {
        return Map.of("autoLockMinutes", "5", "clipboardClearSeconds", "30", "theme", "DARK");
    }
}
