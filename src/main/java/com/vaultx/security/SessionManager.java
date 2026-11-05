package com.vaultx.security;

import java.util.Arrays;
import java.util.Optional;

public final class SessionManager implements AutoCloseable {
    private byte[] vaultKey;
    private long lastActivityNanos;

    public synchronized boolean isUnlocked() {
        return vaultKey != null;
    }

    public synchronized void unlock(byte[] key) {
        lock();
        vaultKey = key.clone();
        touch();
    }

    public synchronized Optional<byte[]> keyCopy() {
        return vaultKey == null ? Optional.empty() : Optional.of(vaultKey.clone());
    }

    public synchronized void touch() {
        lastActivityNanos = System.nanoTime();
    }

    public synchronized boolean hasExpired(int timeoutMinutes) {
        if (timeoutMinutes <= 0 || vaultKey == null) return false;
        return System.nanoTime() - lastActivityNanos >= timeoutMinutes * 60_000_000_000L;
    }

    public synchronized void lock() {
        if (vaultKey != null) Arrays.fill(vaultKey, (byte) 0);
        vaultKey = null;
        lastActivityNanos = 0;
    }

    @Override public void close() { lock(); }
}
