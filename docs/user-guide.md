# VaultX user guide

## First launch and unlock

On first launch, choose and confirm a strong master password. VaultX requires at least 12 characters and diverse character types, and rejects common patterns using an educational local heuristic. The master password cannot be recovered if forgotten. Later launches show the unlock screen; a wrong password leaves the vault locked.

## Dashboard and vault

The dashboard shows counts, favorites, categories, a short recently-added list, and a high-level strength summary. It never renders saved passwords. Choose **Password Vault** to add, edit, search, filter by category, favorite, reveal temporarily, or copy usernames and passwords. Deletion asks for confirmation. Revealed passwords are hidden again when the row is refreshed or the vault is locked.

Use the search field to filter entries by account title, username, or category. Website URL, category, and notes can be edited with a record. Categories include Social Media, Education, Finance, Work, Gaming, and Other.

## Password generator

Select a length, character groups, whether to exclude ambiguous characters, and how many variations to create. At least one character group is required; the generator guarantees at least one character from each selected group and securely shuffles the result. Copy a selected variation when needed. The clipboard clears after the configured timeout if the clipboard still contains the value VaultX placed there.

## Security overview

The security screen summarizes the number of records rated weak through very strong. Ratings are heuristic and are not checked against online breach lists. Use the generator for new passwords and avoid reusing passwords across sites.

## Settings

- Choose the automatic lock delay and clipboard clear delay.
- Select the dark or light theme.
- Change the master password after confirming the current one. VaultX decrypts and re-encrypts every record before committing the new key metadata.
- Export an encrypted backup using a separate strong password. Import asks for that backup password, validates all records first, and skips IDs already in the vault.
- Lock immediately or exit. Locking clears the in-memory key and owned clipboard value.

Store the backup password separately. Losing both the master password and all usable backups means the data cannot be recovered.
