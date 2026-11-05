# VaultX development progress

## Environment and architecture

- Host: Linux x86_64, OpenJDK 27, Maven 3.10.0, Git 2.56.0. The Maven compiler targets Java 21, but this machine does not have a JDK 21 installation.
- Windows packaging must run on native Windows. Windows MSI and portable-image workflows are configured in `.github/workflows/windows-build.yml`.
- GitHub CLI recognizes account `CodezMorarity`, but its stored token is invalid. Repository creation/push cannot be verified from this session.
- The workspace root contains a harness-owned read-only `.git`; the project is isolated in `vaultx-password-manager/` so that metadata is not modified.
- Architecture recorded in `PRODUCT.md` and `docs/`: JavaFX view → services → prepared-statement repositories → SQLite; JCA security primitives isolated in `security/`.
- Credential rows contain a single AES-GCM encrypted serialized record. Master-password changes re-encrypt all entries inside a transaction. Backup uses a separate-password authenticated envelope.

## Implementation completed

- JavaFX screens: setup/unlock, dashboard, vault, generator, security overview, and settings.
- Services: authentication, AES-GCM, PBKDF2, credential CRUD, random password generation, strength estimates, clipboard timeout, and encrypted backup/restore.
- SQLite initialization and repositories; Windows/macOS/Linux per-user data paths.
- Shared CSS, SVG brand mark, generated PNG and Windows ICO application assets.
- Maven dependency and plugin versions pinned; Java 21 compiler target.
- PowerShell scripts for Windows tests, run, and package; CI and Windows package workflows.
- README, MIT license, security and contribution policies, changelog, technical docs, user guide, academic report, UML diagrams, 30 viva Q&A, and a five-minute demonstration script.

## Verification completed

- `mvn -Dmaven.repo.local=/tmp/vaultx-m2 -B -q clean test package` completed successfully on the Linux host (exit code 0). The project targets Java 21; this host's compiler runtime is OpenJDK 27, so native execution on JDK 21 is still a CI check.
- Current suite: 20 tests, 0 failures, 0 errors. Tests exercise KDF behavior, authentication, master-password re-encryption, GCM tamper rejection and nonce uniqueness, SQLite schema, credential CRUD/at-rest encryption, generator constraints, strength ratings, and backup integrity/import behavior.
- Maven produced `target/VaultX.jar` (12 MB) and test reports under `target/surefire-reports/`.
- `jpackage --type app-image` successfully created a Linux app image in `/tmp/vaultx-package-final`. A graphical launch could not be completed from the command-line sandbox because it has no `DISPLAY`; JavaFX screenshot capture did succeed through the UI capture harness.
- Captured and reviewed six actual JavaFX views with fictional records and a disposable database: setup, dashboard, vault, generator, security overview, and settings. The academic report embeds these screenshots; generator secrets are masked.
- Fixed the Windows script's JDK version check to match the version output format emitted by JDK 21.
- GitHub credential check failed because the configured token is invalid. No remote repository was created and no push was attempted.
- Initialized a project-local Git repository on `main`, added the expected `origin` URL, and committed the complete deliverable as `5a70b3a`. The harness-owned parent repository was left untouched. A final documentation commit records this verification state.

## Remaining external verification

- Native Windows execution of `scripts/build-windows.ps1` and a successful run of the Windows workflow before an MSI/portable artifact can be claimed.
- GitHub repository creation/push after valid GitHub write authentication is configured. The connected GitHub tools can inspect and edit existing repositories but do not expose repository creation; no matching user-owned repository exists.
