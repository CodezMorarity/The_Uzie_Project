package com.vaultx.security;

import java.security.SecureRandom;

public final class SecureRandomProvider {
    private final SecureRandom secureRandom;

    public SecureRandomProvider() {
        this(new SecureRandom());
    }

    SecureRandomProvider(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public byte[] bytes(int length) {
        if (length < 1) throw new IllegalArgumentException("Length must be positive.");
        byte[] result = new byte[length];
        secureRandom.nextBytes(result);
        return result;
    }

    public int nextInt(int bound) {
        return secureRandom.nextInt(bound);
    }
}
