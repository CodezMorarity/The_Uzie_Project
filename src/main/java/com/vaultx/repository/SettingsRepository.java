package com.vaultx.repository;

import com.vaultx.database.DatabaseManager;
import com.vaultx.exception.VaultException;
import com.vaultx.model.AppSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class SettingsRepository {
    private static final Logger LOG = LoggerFactory.getLogger(SettingsRepository.class);
    private final DatabaseManager database;

    public SettingsRepository(DatabaseManager database) {
        this.database = database;
    }

    public AppSettings load() {
        Map<String, String> values = new HashMap<>();
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("SELECT setting_key, setting_value FROM application_settings");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) values.put(result.getString(1), result.getString(2));
            return new AppSettings(Integer.parseInt(values.getOrDefault("autoLockMinutes", "5")),
                    Integer.parseInt(values.getOrDefault("clipboardClearSeconds", "30")),
                    AppSettings.Theme.valueOf(values.getOrDefault("theme", "DARK")));
        } catch (SQLException | IllegalArgumentException ex) {
            LOG.error("Could not load application settings.", ex);
            throw new VaultException("Application settings could not be loaded.", ex);
        }
    }

    public void save(AppSettings settings) {
        String sql = "INSERT INTO application_settings(setting_key, setting_value) VALUES (?, ?) "
                + "ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value";
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            put(statement, "autoLockMinutes", Integer.toString(settings.autoLockMinutes()));
            put(statement, "clipboardClearSeconds", Integer.toString(settings.clipboardClearSeconds()));
            put(statement, "theme", settings.theme().name());
        } catch (SQLException ex) {
            LOG.error("Could not save application settings.", ex);
            throw new VaultException("Application settings could not be saved.", ex);
        }
    }

    private static void put(PreparedStatement statement, String key, String value) throws SQLException {
        statement.setString(1, key);
        statement.setString(2, value);
        statement.executeUpdate();
    }
}
