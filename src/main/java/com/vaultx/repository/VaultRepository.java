package com.vaultx.repository;

import com.vaultx.database.DatabaseManager;
import com.vaultx.exception.VaultException;
import com.vaultx.model.EncryptedCredential;
import com.vaultx.model.VaultMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class VaultRepository {
    private static final Logger LOG = LoggerFactory.getLogger(VaultRepository.class);
    private final DatabaseManager database;

    public VaultRepository(DatabaseManager database) {
        this.database = database;
    }

    public Optional<VaultMetadata> loadMetadata() {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT kdf_salt, verifier, iterations, schema_version, created_at FROM vault_metadata WHERE id = 1");
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) return Optional.empty();
            return Optional.of(new VaultMetadata(result.getBytes("kdf_salt"), result.getBytes("verifier"),
                    result.getInt("iterations"), result.getInt("schema_version"), Instant.parse(result.getString("created_at"))));
        } catch (SQLException ex) {
            LOG.error("Could not read vault metadata.", ex);
            throw new VaultException("Vault authentication data could not be read.", ex);
        }
    }

    public void saveInitialMetadata(VaultMetadata metadata) {
        String sql = "INSERT INTO vault_metadata(id, kdf_salt, verifier, iterations, schema_version, created_at) VALUES (1, ?, ?, ?, ?, ?)";
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bindMetadata(statement, metadata);
            statement.executeUpdate();
        } catch (SQLException ex) {
            LOG.error("Could not initialize vault metadata.", ex);
            throw new VaultException("Vault setup could not be completed.", ex);
        }
    }

    public void replaceVault(List<EncryptedCredential> credentials, VaultMetadata metadata) {
        String insertSql = "INSERT INTO credentials(id, created_at, modified_at, nonce, ciphertext) VALUES (?, ?, ?, ?, ?)";
        String updateMetadataSql = "UPDATE vault_metadata SET kdf_salt = ?, verifier = ?, iterations = ?, schema_version = ? WHERE id = 1";
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement delete = connection.prepareStatement("DELETE FROM credentials")) {
                    delete.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                    for (EncryptedCredential row : credentials) {
                        insert.setString(1, row.id().toString());
                        insert.setString(2, row.createdAt().toString());
                        insert.setString(3, row.modifiedAt().toString());
                        insert.setBytes(4, row.nonce());
                        insert.setBytes(5, row.ciphertext());
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                try (PreparedStatement update = connection.prepareStatement(updateMetadataSql)) {
                    update.setBytes(1, metadata.salt());
                    update.setBytes(2, metadata.verifier());
                    update.setInt(3, metadata.iterations());
                    update.setInt(4, metadata.schemaVersion());
                    if (update.executeUpdate() != 1) throw new SQLException("Vault metadata row is missing.");
                }
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            LOG.error("Could not atomically replace vault encryption metadata.", ex);
            throw new VaultException("Master password could not be changed. Existing vault data remains intact.", ex);
        }
    }

    private static void bindMetadata(PreparedStatement statement, VaultMetadata metadata) throws SQLException {
        statement.setBytes(1, metadata.salt());
        statement.setBytes(2, metadata.verifier());
        statement.setInt(3, metadata.iterations());
        statement.setInt(4, metadata.schemaVersion());
        statement.setString(5, metadata.createdAt().toString());
    }
}
