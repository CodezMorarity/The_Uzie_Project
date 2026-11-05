package com.vaultx.model;

import java.time.Instant;
import java.util.UUID;

public record EncryptedCredential(UUID id, Instant createdAt, Instant modifiedAt, byte[] nonce, byte[] ciphertext) {
    public EncryptedCredential {
        nonce = nonce.clone();
        ciphertext = ciphertext.clone();
    }

    @Override public byte[] nonce() { return nonce.clone(); }
    @Override public byte[] ciphertext() { return ciphertext.clone(); }
}
