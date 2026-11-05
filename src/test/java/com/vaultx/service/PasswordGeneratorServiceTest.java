package com.vaultx.service;

import com.vaultx.security.SecureRandomProvider;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PasswordGeneratorServiceTest {
    private final PasswordGeneratorService generator = new PasswordGeneratorService(new SecureRandomProvider());

    @Test
    void generatesMultiplePasswordsMeetingSelectedCharacterRequirements() {
        List<char[]> values = generator.generate(28, true, true, true, true, true, 5);
        assertEquals(5, values.size());
        assertTrue(values.stream().map(String::new).collect(java.util.stream.Collectors.toSet()).size() > 1);
        for (char[] value : values) {
            assertEquals(28, value.length);
            String password = new String(value);
            assertTrue(password.chars().anyMatch(Character::isUpperCase));
            assertTrue(password.chars().anyMatch(Character::isLowerCase));
            assertTrue(password.chars().anyMatch(Character::isDigit));
            assertTrue(password.chars().anyMatch(c -> !Character.isLetterOrDigit(c)));
            assertFalse(password.chars().anyMatch(c -> "Il1O0|`'\"".indexOf(c) >= 0));
            Arrays.fill(value, '\0');
        }
    }

    @Test
    void allowsAmbiguousCharactersWhenRequestedAndRejectsInvalidOptions() {
        List<char[]> values = generator.generate(128, true, false, false, false, false, 1);
        assertEquals(128, values.getFirst().length);
        assertThrows(IllegalArgumentException.class, () -> generator.generate(12, false, false, false, false, true, 1));
        assertThrows(IllegalArgumentException.class, () -> generator.generate(6, true, true, true, true, true, 1));
        assertThrows(IllegalArgumentException.class, () -> generator.generate(12, true, true, false, false, true, 11));
    }
}
