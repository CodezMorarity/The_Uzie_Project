package com.vaultx.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseInitializerTest {
    @TempDir Path temp;

    @Test
    void createsSchemaAndDefaultSettings() throws Exception {
        DatabaseManager manager = new DatabaseManager(temp.resolve("data/vaultx.db"));
        new DatabaseInitializer(manager).initialize();
        try (var connection = manager.connect(); Statement statement = connection.createStatement()) {
            try (ResultSet tables = statement.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table'")) {
                java.util.Set<String> actual = new java.util.HashSet<>();
                while (tables.next()) actual.add(tables.getString(1));
                assertTrue(actual.containsAll(Set.of("vault_metadata", "credentials", "application_settings")));
            }
            try (ResultSet settings = statement.executeQuery("SELECT COUNT(*) FROM application_settings")) {
                assertTrue(settings.next()); assertEquals(3, settings.getInt(1));
            }
        }
    }
}
