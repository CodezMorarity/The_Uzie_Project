package com.vaultx.security;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class KeyDerivationTest {
    private final KeyDerivation keyDerivation = new KeyDerivation();

    @Test
    void derivesRepeatable512BitOutputForSamePasswordAndSalt() {
        char[] password = "Saffron#Bridge47!".toCharArray();
        byte[] salt = new byte[16]; Arrays.fill(salt, (byte) 7);
        byte[] first = keyDerivation.derive(password, salt, 10_000);
        byte[] second = keyDerivation.derive(password, salt, 10_000);
        assertEquals(64, first.length);
        assertArrayEquals(first, second);
        KeyDerivation.clear(first); KeyDerivation.clear(second); KeyDerivation.clear(salt); Arrays.fill(password, '\0');
    }

    @Test
    void differentSaltProducesDifferentOutput() {
        char[] password = "Saffron#Bridge47!".toCharArray();
        byte[] firstSalt = new byte[16]; byte[] secondSalt = new byte[16]; secondSalt[0] = 1;
        byte[] first = keyDerivation.derive(password, firstSalt, 10_000);
        byte[] second = keyDerivation.derive(password, secondSalt, 10_000);
        assertFalse(Arrays.equals(first, second));
        KeyDerivation.clear(first); KeyDerivation.clear(second); Arrays.fill(password, '\0');
    }

    @Test
    void rejectsWeakParameters() {
        assertThrows(IllegalArgumentException.class, () -> keyDerivation.derive("x".toCharArray(), new byte[15], 10_000));
        assertThrows(IllegalArgumentException.class, () -> keyDerivation.derive("x".toCharArray(), new byte[16], 9_999));
    }
}
