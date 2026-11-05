package com.vaultx.service;

import com.vaultx.exception.AuthenticationException;
import com.vaultx.exception.VaultException;
import com.vaultx.model.Credential;
import com.vaultx.model.EncryptedCredential;
import com.vaultx.model.EncryptedValue;
import com.vaultx.repository.CredentialRepository;
import com.vaultx.security.CredentialCodec;
import com.vaultx.security.EncryptionService;
import com.vaultx.security.KeyDerivation;
import com.vaultx.security.SessionManager;
import com.vaultx.util.ValidationUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class CredentialService {
    private final CredentialRepository repository;
    private final EncryptionService encryption;
    private final CredentialCodec codec;
    private final SessionManager session;

    public CredentialService(CredentialRepository repository, EncryptionService encryption,
                             CredentialCodec codec, SessionManager session) {
        this.repository = repository;
        this.encryption = encryption;
        this.codec = codec;
        this.session = session;
    }

    public List<Credential> findAll() {
        byte[] key = requireSessionKey();
        List<Credential> credentials = new ArrayList<>();
        try {
            for (EncryptedCredential row : repository.findAll()) {
                byte[] plaintext = encryption.decrypt(row.id(), row.nonce(), row.ciphertext(), key);
                try {
                    Credential credential = codec.decode(plaintext);
                    if (!credential.id().equals(row.id()) || !credential.createdAt().equals(row.createdAt())
                            || !credential.modifiedAt().equals(row.modifiedAt())) {
                        credential.close();
                        throw new VaultException("Vault entry metadata failed integrity validation.");
                    }
                    credentials.add(credential);
                } finally {
                    KeyDerivation.clear(plaintext);
                }
            }
            credentials.sort(Comparator.comparing(Credential::modifiedAt).reversed());
            return credentials;
        } catch (RuntimeException ex) {
            credentials.forEach(Credential::close);
            throw ex;
        } finally {
            KeyDerivation.clear(key);
        }
    }

    public void add(Credential credential) {
        ValidationUtil.validateCredential(credential);
        byte[] key = requireSessionKey();
        byte[] plaintext = codec.encode(credential);
        try {
            EncryptedValue value = encryption.encrypt(credential.id(), plaintext, key);
            repository.insert(new EncryptedCredential(credential.id(), credential.createdAt(), credential.modifiedAt(),
                    value.nonce(), value.ciphertext()));
        } finally {
            KeyDerivation.clear(plaintext);
            KeyDerivation.clear(key);
        }
    }

    public void update(Credential credential) {
        ValidationUtil.validateCredential(credential);
        byte[] key = requireSessionKey();
        byte[] plaintext = codec.encode(credential);
        try {
            EncryptedValue value = encryption.encrypt(credential.id(), plaintext, key);
            repository.update(new EncryptedCredential(credential.id(), credential.createdAt(), credential.modifiedAt(),
                    value.nonce(), value.ciphertext()));
        } finally {
            KeyDerivation.clear(plaintext);
            KeyDerivation.clear(key);
        }
    }

    public boolean delete(UUID id) {
        ensureUnlocked();
        return repository.delete(id);
    }

    public int importNew(List<Credential> imports) {
        ensureUnlocked();
        List<EncryptedCredential> encrypted = new ArrayList<>(imports.size());
        byte[] key = requireSessionKey();
        try {
            for (Credential credential : imports) {
                ValidationUtil.validateCredential(credential);
                byte[] plaintext = codec.encode(credential);
                try {
                    EncryptedValue value = encryption.encrypt(credential.id(), plaintext, key);
                    encrypted.add(new EncryptedCredential(credential.id(), credential.createdAt(), credential.modifiedAt(),
                            value.nonce(), value.ciphertext()));
                } finally {
                    KeyDerivation.clear(plaintext);
                }
            }
            return repository.insertManyIfAbsent(encrypted);
        } finally {
            KeyDerivation.clear(key);
        }
    }

    public int count() {
        ensureUnlocked();
        return repository.count();
    }

    private byte[] requireSessionKey() {
        return session.keyCopy().orElseThrow(() -> new AuthenticationException("Unlock the vault to continue."));
    }

    private void ensureUnlocked() {
        if (!session.isUnlocked()) throw new AuthenticationException("Unlock the vault to continue.");
    }
}
