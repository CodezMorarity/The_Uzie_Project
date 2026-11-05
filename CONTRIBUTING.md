# Contributing

Thanks for helping improve VaultX. Keep changes small, explain security-sensitive decisions, and preserve the offline-first design.

## Development setup

- Use a 64-bit JDK 21 and Maven 3.9+.
- Clone the repository and run `mvn clean test package` before changing code.
- Run `mvn javafx:run` to start the desktop application in a graphical session.

## Change guidelines

- Keep UI, service, repository, and security responsibilities separated.
- Use prepared statements for SQL and keep credential payloads encrypted at rest.
- Do not add telemetry, remote calls, or plaintext credential logging.
- Add JUnit tests for behavior changes, especially security boundaries.
- Do not commit local databases, backups, keys, passwords, generated build output, or IDE state.
- Update the user guide or design documents when behavior or formats change.

## Pull requests

Describe the problem, implementation, and security impact. Include test output and screenshots of UI changes when available. A change that alters encryption or the backup envelope should include compatibility and migration notes.
