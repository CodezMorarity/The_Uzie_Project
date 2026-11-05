package com.vaultx.model;

public record EncryptedValue(byte[] nonce, byte[] ciphertext) {
    public EncryptedValue {
        nonce = nonce.clone();
        ciphertext = ciphertext.clone();
    }

    @Override public byte[] nonce() { return nonce.clone(); }
    @Override public byte[] ciphertext() { return ciphertext.clone(); }
}
