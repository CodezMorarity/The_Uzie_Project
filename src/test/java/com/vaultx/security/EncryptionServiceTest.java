package com.vaultx.security;

import com.vaultx.exception.VaultException;
import com.vaultx.model.EncryptedValue;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {
    private final EncryptionService encryption = new EncryptionService(new SecureRandomProvider());

    @Test
    void encryptsAndDecryptsAndUsesFreshNonceEveryTime() {
        byte[] key = new byte[32]; Arrays.fill(key, (byte) 23);
        byte[] plaintext = "private credential payload".getBytes(StandardCharsets.UTF_8);
        UUID id = UUID.randomUUID();
        EncryptedValue first = encryption.encrypt(id, plaintext, key);
        EncryptedValue second = encryption.encrypt(id, plaintext, key);
        assertArrayEquals(plaintext, encryption.decrypt(id, first.nonce(), first.ciphertext(), key));
        assertFalse(Arrays.equals(first.nonce(), second.nonce()));
        assertFalse(Arrays.equals(first.ciphertext(), second.ciphertext()));
    }

    @Test
    void rejectsCiphertextTamperingAndRecordIdSubstitution() {
        byte[] key = new byte[32]; Arrays.fill(key, (byte) 4);
        byte[] plaintext = "secret".getBytes(StandardCharsets.UTF_8);
        UUID id = UUID.randomUUID();
        EncryptedValue value = encryption.encrypt(id, plaintext, key);
        byte[] tampered = value.ciphertext(); tampered[0] ^= 1;
        assertThrows(VaultException.class, () -> encryption.decrypt(id, value.nonce(), tampered, key));
        assertThrows(VaultException.class, () -> encryption.decrypt(UUID.randomUUID(), value.nonce(), value.ciphertext(), key));
    }

    @Test
    void backupEncryptionAuthenticatesHeaderAsAssociatedData() {
        byte[] key = new byte[32]; Arrays.fill(key, (byte) 9);
        byte[] nonce = new SecureRandomProvider().bytes(12);
        byte[] header = "VXBACKUP header".getBytes(StandardCharsets.UTF_8);
        byte[] plaintext = "backup payload".getBytes(StandardCharsets.UTF_8);
        EncryptedValue value = encryption.encrypt(plaintext, key, nonce, header);
        assertArrayEquals(plaintext, encryption.decrypt(value.ciphertext(), key, nonce, header));
        header[0] ^= 1;
        assertThrows(VaultException.class, () -> encryption.decrypt(value.ciphertext(), key, nonce, header));
    }
}
