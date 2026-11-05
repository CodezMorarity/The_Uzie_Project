# UI direction contract

## Product and task

VaultX is a locally hosted password manager for people who need to retrieve credentials without a cloud account. The interface must make lock state, search, category filtering, and the next credential action obvious while keeping secret values out of the dashboard.

## Chosen world: Graphite key ledger

The user's explicit visual brief governs the build: a dark graphite workspace with quiet silver typography and emerald reserved for primary actions, active navigation, and positive security state. A shield-and-key mark identifies the product. The interface behaves like a dependable desktop tool, not a security-themed marketing page.

## First screen

The dashboard opens with a small welcome line, three compact vault metrics, a recently added list, and a password-health summary. A persistent left navigation rail exposes Dashboard, Password Vault, Password Generator, Security Overview, and Settings. A top-level global search opens the vault with the query applied. No stored secret appears on the dashboard.

## Visitor path

First-time users create and confirm a master password, then land in the dashboard. Returning users unlock the vault. From the sidebar they can add or search credentials, generate a password, inspect aggregate security signals, or manage preferences and encrypted backups. Every sensitive action gives clear success or failure feedback.

## Signature interaction

Credential rows reveal actions only when focused or hovered. A selected row opens its detail panel, where password visibility and copying are explicit. Search and category filtering update the list in place. Copy feedback is short and the owned clipboard value is cleared after the configured timeout.

## Cross-screen rules

Cards use a second graphite surface, subtle hairline dividers, and consistent corner radii. Type uses a single readable system sans family with a compact label scale. Emerald marks the current destination and primary action; red, amber, and neutral colors communicate actual state. Controls keep familiar desktop affordances and visible keyboard focus.

## Decision record

The random concept seed assigned a warm near-white design annual plate direction. That assignment could not supersede the user's binding dark graphite, silver, and emerald direction. The concept remains a useful information-hierarchy reference, but its off-white ground and periwinkle seal conflict with the brief.

The strongest compatible challenger was the boarding-pass and gate-board pattern. It was competitive for a dense, sortable credential list, but the travel-document metaphor would reduce product clarity. The build keeps its discipline of stable table columns and clear changed/focused row state, without its metaphor, scannable ticket motif, live reranking animation, or non-product data. The remaining challengers were declined because their own visual grammars either conflict with the explicit palette or make password-management tasks harder to recognize:

- **Ikeda data field:** high-contrast barcode and sine-wave graphics are decorative and compete with credential scanning; no pattern or strobe is retained.
- **Crouwel grid specimen:** visible construction grids and monumental letters compete with compact tool controls; grid alignment survives as ordinary layout discipline only.
- **Convention circle catalog:** postage-stamp cards and hall-map navigation are unsuitable for locating credential records; no map or tiny thumbnail grid is retained.
- **Design annual plate:** warm stock and a periwinkle seal conflict with the requested dark emerald identity; hairline separators and calm label hierarchy are retained.
- **Airline timetable slide rack:** one-size type, raked glass panels, and goldenrod ground harm readable data entry; no material or geometry is retained.

The product brief's fixed identity and task requirements are the approval for this direction. The build uses a code-first implementation with the contract above as its visual bar.
