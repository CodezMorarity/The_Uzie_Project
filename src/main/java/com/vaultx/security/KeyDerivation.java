package com.vaultx.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public final class KeyDerivation {
    private final int keyBits;

    public KeyDerivation() {
        this(512);
    }

    public KeyDerivation(int keyBits) {
        if (keyBits < 256 || keyBits % 8 != 0) throw new IllegalArgumentException("Derived key length must be at least 256 bits.");
        this.keyBits = keyBits;
    }

    public byte[] derive(char[] password, byte[] salt, int iterations) {
        if (password == null || password.length == 0) throw new IllegalArgumentException("Password is required.");
        if (salt == null || salt.length < 16) throw new IllegalArgumentException("Salt must be at least 16 bytes.");
        if (iterations < 10_000 || iterations > 10_000_000) throw new IllegalArgumentException("Iteration count is outside the supported range.");
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyBits);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("PBKDF2 key derivation is unavailable.", ex);
        } finally {
            spec.clearPassword();
        }
    }

    public static void clear(byte[] bytes) {
        if (bytes != null) Arrays.fill(bytes, (byte) 0);
    }
}
