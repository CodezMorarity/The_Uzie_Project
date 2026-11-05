package com.vaultx.model;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public final class Credential implements AutoCloseable {
    private final UUID id;
    private final String name;
    private final String url;
    private final String username;
    private char[] password;
    private final String category;
    private final String notes;
    private final Instant createdAt;
    private final Instant modifiedAt;
    private final boolean favorite;

    public Credential(UUID id, String name, String url, String username, char[] password,
                      String category, String notes, Instant createdAt, Instant modifiedAt, boolean favorite) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNullElse(name, "");
        this.url = Objects.requireNonNullElse(url, "");
        this.username = Objects.requireNonNullElse(username, "");
        this.password = Objects.requireNonNull(password).clone();
        this.category = Objects.requireNonNullElse(category, "Other");
        this.notes = Objects.requireNonNullElse(notes, "");
        this.createdAt = Objects.requireNonNull(createdAt);
        this.modifiedAt = Objects.requireNonNull(modifiedAt);
        this.favorite = favorite;
    }

    public static Credential create(String name, String url, String username, char[] password,
                                    String category, String notes, boolean favorite) {
        Instant now = Instant.now();
        return new Credential(UUID.randomUUID(), name, url, username, password, category, notes, now, now, favorite);
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public String url() { return url; }
    public String username() { return username; }
    public synchronized char[] password() { return password.clone(); }
    public String category() { return category; }
    public String notes() { return notes; }
    public Instant createdAt() { return createdAt; }
    public Instant modifiedAt() { return modifiedAt; }
    public boolean favorite() { return favorite; }

    public Credential edited(String newName, String newUrl, String newUsername, char[] newPassword,
                             String newCategory, String newNotes, boolean newFavorite) {
        return new Credential(id, newName, newUrl, newUsername, newPassword, newCategory, newNotes,
                createdAt, Instant.now(), newFavorite);
    }

    @Override
    public synchronized void close() {
        Arrays.fill(password, '\0');
        password = new char[0];
    }

    @Override
    public String toString() {
        return "Credential[id=" + id + ", name=" + name + ", category=" + category + "]";
    }
}
