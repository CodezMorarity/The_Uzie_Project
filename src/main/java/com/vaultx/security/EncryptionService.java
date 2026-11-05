package com.vaultx.security;

import com.vaultx.exception.VaultException;
import com.vaultx.model.EncryptedValue;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.UUID;

public final class EncryptionService {
    private final SecureRandomProvider random;

    public EncryptionService(SecureRandomProvider random) {
        this.random = random;
    }

    public EncryptedValue encrypt(UUID recordId, byte[] plaintext, byte[] key) {
        requireKey(key);
        byte[] nonce = random.bytes(AppConfigValues.NONCE_BYTES);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(AppConfigValues.TAG_BITS, nonce));
            cipher.updateAAD(recordId.toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            return new EncryptedValue(nonce, cipher.doFinal(plaintext));
        } catch (GeneralSecurityException ex) {
            throw new VaultException("Could not encrypt vault data.", ex);
        }
    }

    public byte[] decrypt(UUID recordId, byte[] nonce, byte[] ciphertext, byte[] key) {
        requireKey(key);
        if (nonce == null || nonce.length != AppConfigValues.NONCE_BYTES) throw new VaultException("Vault data is invalid.");
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(AppConfigValues.TAG_BITS, nonce));
            cipher.updateAAD(recordId.toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            return cipher.doFinal(ciphertext);
        } catch (AEADBadTagException ex) {
            throw new VaultException("Vault integrity check failed. Data may have been changed or damaged.", ex);
        } catch (GeneralSecurityException ex) {
            throw new VaultException("Could not decrypt vault data.", ex);
        }
    }

    public EncryptedValue encrypt(byte[] plaintext, byte[] key, byte[] nonce, byte[] aad) {
        requireKey(key);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(AppConfigValues.TAG_BITS, nonce));
            if (aad != null) cipher.updateAAD(aad);
            return new EncryptedValue(nonce, cipher.doFinal(plaintext));
        } catch (GeneralSecurityException ex) {
            throw new VaultException("Could not encrypt backup data.", ex);
        }
    }

    public byte[] decrypt(byte[] ciphertext, byte[] key, byte[] nonce, byte[] aad) {
        requireKey(key);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(AppConfigValues.TAG_BITS, nonce));
            if (aad != null) cipher.updateAAD(aad);
            return cipher.doFinal(ciphertext);
        } catch (AEADBadTagException ex) {
            throw new VaultException("Backup integrity check failed. The password may be wrong or the file was changed.", ex);
        } catch (GeneralSecurityException ex) {
            throw new VaultException("Could not decrypt backup data.", ex);
        }
    }

    private static void requireKey(byte[] key) {
        if (key == null || key.length != 32) throw new IllegalArgumentException("AES-256 requires a 32-byte key.");
    }

    private static final class AppConfigValues {
        private static final int NONCE_BYTES = 12;
        private static final int TAG_BITS = 128;
    }
}
