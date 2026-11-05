# Database schema

The database is stored in the current user's application data directory (`%LOCALAPPDATA%\VaultX` on Windows). SQLite is initialized automatically and uses foreign keys and a busy timeout.

```sql
CREATE TABLE vault_metadata (
  id INTEGER PRIMARY KEY CHECK (id = 1),
  kdf_salt BLOB NOT NULL,
  verifier BLOB NOT NULL,
  iterations INTEGER NOT NULL,
  schema_version INTEGER NOT NULL,
  created_at TEXT NOT NULL
);

CREATE TABLE credentials (
  id TEXT PRIMARY KEY,
  created_at TEXT NOT NULL,
  modified_at TEXT NOT NULL,
  nonce BLOB NOT NULL,
  ciphertext BLOB NOT NULL
);

CREATE TABLE application_settings (
  setting_key TEXT PRIMARY KEY,
  setting_value TEXT NOT NULL
);
```

`credentials.id` is a random UUID used as AES-GCM associated data. The nonce and authenticated ciphertext/tag are separate BLOBs. All user-supplied credential fields, including category and favorite state, are serialized into the encrypted payload. The timestamps are stored separately for ordering and remain metadata visible to a local database reader.

`vault_metadata` is a single-row table. It stores the PBKDF2 salt, derived verifier, iteration count, and schema version; it never stores the master password or AES key. Settings are non-secret UI preferences.

Database creation and schema initialization run through `DatabaseInitializer`. Repository queries use prepared statements. Master-password changes replace credential ciphertext and metadata in a single transaction.
