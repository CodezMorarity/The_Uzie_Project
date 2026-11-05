# Product

<!-- impeccable:product-schema 1 -->

## Stack

Java 21, JavaFX 21, Maven, SQLite through JDBC, JCA cryptography, JUnit 5, and SLF4J as specified by the project brief.

## Users

The primary user is an individual who needs to store and retrieve account credentials on one computer without relying on cloud services. A secondary audience is the university evaluator reviewing the project's Java architecture and security implementation.

## Product Purpose

VaultX stores, organizes, generates, and retrieves credentials in a local encrypted vault. Success means a user can set a master password, unlock the vault, perform common credential tasks, and securely back up or restore the vault while offline.

## Positioning

VaultX is an offline-first desktop vault whose credential records are encrypted locally with a key derived from the user's master password. It does not require a cloud account or remote authentication service.

## Operating Context

VaultX is used as a Java desktop application on Windows 10 or Windows 11, with Linux used as a development environment when applicable. Users move between a dashboard, vault, password generator, security overview, and settings. Installation should not require a user-managed JavaFX runtime or Maven.

## Capabilities and Constraints

- The application must work offline after installation.
- Credentials support a name, URL, username, password, category, notes, timestamps, and favorite status.
- The vault uses SQLite and AES-256-GCM; master-password derivation uses PBKDF2WithHmacSHA256.
- The master password is never stored in plaintext.
- Encrypted backups use a separate backup password.
- Windows 10/11 x64 packaging is a release requirement.
- Host malware and keyloggers are outside the application's protection boundary.
- Product scope and feature requirements are based on the project brief; no external customer research or production security audit is available.

## Brand Commitments

The product name is VaultX. The requested identity is a premium, minimal cybersecurity product with a dark graphite base, silver/white typography, and an emerald accent.

## Evidence on Hand

The project brief is the source of product requirements. No user research, production usage data, or existing brand assets were supplied. Do not invent adoption, performance, or security-audit claims.

## Product Principles

- Keep vault data local and usable offline.
- Protect stored values before they reach persistent storage.
- Make routine credential tasks direct and understandable.
- Explain educational security estimates and their limits.
- Keep the implementation clear enough to explain in an academic practical examination.
