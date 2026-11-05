package com.vaultx;

import com.vaultx.database.DatabaseInitializer;
import com.vaultx.database.DatabaseManager;
import com.vaultx.repository.CredentialRepository;
import com.vaultx.repository.SettingsRepository;
import com.vaultx.repository.VaultRepository;
import com.vaultx.security.CredentialCodec;
import com.vaultx.security.EncryptionService;
import com.vaultx.security.KeyDerivation;
import com.vaultx.security.SecureRandomProvider;
import com.vaultx.security.SessionManager;
import com.vaultx.service.AuthenticationService;
import com.vaultx.service.BackupService;
import com.vaultx.service.CredentialService;
import com.vaultx.service.PasswordGeneratorService;
import com.vaultx.service.PasswordStrengthService;
import com.vaultx.service.SettingsService;

import java.nio.file.Path;

public final class TestVault implements AutoCloseable {
    static final int TEST_ITERATIONS = 10_000;
    public final DatabaseManager database;
    public final CredentialRepository credentialRepository;
    public final VaultRepository vaultRepository;
    public final SessionManager session = new SessionManager();
    public final CredentialCodec codec = new CredentialCodec();
    public final SecureRandomProvider random = new SecureRandomProvider();
    public final EncryptionService encryption = new EncryptionService(random);
    public final PasswordStrengthService strength = new PasswordStrengthService();
    public final AuthenticationService authentication;
    public final CredentialService credentials;
    public final BackupService backups;

    public TestVault(Path databasePath) {
        database = new DatabaseManager(databasePath);
        new DatabaseInitializer(database).initialize();
        credentialRepository = new CredentialRepository(database);
        vaultRepository = new VaultRepository(database);
        authentication = new AuthenticationService(vaultRepository, credentialRepository, new KeyDerivation(),
                encryption, random, session, strength, TEST_ITERATIONS);
        credentials = new CredentialService(credentialRepository, encryption, codec, session);
        backups = new BackupService(codec, encryption, new KeyDerivation(), random, session, strength, TEST_ITERATIONS);
    }

    public void setup() {
        authentication.initialize("Saffron#Bridge47!".toCharArray());
    }

    @Override public void close() { session.lock(); }
}
