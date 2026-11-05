package com.vaultx.service;

import com.vaultx.security.SecureRandomProvider;

import java.util.ArrayList;
import java.util.List;

public final class PasswordGeneratorService {
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}:,.?";
    private static final String AMBIGUOUS = "Il1O0|`'\"";
    private final SecureRandomProvider random;

    public PasswordGeneratorService(SecureRandomProvider random) {
        this.random = random;
    }

    public List<char[]> generate(int length, boolean uppercase, boolean lowercase, boolean numbers,
                                  boolean symbols, boolean excludeAmbiguous, int count) {
        List<String> selected = new ArrayList<>();
        if (uppercase) selected.add(filter(UPPER, excludeAmbiguous));
        if (lowercase) selected.add(filter(LOWER, excludeAmbiguous));
        if (numbers) selected.add(filter(DIGITS, excludeAmbiguous));
        if (symbols) selected.add(filter(SYMBOLS, excludeAmbiguous));
        if (selected.isEmpty()) throw new IllegalArgumentException("Choose at least one character type.");
        if (length < Math.max(8, selected.size()) || length > 128) throw new IllegalArgumentException("Length must be 8–128 and include each selected character type.");
        if (count < 1 || count > 10) throw new IllegalArgumentException("Generate between 1 and 10 variations.");
        StringBuilder all = new StringBuilder();
        selected.forEach(all::append);

        List<char[]> results = new ArrayList<>(count);
        for (int variation = 0; variation < count; variation++) {
            char[] password = new char[length];
            int position = 0;
            for (String pool : selected) password[position++] = pool.charAt(random.nextInt(pool.length()));
            while (position < length) password[position++] = all.charAt(random.nextInt(all.length()));
            for (int i = password.length - 1; i > 0; i--) {
                int swap = random.nextInt(i + 1);
                char current = password[i]; password[i] = password[swap]; password[swap] = current;
            }
            results.add(password);
        }
        return results;
    }

    private static String filter(String pool, boolean excludeAmbiguous) {
        if (!excludeAmbiguous) return pool;
        StringBuilder filtered = new StringBuilder();
        for (int i = 0; i < pool.length(); i++) {
            char c = pool.charAt(i);
            if (AMBIGUOUS.indexOf(c) < 0) filtered.append(c);
        }
        return filtered.toString();
    }
}
