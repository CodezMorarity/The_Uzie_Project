package com.vaultx.service;

import com.vaultx.config.AppConfig;
import com.vaultx.exception.AuthenticationException;
import com.vaultx.exception.VaultException;
import com.vaultx.model.EncryptedCredential;
import com.vaultx.model.EncryptedValue;
import com.vaultx.model.VaultMetadata;
import com.vaultx.repository.CredentialRepository;
import com.vaultx.repository.VaultRepository;
import com.vaultx.security.EncryptionService;
import com.vaultx.security.KeyDerivation;
import com.vaultx.security.SecureRandomProvider;
import com.vaultx.security.SessionManager;
import com.vaultx.service.PasswordStrengthService;
import com.vaultx.util.ValidationUtil;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AuthenticationService {
    private final VaultRepository vaultRepository;
    private final CredentialRepository credentialRepository;
    private final KeyDerivation keyDerivation;
    private final EncryptionService encryption;
    private final SecureRandomProvider random;
    private final SessionManager session;
    private final PasswordStrengthService strength;
    private final int iterations;

    public AuthenticationService(VaultRepository vaultRepository, CredentialRepository credentialRepository,
                                 KeyDerivation keyDerivation, EncryptionService encryption,
                                 SecureRandomProvider random, SessionManager session,
                                 PasswordStrengthService strength) {
        this(vaultRepository, credentialRepository, keyDerivation, encryption, random, session, strength,
                AppConfig.KDF_ITERATIONS);
    }

    public AuthenticationService(VaultRepository vaultRepository, CredentialRepository credentialRepository,
                                 KeyDerivation keyDerivation, EncryptionService encryption,
                                 SecureRandomProvider random, SessionManager session,
                                 PasswordStrengthService strength, int iterations) {
        this.vaultRepository = vaultRepository;
        this.credentialRepository = credentialRepository;
        this.keyDerivation = keyDerivation;
        this.encryption = encryption;
        this.random = random;
        this.session = session;
        this.strength = strength;
        this.iterations = iterations;
    }

    public boolean isConfigured() {
        var metadata = vaultRepository.loadMetadata();
        metadata.ifPresent(VaultMetadata::clear);
        return metadata.isPresent();
    }

    public void initialize(char[] masterPassword) {
        byte[] salt = null;
        byte[] derived = null;
        byte[] verifier = null;
        byte[] vaultKey = null;
        try {
            if (isConfigured()) throw new VaultException("Vault setup has already been completed.");
            if (!ValidationUtil.validMasterPassword(masterPassword) || !strength.meetsMasterPasswordPolicy(masterPassword)) {
                throw new VaultException("Choose a master password with at least 12 characters, three character types, and no common pattern.");
            }
            salt = random.bytes(AppConfig.KDF_SALT_BYTES);
            derived = keyDerivation.derive(masterPassword, salt, iterations);
            verifier = Arrays.copyOfRange(derived, 0, 32);
            vaultKey = Arrays.copyOfRange(derived, 32, 64);
            VaultMetadata metadata = new VaultMetadata(salt, verifier, iterations,
                    AppConfig.DATABASE_SCHEMA_VERSION, Instant.now());
            try {
                vaultRepository.saveInitialMetadata(metadata);
                session.unlock(vaultKey);
            } finally {
                metadata.clear();
            }
        } finally {
            Arrays.fill(masterPassword, '\0');
            KeyDerivation.clear(salt);
            KeyDerivation.clear(derived);
            KeyDerivation.clear(verifier);
            KeyDerivation.clear(vaultKey);
        }
    }

    public void unlock(char[] masterPassword) {
        byte[] salt = null;
        byte[] storedVerifier = null;
        byte[] derived = null;
        byte[] candidateVerifier = null;
        byte[] vaultKey = null;
        try {
            VaultMetadata metadata = vaultRepository.loadMetadata()
                    .orElseThrow(() -> new AuthenticationException("Vault setup is required."));
            try {
                salt = metadata.salt();
                storedVerifier = metadata.verifier();
                derived = keyDerivation.derive(masterPassword, salt, metadata.iterations());
                candidateVerifier = Arrays.copyOfRange(derived, 0, 32);
                if (!MessageDigest.isEqual(storedVerifier, candidateVerifier)) {
                    throw new AuthenticationException("The master password is incorrect.");
                }
                vaultKey = Arrays.copyOfRange(derived, 32, 64);
                session.unlock(vaultKey);
            } finally {
                metadata.clear();
            }
        } finally {
            Arrays.fill(masterPassword, '\0');
            KeyDerivation.clear(salt);
            KeyDerivation.clear(storedVerifier);
            KeyDerivation.clear(derived);
            KeyDerivation.clear(candidateVerifier);
            KeyDerivation.clear(vaultKey);
        }
    }

    public void changeMasterPassword(char[] currentPassword, char[] newPassword) {
        byte[] oldKey = null, salt = null, derived = null, verifier = null, newKey = null;
        List<EncryptedCredential> replacements = new ArrayList<>();
        try {
            if (!session.isUnlocked()) throw new AuthenticationException("Unlock the vault before changing its master password.");
            byte[] expectedKey = session.keyCopy().orElseThrow();
            try {
                VaultMetadata current = vaultRepository.loadMetadata()
                        .orElseThrow(() -> new AuthenticationException("Vault setup is required."));
                salt = current.salt();
                byte[] storedVerifier = current.verifier();
                byte[] verification = keyDerivation.derive(currentPassword, salt, current.iterations());
                byte[] actualVerifier = Arrays.copyOfRange(verification, 0, 32);
                byte[] derivedKey = Arrays.copyOfRange(verification, 32, 64);
                boolean valid = MessageDigest.isEqual(storedVerifier, actualVerifier)
                        && MessageDigest.isEqual(expectedKey, derivedKey);
                KeyDerivation.clear(storedVerifier);
                KeyDerivation.clear(verification);
                KeyDerivation.clear(actualVerifier);
                KeyDerivation.clear(derivedKey);
                current.clear();
                if (!valid) throw new AuthenticationException("The current master password is incorrect.");
                oldKey = expectedKey.clone();
            } finally {
                KeyDerivation.clear(expectedKey);
            }

            if (!ValidationUtil.validMasterPassword(newPassword) || !strength.meetsMasterPasswordPolicy(newPassword)) {
                throw new VaultException("Choose a new master password with at least 12 characters, three character types, and no common pattern.");
            }
            KeyDerivation.clear(salt);
            salt = random.bytes(AppConfig.KDF_SALT_BYTES);
            derived = keyDerivation.derive(newPassword, salt, iterations);
            verifier = Arrays.copyOfRange(derived, 0, 32);
            newKey = Arrays.copyOfRange(derived, 32, 64);

            for (EncryptedCredential row : credentialRepository.findAll()) {
                byte[] plaintext = encryption.decrypt(row.id(), row.nonce(), row.ciphertext(), oldKey);
                try {
                    EncryptedValue value = encryption.encrypt(row.id(), plaintext, newKey);
                    replacements.add(new EncryptedCredential(row.id(), row.createdAt(), row.modifiedAt(),
                            value.nonce(), value.ciphertext()));
                } finally {
                    KeyDerivation.clear(plaintext);
                }
            }

            VaultMetadata updated = new VaultMetadata(salt, verifier, iterations,
                    AppConfig.DATABASE_SCHEMA_VERSION, Instant.now());
            try { vaultRepository.replaceVault(replacements, updated); }
            finally { updated.clear(); }
            session.unlock(newKey);
        } finally {
            Arrays.fill(currentPassword, '\0');
            Arrays.fill(newPassword, '\0');
            KeyDerivation.clear(oldKey);
            KeyDerivation.clear(salt);
            KeyDerivation.clear(derived);
            KeyDerivation.clear(verifier);
            KeyDerivation.clear(newKey);
        }
    }

    public void lock() {
        session.lock();
    }

    public boolean isUnlocked() {
        return session.isUnlocked();
    }

    public SessionManager session() {
        return session;
    }
}
