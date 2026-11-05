package com.vaultx.util;

import com.vaultx.exception.VaultException;
import com.vaultx.model.Credential;

import java.net.URI;

public final class ValidationUtil {
    private ValidationUtil() { }

    public static void validateCredential(Credential credential) {
        if (credential.name().isBlank()) throw new VaultException("Add a name for this credential.");
        if (credential.name().length() > 120) throw new VaultException("The name must be 120 characters or fewer.");
        if (credential.url().length() > 2048) throw new VaultException("The website address is too long.");
        if (!credential.url().isBlank()) {
            try {
                URI uri = URI.create(credential.url().strip());
                if (uri.getScheme() == null || !(uri.getScheme().equalsIgnoreCase("https") || uri.getScheme().equalsIgnoreCase("http"))) {
                    throw new VaultException("Website addresses must begin with http:// or https://.");
                }
                if (uri.getHost() == null) throw new VaultException("Enter a complete website address.");
            } catch (IllegalArgumentException ex) {
                throw new VaultException("Enter a valid website address.");
            }
        }
        if (credential.username().length() > 320) throw new VaultException("The username or email is too long.");
        char[] copy = credential.password();
        try {
            if (copy.length == 0) throw new VaultException("Add a password for this credential.");
        } finally {
            java.util.Arrays.fill(copy, '\0');
        }
        if (credential.notes().length() > 4000) throw new VaultException("Notes must be 4,000 characters or fewer.");
        if (!java.util.Set.of("Social Media", "Education", "Finance", "Work", "Gaming", "Other").contains(credential.category())) {
            throw new VaultException("Choose a valid category.");
        }
    }

    public static boolean validMasterPassword(char[] password) {
        if (password == null || password.length < 12 || password.length > 1024) return false;
        boolean lower = false, upper = false, digit = false, symbol = false;
        for (char c : password) {
            if (Character.isLowerCase(c)) lower = true;
            else if (Character.isUpperCase(c)) upper = true;
            else if (Character.isDigit(c)) digit = true;
            else symbol = true;
        }
        return (lower && upper && digit) || (lower && upper && symbol) || (lower && digit && symbol) || (upper && digit && symbol);
    }
}
