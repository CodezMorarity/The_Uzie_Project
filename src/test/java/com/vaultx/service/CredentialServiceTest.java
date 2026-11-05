package com.vaultx.service;

import com.vaultx.TestVault;
import com.vaultx.exception.VaultException;
import com.vaultx.model.Credential;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CredentialServiceTest {
    @TempDir Path temp;

    @Test
    void performsEncryptedCredentialCrudAndKeepsSensitiveFieldsOutOfDatabaseColumns() throws Exception {
        try (TestVault vault = new TestVault(temp.resolve("vault.db"))) {
            vault.setup();
            Credential credential = new Credential(UUID.randomUUID(), "Example Mail", "https://mail.example", "person@example.test",
                    "Unique!MailSecret84".toCharArray(), "Work", "recovery note", Instant.now(), Instant.now(), false);
            vault.credentials.add(credential);
            assertEquals(1, vault.credentials.count());

            try (var connection = vault.database.connect();
                 var query = connection.prepareStatement("SELECT nonce, ciphertext FROM credentials WHERE id = ?")) {
                query.setString(1, credential.id().toString());
                try (var result = query.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(12, result.getBytes(1).length);
                    byte[] ciphertext = result.getBytes(2);
                    String stored = new String(ciphertext, java.nio.charset.StandardCharsets.ISO_8859_1);
                    assertFalse(stored.contains("Example Mail"));
                    assertFalse(stored.contains("person@example.test"));
                    assertFalse(stored.contains("Unique!MailSecret84"));
                    Arrays.fill(ciphertext, (byte) 0);
                }
            }

            var loaded = vault.credentials.findAll();
            assertEquals(1, loaded.size());
            assertEquals("Example Mail", loaded.getFirst().name());
            assertEquals("Work", loaded.getFirst().category());
            char[] original = loaded.getFirst().password();
            assertEquals("Unique!MailSecret84", new String(original));
            Arrays.fill(original, '\0');
            Credential updated = loaded.getFirst().edited("Example Mail", "https://mail.example", "person@example.test",
                    "Fresh!MailSecret85".toCharArray(), "Finance", "updated", true);
            vault.credentials.update(updated);
            loaded.forEach(Credential::close); updated.close(); credential.close();

            var afterUpdate = vault.credentials.findAll();
            assertEquals("Finance", afterUpdate.getFirst().category());
            assertTrue(afterUpdate.getFirst().favorite());
            assertTrue(vault.credentials.delete(afterUpdate.getFirst().id()));
            afterUpdate.forEach(Credential::close);
            assertEquals(0, vault.credentials.count());
        }
    }

    @Test
    void rejectsInvalidWebsiteBeforeWriting() {
        try (TestVault vault = new TestVault(temp.resolve("vault.db"))) {
            vault.setup();
            Credential invalid = new Credential(UUID.randomUUID(), "Bad URL", "javascript:alert(1)", "person",
                    "Unique!Password884".toCharArray(), "Other", "", Instant.now(), Instant.now(), false);
            assertThrows(VaultException.class, () -> vault.credentials.add(invalid));
            assertEquals(0, vault.credentials.count());
            invalid.close();
        }
    }
}
