package com.vaultx.service;

import com.vaultx.model.StrengthLevel;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class PasswordStrengthServiceTest {
    private final PasswordStrengthService strength = new PasswordStrengthService();

    @Test
    void flagsCommonAndShortPasswordsAsWeak() {
        assertEquals(StrengthLevel.WEAK, strength.evaluate("password".toCharArray()).level());
        assertEquals(StrengthLevel.WEAK, strength.evaluate("Ab1!".toCharArray()).level());
    }

    @Test
    void ratesLongVariedPassphraseStrongAndAddsEducationalDisclaimer() {
        char[] password = "Saffron#Bridge47!".toCharArray();
        var result = strength.evaluate(password);
        assertTrue(result.level() == StrengthLevel.STRONG || result.level() == StrengthLevel.VERY_STRONG);
        assertTrue(result.feedback().stream().anyMatch(message -> message.contains("not a formal security audit")));
        Arrays.fill(password, '\0');
    }

    @Test
    void detectsSequencesAndRepeatedCharacters() {
        assertTrue(strength.evaluate("Abcdefgh123!".toCharArray()).feedback().stream().anyMatch(item -> item.contains("sequences")));
        assertTrue(strength.evaluate("Aaaaaaaaaaaa1!".toCharArray()).feedback().stream().anyMatch(item -> item.contains("repeating")));
    }
}
