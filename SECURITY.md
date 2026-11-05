# Security policy

VaultX is an educational project and has not received an independent audit. Do not treat it as a substitute for a mature, audited password manager when storing high-value credentials.

## Reporting a vulnerability

Do not post a sensitive exploit or private vault data in a public issue. Contact the repository maintainer privately through the GitHub security advisory feature once this project has a published repository. Until then, share a minimal reproduction without real credentials through the project owner’s trusted channel.

## Security model and limits

- The database is local. Credential metadata and secret fields are encrypted together with AES-256-GCM.
- The master password is converted with PBKDF2-HMAC-SHA256 and a unique random salt. Only the salt, work factor, and derived verifier are persisted.
- The AES key exists in process memory while unlocked. Clipboard contents, screen capture, malware, keyloggers, memory inspection, and a compromised operating system are outside the protection boundary.
- Best-effort clearing of Java arrays does not guarantee erasure from heap copies, swap, crash dumps, or hardware.
- Password strength is an educational heuristic, not a formal entropy or breach check. The app performs no network lookups.

## Backups

Backup files are encrypted with a separate user-supplied password. Store that password separately from the backup. Anyone who obtains both can restore the records. Keep at least one backup in a secure location and test restore using a copy.
