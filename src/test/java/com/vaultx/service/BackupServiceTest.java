package com.vaultx.service;

import com.vaultx.TestVault;
import com.vaultx.exception.VaultException;
import com.vaultx.model.Credential;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BackupServiceTest {
    @TempDir Path temp;

    @Test
    void exportsAndImportsEncryptedBackupAndSkipsExistingIds() throws Exception {
        try (TestVault vault = new TestVault(temp.resolve("vault.db"))) {
            vault.setup();
            Credential credential = new Credential(UUID.randomUUID(), "Campus", "https://campus.example", "student@example.test",
                    "Campus!Private84".toCharArray(), "Education", "portal", Instant.now(), Instant.now(), true);
            vault.credentials.add(credential);
            Path backupPath = temp.resolve("vault.vxbackup");
            vault.backups.exportBackup(java.util.List.of(credential), "VioletQuartz#Raven726!".toCharArray(), backupPath);
            assertTrue(Files.size(backupPath) > 44);
            byte[] backupBytes = Files.readAllBytes(backupPath);
            assertFalse(new String(backupBytes, java.nio.charset.StandardCharsets.ISO_8859_1).contains("Campus!Private84"));

            var restored = vault.backups.importBackup(backupPath, "VioletQuartz#Raven726!".toCharArray());
            assertEquals(1, restored.size());
            assertEquals("Campus", restored.getFirst().name());
            assertEquals(0, vault.credentials.importNew(restored));
            assertEquals(1, vault.credentials.count());
            restored.forEach(Credential::close); credential.close();
        }
    }

    @Test
    void rejectsTamperedBackupAndWrongPasswordBeforeDatabaseChanges() throws Exception {
        try (TestVault vault = new TestVault(temp.resolve("vault.db"))) {
            vault.setup();
            Credential credential = new Credential(UUID.randomUUID(), "Social", "https://social.example", "person",
                    "Different!Secret842".toCharArray(), "Social Media", "", Instant.now(), Instant.now(), false);
            Path backupPath = temp.resolve("clean.vxbackup");
            vault.backups.exportBackup(java.util.List.of(credential), "JadeHarbor&Orbit851!".toCharArray(), backupPath);
            Path tampered = temp.resolve("tampered.vxbackup");
            byte[] bytes = Files.readAllBytes(backupPath); bytes[bytes.length - 1] ^= 1; Files.write(tampered, bytes);
            assertThrows(VaultException.class, () -> vault.backups.importBackup(tampered, "JadeHarbor&Orbit851!".toCharArray()));
            assertThrows(VaultException.class, () -> vault.backups.importBackup(backupPath, "WrongVaultKey&Orchid97!".toCharArray()));
            assertEquals(0, vault.credentials.count());
            Arrays.fill(bytes, (byte) 0); credential.close();
        }
    }

    @Test
    void requiresUnlockedSessionForExport() {
        try (TestVault vault = new TestVault(temp.resolve("vault.db"))) {
            assertThrows(com.vaultx.exception.AuthenticationException.class,
                    () -> vault.backups.exportBackup(java.util.List.of(), "SomeBackup#Pass99!".toCharArray(), temp.resolve("blocked.vxbackup")));
        }
    }
}
