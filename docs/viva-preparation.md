# Viva preparation and five-minute demonstration

## Likely viva questions

1. **What problem does VaultX solve?** It provides an offline local workflow for securely organizing account credentials and generating unique passwords.
2. **Why use JavaFX?** It is Java’s desktop UI toolkit and provides reusable controls, layouts, CSS styling, and event handling.
3. **Why use SQLite?** It is a small embedded relational database with no server process or account requirement.
4. **What does JDBC do?** JDBC is the Java API used to connect to SQLite, execute prepared SQL, and map result rows.
5. **What is OOP in this project?** Responsibilities are split among models, views, services, repositories, and crypto classes with encapsulated state.
6. **Where is the master password stored?** It is not stored. A derived verifier and salt are stored for password verification.
7. **What is PBKDF2?** A password-based key derivation function that repeatedly applies a pseudorandom function to a password and salt to make guessing more expensive.
8. **Why use a salt?** A unique random salt makes identical passwords derive different values and defeats precomputed rainbow tables.
9. **Why use 600,000 PBKDF2 iterations?** It is a deliberately costly work factor aligned with current OWASP PBKDF2-HMAC-SHA256 guidance; it should be benchmarked on supported target hardware.
10. **What is the verifier used for?** The first derived 32 bytes are compared in constant time with the stored verifier; the password itself is never compared or persisted.
11. **How is the encryption key produced?** The other 32 bytes of PBKDF2 output become the AES-256 vault key for the unlocked session.
12. **What is AES-GCM?** An authenticated encryption mode that encrypts data and produces a tag used to detect tampering.
13. **Why AES-256-GCM?** It provides confidentiality and integrity in one standard mode supported by JCA.
14. **What is a nonce?** A unique per-key value supplied to GCM. VaultX uses a fresh 12-byte random nonce for each encryption and stores it beside ciphertext.
15. **What happens when ciphertext changes?** GCM tag verification fails, and decryption is rejected.
16. **What is AAD?** Additional authenticated data is not encrypted but is included in integrity protection; VaultX binds each record UUID to its ciphertext.
17. **Why encrypt whole credential records?** It hides titles, URLs, usernames, categories, notes, and passwords from SQLite inspection.
18. **How does login work?** PBKDF2 derives a candidate verifier and key from the entered password and stored salt; a constant-time verifier comparison gates unlock.
19. **How does password change work?** The old password is reverified, records are decrypted and re-encrypted with a fresh salt-derived key, and repository replacement updates rows and metadata in one transaction.
20. **How are SQL injection risks reduced?** Repository operations bind values through `PreparedStatement` rather than concatenating user input into SQL.
21. **What is a JavaFX background task for?** Costly KDF, backup, and database operations run away from the UI thread so the window can remain responsive.
22. **How does the generator use randomness?** It draws from `SecureRandom`, ensures each selected character group appears, then shuffles the output.
23. **What does the strength checker evaluate?** Length, character variety, repeated characters, common patterns, and a small local common-password list.
24. **Is the strength result a security guarantee?** No. It is an educational heuristic without a formal audit or online breach check.
25. **How does automatic locking work?** User interaction updates the session activity time; a timer locks and clears session key material after the configured idle period.
26. **How does clipboard clearing avoid deleting unrelated data?** VaultX compares the clipboard value it placed with the current clipboard contents before clearing.
27. **How are backups encrypted?** A separate password is derived with PBKDF2 and a backup-specific salt; AES-GCM authenticates the header and encrypted payload.
28. **How is an import kept from overwriting existing records?** The file is decrypted and validated before persistence; inserts use IDs and skip conflicts instead of replacing them.
29. **What does `AutoCloseable` contribute?** It supports explicit cleanup in try-with-resources and orderly session/resource shutdown.
30. **What are VaultX’s limits?** It is not independently audited and cannot protect against malware, keyloggers, a compromised host, forgotten passwords, or all JVM memory copies.

## Five-minute demonstration

Use fictitious data only. Have a test profile ready and never display a real password on a projector.

| Time | Action | Talking point |
| --- | --- | --- |
| 0:00–0:35 | Start VaultX and show first-run setup or unlock. | The master password is never stored; PBKDF2 produces a verifier and a vault key. |
| 0:35–1:20 | Open the dashboard and point out totals, favorites, recent items, and strength overview. | Dashboard summaries never reveal stored passwords. |
| 1:20–2:15 | Add a fictional account, search/filter it, reveal briefly, copy username, then hide it. | Credential fields are encrypted together before SQLite persistence. |
| 2:15–3:00 | Open the generator, choose length and character sets, generate a few alternatives, copy one. | `SecureRandom` and group guarantees avoid weak predictable output. |
| 3:00–3:45 | Show Security Overview and explain educational ratings; show inactivity/clipboard settings. | Ratings are local heuristics; clipboard clearing only removes VaultX’s unchanged value. |
| 3:45–4:35 | Export an encrypted backup using a separate demonstration password; show the file exists. | Backup format is versioned and authenticated with AES-GCM. Do not expose the password. |
| 4:35–5:00 | Lock the vault and show the unlock screen. | Locking discards the session key reference; host malware and memory copies remain outside scope. |

## Practical explanation of crypto flow

```text
Master password + random 16-byte salt
        -> PBKDF2-HMAC-SHA256 (600,000 rounds, 64 bytes)
        -> 32-byte verifier + 32-byte AES key
        -> verifier comparison permits unlock
        -> AES-256-GCM with a fresh 12-byte nonce encrypts each record
        -> nonce + ciphertext/tag are persisted in SQLite
```

Java array clearing is best-effort only. Never claim JVM garbage collection guarantees memory erasure.
