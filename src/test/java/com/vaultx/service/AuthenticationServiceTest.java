package com.vaultx.service;

import com.vaultx.TestVault;
import com.vaultx.exception.AuthenticationException;
import com.vaultx.model.Credential;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationServiceTest {
    @TempDir Path temp;

    @Test
    void setupStoresOnlySaltAndVerifierAndPasswordArrayIsCleared() throws Exception {
        try (TestVault vault = new TestVault(temp.resolve("vault.db"))) {
            char[] master = "Saffron#Bridge47!".toCharArray();
            vault.authentication.initialize(master);
            assertTrue(vault.authentication.isConfigured());
            assertTrue(vault.authentication.isUnlocked());
            assertTrue(Arrays.equals(new char[master.length], master));
            byte[] db = Files.readAllBytes(vault.database.databasePath());
            assertFalse(new String(db, java.nio.charset.StandardCharsets.ISO_8859_1).contains("Saffron#Bridge47!"));
        }
    }

    @Test
    void verifiesMasterPasswordAndUnlocksOnlyAfterCorrectPassword() {
        try (TestVault vault = new TestVault(temp.resolve("vault.db"))) {
            vault.setup(); vault.authentication.lock();
            assertThrows(AuthenticationException.class, () -> vault.authentication.unlock("Wrong!Password77".toCharArray()));
            assertFalse(vault.authentication.isUnlocked());
            vault.authentication.unlock("Saffron#Bridge47!".toCharArray());
            assertTrue(vault.authentication.isUnlocked());
        }
    }

    @Test
    void changingMasterPasswordReencryptsExistingCredentialPayloads() {
        try (TestVault vault = new TestVault(temp.resolve("vault.db"))) {
            vault.setup();
            Credential credential = new Credential(UUID.randomUUID(), "Mail", "https://mail.example", "person@example.test",
                    "M!ailS3cret#Wide".toCharArray(), "Work", "primary inbox", Instant.now(), Instant.now(), false);
            vault.credentials.add(credential); credential.close();
            vault.authentication.changeMasterPassword("Saffron#Bridge47!".toCharArray(), "Cobalt&Meadow84!".toCharArray());
            assertTrue(vault.authentication.isUnlocked());
            vault.authentication.lock();
            assertThrows(AuthenticationException.class, () -> vault.authentication.unlock("Saffron#Bridge47!".toCharArray()));
            vault.authentication.unlock("Cobalt&Meadow84!".toCharArray());
            var loaded = vault.credentials.findAll();
            assertEquals(1, loaded.size());
            assertEquals("Mail", loaded.getFirst().name());
            char[] password = loaded.getFirst().password();
            assertEquals("M!ailS3cret#Wide", new String(password));
            Arrays.fill(password, '\0'); loaded.forEach(Credential::close);
        }
    }
}
