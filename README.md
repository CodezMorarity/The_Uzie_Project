# VaultX — Secure Password Manager

VaultX is an offline-first Java desktop password manager. It keeps credential records in a local SQLite database and encrypts each complete record with AES-256-GCM. The master password is never stored: PBKDF2-HMAC-SHA256 derives a verifier and vault key from a random salt.

> **Academic project:** VaultX demonstrates application security concepts; it has not undergone an independent security audit. It cannot protect a password from malware, a keylogger, screen capture, or an already compromised host. Keep the master password safe and maintain encrypted backups.

## Features

- First-run master-password setup and authenticated unlock with an inactivity lock.
- Encrypted create, edit, delete, search, category filtering, favorites, username/password copy, and temporary reveal.
- Secure password generator with selectable character groups, ambiguity exclusion, and multiple results.
- Educational strength estimator with a local common-pattern list.
- Dashboard counts and recent-account summary without showing stored passwords.
- Dark and light themes, clipboard clearing, lock timeout, master-password change, and secure exit.
- Encrypted backup export and import with a separate password; import validates before inserting and skips existing IDs.
- Offline operation after distribution has been built.

## Technology

| Component | Pinned version |
| --- | --- |
| Java target | 21 |
| JavaFX | 21.0.13 |
| Maven Compiler Plugin | 3.14.1 |
| SQLite JDBC | 3.54.1.0 |
| SLF4J API / Simple | 2.0.20 |
| JUnit Jupiter | 5.14.4 |

See [`docs/architecture.md`](docs/architecture.md), [`docs/security-design.md`](docs/security-design.md), and [`docs/database-schema.md`](docs/database-schema.md) for implementation details.

## Run from source

Prerequisites: a 64-bit JDK 21 and Maven 3.9 or later. Internet access is needed once to fetch Maven dependencies; the installed desktop app does not need network access.

```bash
mvn clean test package
mvn javafx:run
```

On Linux, JavaFX needs a graphical desktop session. On Windows, prefer the self-contained release package; no separate Java or Maven configuration is needed for its users.

## Windows build and install

Build on a 64-bit Windows 10/11 host with JDK 21, Maven 3.9+, and WiX Toolset 3.0+ on `PATH` for the MSI. The project’s [Windows workflow](.github/workflows/windows-build.yml) builds on `windows-latest` and publishes the MSI and portable app image as workflow artifacts. Run:

```powershell
scripts\build-windows.ps1
```

The script checks prerequisites, executes the test suite, builds the app, then creates `target\dist\VaultX-1.0.0.msi` and `target\dist\VaultX-portable.zip`. Installation and data-location details are in [`docs/windows-installation.md`](docs/windows-installation.md).

## Data and security

The database is created under `%LOCALAPPDATA%\VaultX\vaultx.db` on Windows, `~/Library/Application Support/VaultX` on macOS, or `$XDG_DATA_HOME/vaultx` / `~/.local/share/vaultx` on Linux. Every credential record, including its title, username, URL, notes, and password, is encrypted as a single authenticated payload. Metadata stores only the random salt, derived verifier, KDF work factor, schema version, and setup time. SQLite settings contain non-secret preferences.

Production PBKDF2 uses 600,000 iterations and a 16-byte random salt. AES-GCM uses a fresh 12-byte nonce per encryption and a 128-bit authentication tag. Java arrays are cleared when practical, but garbage collection and the operating system do not guarantee complete erasure from memory. See [SECURITY.md](SECURITY.md) for reporting and backup guidance.

## Tests

```bash
mvn test
```

Tests cover KDF behavior, setup and unlock, re-encryption after master-password change, authenticated encryption and tamper rejection, unique nonces, database initialization, encrypted credential CRUD, generator constraints, strength scoring, and backup import/export. Test KDF rounds are reduced to 10,000 to keep the suite practical; production remains at 600,000.

## Project files

- `src/main/java/com/vaultx/` — JavaFX app, services, repositories, security, and database.
- `src/test/java/` — JUnit 5 tests.
- `scripts/` — Windows build, test, and launch scripts.
- `docs/` — technical design, installation, user guide, report, and viva preparation.
- `.github/workflows/` — cross-platform CI and native Windows packaging.

## License

MIT; see [`LICENSE`](LICENSE).
