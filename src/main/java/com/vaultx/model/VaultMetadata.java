package com.vaultx.model;

import java.time.Instant;
import java.util.Arrays;

public final class VaultMetadata {
    private final byte[] salt;
    private final byte[] verifier;
    private final int iterations;
    private final int schemaVersion;
    private final Instant createdAt;

    public VaultMetadata(byte[] salt, byte[] verifier, int iterations, int schemaVersion, Instant createdAt) {
        this.salt = salt.clone();
        this.verifier = verifier.clone();
        this.iterations = iterations;
        this.schemaVersion = schemaVersion;
        this.createdAt = createdAt;
    }

    public byte[] salt() { return salt.clone(); }
    public byte[] verifier() { return verifier.clone(); }
    public int iterations() { return iterations; }
    public int schemaVersion() { return schemaVersion; }
    public Instant createdAt() { return createdAt; }

    public void clear() {
        Arrays.fill(salt, (byte) 0);
        Arrays.fill(verifier, (byte) 0);
    }
}
