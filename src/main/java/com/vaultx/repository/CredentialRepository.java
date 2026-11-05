package com.vaultx.repository;

import com.vaultx.database.DatabaseManager;
import com.vaultx.exception.VaultException;
import com.vaultx.model.EncryptedCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class CredentialRepository {
    private static final Logger LOG = LoggerFactory.getLogger(CredentialRepository.class);
    private final DatabaseManager database;

    public CredentialRepository(DatabaseManager database) {
        this.database = database;
    }

    public List<EncryptedCredential> findAll() {
        String sql = "SELECT id, created_at, modified_at, nonce, ciphertext FROM credentials ORDER BY modified_at DESC";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<EncryptedCredential> rows = new ArrayList<>();
            while (result.next()) rows.add(read(result));
            return rows;
        } catch (SQLException | IllegalArgumentException ex) {
            LOG.error("Could not read credential records.", ex);
            throw new VaultException("Vault entries could not be read.", ex);
        }
    }

    public Optional<EncryptedCredential> findById(UUID id) {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, created_at, modified_at, nonce, ciphertext FROM credentials WHERE id = ?")) {
            statement.setString(1, id.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(read(result)) : Optional.empty();
            }
        } catch (SQLException | IllegalArgumentException ex) {
            LOG.error("Could not read a credential record.", ex);
            throw new VaultException("Vault entry could not be read.", ex);
        }
    }

    public void insert(EncryptedCredential row) {
        String sql = "INSERT INTO credentials(id, created_at, modified_at, nonce, ciphertext) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, row);
            statement.executeUpdate();
        } catch (SQLException ex) {
            LOG.error("Could not insert a credential record.", ex);
            throw new VaultException("Vault entry could not be saved.", ex);
        }
    }

    public boolean insertIfAbsent(EncryptedCredential row) {
        String sql = "INSERT OR IGNORE INTO credentials(id, created_at, modified_at, nonce, ciphertext) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, row);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LOG.error("Could not import a credential record.", ex);
            throw new VaultException("A backup entry could not be imported.", ex);
        }
    }

    public int insertManyIfAbsent(List<EncryptedCredential> rows) {
        String sql = "INSERT OR IGNORE INTO credentials(id, created_at, modified_at, nonce, ciphertext) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (EncryptedCredential row : rows) {
                    bind(statement, row);
                    statement.addBatch();
                }
                int inserted = 0;
                for (int changed : statement.executeBatch()) if (changed > 0) inserted += changed;
                connection.commit();
                return inserted;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            LOG.error("Could not import backup records.", ex);
            throw new VaultException("Backup entries could not be imported. Existing vault data was preserved.", ex);
        }
    }

    public void update(EncryptedCredential row) {
        String sql = "UPDATE credentials SET modified_at = ?, nonce = ?, ciphertext = ? WHERE id = ?";
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, row.modifiedAt().toString());
            statement.setBytes(2, row.nonce());
            statement.setBytes(3, row.ciphertext());
            statement.setString(4, row.id().toString());
            if (statement.executeUpdate() != 1) throw new VaultException("Vault entry no longer exists.");
        } catch (SQLException ex) {
            LOG.error("Could not update a credential record.", ex);
            throw new VaultException("Vault entry could not be updated.", ex);
        }
    }

    public boolean delete(UUID id) {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM credentials WHERE id = ?")) {
            statement.setString(1, id.toString());
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LOG.error("Could not delete a credential record.", ex);
            throw new VaultException("Vault entry could not be deleted.", ex);
        }
    }

    public int count() {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM credentials");
             ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getInt(1) : 0;
        } catch (SQLException ex) {
            LOG.error("Could not count credential records.", ex);
            throw new VaultException("Vault summary could not be loaded.", ex);
        }
    }

    private static EncryptedCredential read(ResultSet result) throws SQLException {
        return new EncryptedCredential(UUID.fromString(result.getString("id")),
                Instant.parse(result.getString("created_at")), Instant.parse(result.getString("modified_at")),
                result.getBytes("nonce"), result.getBytes("ciphertext"));
    }

    private static void bind(PreparedStatement statement, EncryptedCredential row) throws SQLException {
        statement.setString(1, row.id().toString());
        statement.setString(2, row.createdAt().toString());
        statement.setString(3, row.modifiedAt().toString());
        statement.setBytes(4, row.nonce());
        statement.setBytes(5, row.ciphertext());
    }
}
