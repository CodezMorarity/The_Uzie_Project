package com.vaultx.database;

import com.vaultx.config.AppConfig;
import org.sqlite.SQLiteConfig;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseManager {
    private final Path databasePath;

    public DatabaseManager() {
        this(AppConfig.applicationDataDirectory().resolve("vaultx.db"));
    }

    public DatabaseManager(Path databasePath) {
        this.databasePath = databasePath.toAbsolutePath().normalize();
    }

    public Path databasePath() {
        return databasePath;
    }

    public Connection connect() throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        config.setBusyTimeout(5_000);
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath, config.toProperties());
    }
}
