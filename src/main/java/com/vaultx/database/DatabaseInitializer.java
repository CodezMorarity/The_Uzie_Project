package com.vaultx.database;

import com.vaultx.config.AppConfig;
import com.vaultx.exception.VaultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

public final class DatabaseInitializer {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseInitializer.class);
    private final DatabaseManager manager;

    public DatabaseInitializer(DatabaseManager manager) {
        this.manager = manager;
    }

    public void initialize() {
        try {
            Files.createDirectories(manager.databasePath().getParent());
            restrictDirectoryPermissions();
            try (Connection connection = manager.connect(); Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS vault_metadata (
                          id INTEGER PRIMARY KEY CHECK (id = 1),
                          kdf_salt BLOB NOT NULL,
                          verifier BLOB NOT NULL,
                          iterations INTEGER NOT NULL,
                          schema_version INTEGER NOT NULL,
                          created_at TEXT NOT NULL
                        )
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS credentials (
                          id TEXT PRIMARY KEY,
                          created_at TEXT NOT NULL,
                          modified_at TEXT NOT NULL,
                          nonce BLOB NOT NULL,
                          ciphertext BLOB NOT NULL
                        )
                        """);
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_credentials_modified ON credentials(modified_at)");
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS application_settings (
                          setting_key TEXT PRIMARY KEY,
                          setting_value TEXT NOT NULL
                        )
                        """);
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT OR IGNORE INTO application_settings(setting_key, setting_value) VALUES (?, ?)")) {
                    for (var entry : AppConfig.defaultSettings().entrySet()) {
                        insert.setString(1, entry.getKey());
                        insert.setString(2, entry.getValue());
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
            }
            restrictFilePermissions();
        } catch (SQLException | IOException ex) {
            LOG.error("Database initialization failed.", ex);
            throw new VaultException("Vault storage could not be initialized.", ex);
        }
    }

    private void restrictDirectoryPermissions() {
        try {
            Files.setPosixFilePermissions(manager.databasePath().getParent(),
                    Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Windows ACLs inherit from the current user's local application data directory.
        }
    }

    private void restrictFilePermissions() {
        try {
            Files.setPosixFilePermissions(manager.databasePath(),
                    Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException | IOException ignored) {
            // The file is already under the current user's application data directory.
        }
    }
}
