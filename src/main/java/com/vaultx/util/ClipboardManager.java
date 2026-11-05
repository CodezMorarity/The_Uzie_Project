package com.vaultx.util;

import com.vaultx.security.KeyDerivation;
import javafx.application.Platform;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class ClipboardManager implements AutoCloseable {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "vaultx-clipboard-clear");
        thread.setDaemon(true);
        return thread;
    });
    private int clearTimeoutSeconds;
    private byte[] ownedDigest;
    private long sequence;
    private ScheduledFuture<?> pendingClear;

    public ClipboardManager(int clearTimeoutSeconds) {
        setClearTimeoutSeconds(clearTimeoutSeconds);
    }

    public synchronized void setClearTimeoutSeconds(int seconds) {
        if (seconds < 0 || seconds > 300) throw new IllegalArgumentException("Clipboard timeout must be 0–300 seconds.");
        clearTimeoutSeconds = seconds;
    }

    public void copy(String value) {
        if (value == null) return;
        copyValue(value);
    }

    public void copy(char[] value) {
        if (value == null) return;
        String transientValue = new String(value);
        Arrays.fill(value, '\0');
        copyValue(transientValue);
    }

    private void copyValue(String value) {
        byte[] digest = digest(value);
        long current;
        int timeout;
        synchronized (this) {
            sequence++;
            current = sequence;
            timeout = clearTimeoutSeconds;
            if (pendingClear != null) pendingClear.cancel(false);
            KeyDerivation.clear(ownedDigest);
            ownedDigest = digest.clone();
        }
        Platform.runLater(() -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(value);
            Clipboard.getSystemClipboard().setContent(content);
        });
        if (timeout > 0) {
            byte[] scheduledDigest = digest.clone();
            synchronized (this) {
                pendingClear = scheduler.schedule(() -> Platform.runLater(() -> clearIfOwned(current, scheduledDigest)), timeout, TimeUnit.SECONDS);
            }
        }
        KeyDerivation.clear(digest);
    }

    public void clearOwned() {
        final long current;
        synchronized (this) {
            current = ++sequence;
            if (pendingClear != null) pendingClear.cancel(false);
        }
        Platform.runLater(() -> clearIfOwned(current, null));
    }

    private void clearIfOwned(long expectedSequence, byte[] expectedDigest) {
        byte[] owned;
        synchronized (this) {
            if (expectedSequence != sequence || ownedDigest == null) {
                KeyDerivation.clear(expectedDigest);
                return;
            }
            owned = ownedDigest.clone();
        }
        Clipboard clipboard = Clipboard.getSystemClipboard();
        String current = clipboard.hasString() ? clipboard.getString() : null;
        byte[] currentDigest = current == null ? null : digest(current);
        boolean stillOwned = currentDigest != null && MessageDigest.isEqual(owned, currentDigest)
                && (expectedDigest == null || MessageDigest.isEqual(owned, expectedDigest));
        if (stillOwned) clipboard.clear();
        synchronized (this) {
            if (expectedSequence == sequence) {
                KeyDerivation.clear(ownedDigest);
                ownedDigest = null;
            }
        }
        KeyDerivation.clear(owned);
        KeyDerivation.clear(currentDigest);
        KeyDerivation.clear(expectedDigest);
    }

    private static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }

    @Override public void close() {
        clearOwned();
        scheduler.shutdownNow();
    }
}
