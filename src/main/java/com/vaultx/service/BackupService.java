package com.vaultx.service;

import com.vaultx.config.AppConfig;
import com.vaultx.exception.AuthenticationException;
import com.vaultx.exception.VaultException;
import com.vaultx.model.Credential;
import com.vaultx.model.EncryptedValue;
import com.vaultx.security.CredentialCodec;
import com.vaultx.security.EncryptionService;
import com.vaultx.security.KeyDerivation;
import com.vaultx.security.SecureRandomProvider;
import com.vaultx.security.SessionManager;
import com.vaultx.util.ValidationUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class BackupService {
    private static final byte[] MAGIC = "VXBACKUP".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private static final int FORMAT_VERSION = 1;
    private static final int NONCE_BYTES = 12;
    private static final int HEADER_BYTES = 8 + Integer.BYTES * 2 + AppConfig.KDF_SALT_BYTES + NONCE_BYTES;
    private static final long MAX_BACKUP_BYTES = 100L * 1024 * 1024;

    private final CredentialCodec codec;
    private final EncryptionService encryption;
    private final KeyDerivation keyDerivation;
    private final SecureRandomProvider random;
    private final SessionManager session;
    private final PasswordStrengthService strength;
    private final int iterations;

    public BackupService(CredentialCodec codec, EncryptionService encryption, KeyDerivation keyDerivation,
                         SecureRandomProvider random, SessionManager session, PasswordStrengthService strength) {
        this(codec, encryption, keyDerivation, random, session, strength, AppConfig.KDF_ITERATIONS);
    }

    public BackupService(CredentialCodec codec, EncryptionService encryption, KeyDerivation keyDerivation,
                         SecureRandomProvider random, SessionManager session, PasswordStrengthService strength,
                         int iterations) {
        this.codec = codec;
        this.encryption = encryption;
        this.keyDerivation = keyDerivation;
        this.random = random;
        this.session = session;
        this.strength = strength;
        this.iterations = iterations;
    }

    public void exportBackup(List<Credential> credentials, char[] backupPassword, Path destination) {
        byte[] salt = null, nonce = null, backupKey = null, payload = null, header = null;
        byte[] ciphertext = null, file = null, derived = null;
        Path temporary = null;
        try {
            requireUnlocked();
            requireStrongPassword(backupPassword);
            for (Credential credential : credentials) ValidationUtil.validateCredential(credential);
            salt = random.bytes(AppConfig.KDF_SALT_BYTES);
            nonce = random.bytes(NONCE_BYTES);
            payload = codec.encodeList(credentials);
            header = header(iterations, salt, nonce);
            derived = keyDerivation.derive(backupPassword, salt, iterations);
            backupKey = Arrays.copyOf(derived, 32);
            EncryptedValue encrypted = encryption.encrypt(payload, backupKey, nonce, header);
            ciphertext = encrypted.ciphertext();
            file = ByteBuffer.allocate(header.length + ciphertext.length).put(header).put(ciphertext).array();
            if (file.length > MAX_BACKUP_BYTES) throw new VaultException("Encrypted backup exceeds the 100 MB size limit.");

            Path parent = destination.toAbsolutePath().normalize().getParent();
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, ".vaultx-backup-", ".tmp");
            Files.write(temporary, file, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new VaultException("Encrypted backup could not be written.", ex);
        } finally {
            Arrays.fill(backupPassword, '\0');
            KeyDerivation.clear(salt);
            KeyDerivation.clear(nonce);
            KeyDerivation.clear(backupKey);
            KeyDerivation.clear(derived);
            KeyDerivation.clear(payload);
            KeyDerivation.clear(header);
            KeyDerivation.clear(ciphertext);
            KeyDerivation.clear(file);
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    public List<Credential> importBackup(Path source, char[] backupPassword) {
        byte[] file = null, header = null, salt = null, nonce = null, derived = null, key = null, ciphertext = null, plaintext = null;
        List<Credential> credentials = new ArrayList<>();
        try {
            requireUnlocked();
            long size = Files.size(source);
            if (size < HEADER_BYTES + 16 || size > MAX_BACKUP_BYTES) throw new VaultException("Backup file size is invalid.");
            file = Files.readAllBytes(source);
            ByteBuffer buffer = ByteBuffer.wrap(file);
            byte[] magic = new byte[MAGIC.length]; buffer.get(magic);
            if (!Arrays.equals(magic, MAGIC)) throw new VaultException("This file is not a VaultX backup.");
            int version = buffer.getInt();
            if (version != FORMAT_VERSION) throw new VaultException("This backup version is not supported.");
            int backupIterations = buffer.getInt();
            if (backupIterations < 10_000 || backupIterations > 10_000_000) throw new VaultException("Backup key settings are invalid.");
            salt = new byte[AppConfig.KDF_SALT_BYTES]; buffer.get(salt);
            nonce = new byte[NONCE_BYTES]; buffer.get(nonce);
            header = Arrays.copyOf(file, HEADER_BYTES);
            ciphertext = Arrays.copyOfRange(file, HEADER_BYTES, file.length);
            derived = keyDerivation.derive(backupPassword, salt, backupIterations);
            key = Arrays.copyOf(derived, 32);
            plaintext = encryption.decrypt(ciphertext, key, nonce, header);
            credentials = codec.decodeList(plaintext);
            Set<UUID> ids = new HashSet<>();
            for (Credential credential : credentials) {
                ValidationUtil.validateCredential(credential);
                if (!ids.add(credential.id())) throw new VaultException("Backup contains duplicate credential identifiers.");
            }
            return credentials;
        } catch (IOException ex) {
            credentials.forEach(Credential::close);
            throw new VaultException("Backup file could not be read.", ex);
        } catch (RuntimeException ex) {
            credentials.forEach(Credential::close);
            throw ex;
        } finally {
            Arrays.fill(backupPassword, '\0');
            KeyDerivation.clear(file);
            KeyDerivation.clear(header);
            KeyDerivation.clear(salt);
            KeyDerivation.clear(nonce);
            KeyDerivation.clear(derived);
            KeyDerivation.clear(key);
            KeyDerivation.clear(ciphertext);
            KeyDerivation.clear(plaintext);
        }
    }

    private static byte[] header(int iterations, byte[] salt, byte[] nonce) {
        return ByteBuffer.allocate(HEADER_BYTES).put(MAGIC).putInt(FORMAT_VERSION).putInt(iterations).put(salt).put(nonce).array();
    }

    private void requireStrongPassword(char[] password) {
        if (!ValidationUtil.validMasterPassword(password) || !strength.meetsMasterPasswordPolicy(password)) {
            throw new VaultException("Use a separate backup password with at least 12 characters and a strong rating.");
        }
    }

    private void requireUnlocked() {
        if (!session.isUnlocked()) throw new AuthenticationException("Unlock the vault before working with backups.");
    }
}
