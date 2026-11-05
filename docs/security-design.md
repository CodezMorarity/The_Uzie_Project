# Security design

## Threat boundary

VaultX protects credential records at rest in its local SQLite database and encrypted backups. It does not claim protection from a compromised operating system, malware, screen capture, keyloggers, an attacker observing an unlocked session, or memory inspection while credentials are in use. Java garbage collection does not guarantee erasure of old object contents.

## Master password and key derivation

PBKDF2WithHmacSHA256 uses a unique 16-byte `SecureRandom` salt and 600,000 iterations. The iteration count is stored with metadata so future versions can migrate it. A 64-byte output is split into a 32-byte verification value and a 32-byte AES key. Only the salt, iteration count, and verification value are persisted. Login compares candidate and stored verification bytes with `MessageDigest.isEqual`. Password character arrays and the temporary derived buffer are cleared in `finally` blocks where practical.

## Vault encryption

Credential payloads use AES/GCM/NoPadding with a 256-bit session key, a 96-bit random nonce, and a 128-bit authentication tag. Every encryption call requests a new nonce from `SecureRandom`; nonce and ciphertext are stored in distinct SQLite BLOB columns. The record UUID is authenticated as associated data. Any tag failure is treated as tampering or corruption and the record is not returned.

The database contains only record identifiers, timestamps, salts/verifiers, non-sensitive settings, and encrypted payload bytes. Names, URLs, usernames, passwords, categories, notes, and favorite flags are inside the encrypted payload.

## Session and clipboard

The session holds the AES key only while unlocked. Lock and exit clear the session's key bytes and clear any clipboard value still owned by VaultX. Clipboard timeout checks a digest of the copied value; if the clipboard has changed, VaultX leaves it untouched. JavaFX and the operating system necessarily make transient string copies for clipboard and text controls.

## Backup

The binary `VXBACKUP` envelope contains a version, KDF iteration count, random 16-byte salt, random 12-byte nonce, and AES-GCM ciphertext/tag. PBKDF2-HMAC-SHA256 derives its independent 256-bit backup key from a separate backup password. Header bytes are authenticated as AAD. A backup import decrypts and validates every entry in memory before starting the database transaction. Existing record identifiers are skipped; records are never overwritten by import.

## Secure coding

All SQL values use `PreparedStatement`. Credential content is encrypted before repository insertion. Passwords and keys are never logged. The password generator uses `SecureRandom` and forces at least one character from each selected pool. User-facing strength results are educational estimates, not formal audits.

## Limitations

The authentication verifier and salt are stored beside the database, so an offline attacker can attempt guesses; PBKDF2 raises the cost but cannot compensate for a weak master password. VaultX does not protect an unlocked device from malware or a compromised OS. Clipboard clearing cannot guarantee operating-system clipboard history or synchronization is erased. Java cannot promise complete memory erasure.
