# Data and Privacy

DragonAltar stores configuration and gameplay state locally under the Paper server's `plugins/DragonAltar` directory.

It does not include:

- Telemetry
- Analytics
- Advertising identifiers
- An update checker
- Crash-report uploads
- Outbound web-service calls

Optional integrations use APIs already exposed inside the running Paper server. DragonAltar does not send their data to an outside service.

## Stored configuration

The six editable files can contain:

- Server mode and safety choices
- Eligibility and transfer rules
- Altar, fountain, crystal, pedestal, and protection coordinates
- Ritual offerings and presentation
- Ability, passive, resonance, sound, and particle settings
- Animation steps
- Localized MiniMessage text

## Stored runtime data

Runtime files can contain:

- Player UUIDs
- Accessibility and control settings
- Dragon Energy
- Selected ability
- Cooldown timestamps
- Dragonborn gain and loss history
- Soul holder and lifecycle state
- Soul history needed for recovery
- Ritual reservation and exact pending refund items
- Pending transfer state
- Event session and canonical entity references
- Altar state
- Limbo timers
- Fractured Soul entity, location, and teleport timing
- Mother Soul consequence state

Exact item refunds can include full Bukkit item metadata because the plugin must return the same consumed item.

## Operational files

DragonAltar also creates:

- `backups` for developer command snapshots
- `logs` for local operational auditing

Operational diagnostics can contain server and player information. Restrict these directories to trusted server owners.

## Persistence behavior

Runtime writes are serialized through one asynchronous queue. Each save writes a temporary file, then replaces the destination. Shutdown flushes queued state.

Do not edit data YAML while Paper is running. Hand edits can race queued writes, break state invariants, or invalidate restart recovery.

## Public views

Approved player-facing views are:

- `/dragon status`
- `/dragon history`
- Player menus and HUD
- Privacy-safe API snapshots
- Carefully scoped placeholders

The public API maps soul state to Held, Dormant, Limbo, or Fractured. When holder information should not be public, the snapshot reports Dormant with no holder. Public history uses neutral maintenance descriptions and omits private operational provenance.

Do not try to reverse that redaction by combining diagnostics, data files, permission state, placeholders, or logs.

PlaceholderAPI's 1.4.19 expansion reads live state directly. Keep its player-scoped placeholders in the subject's own HUD and use the public API snapshots for shared dashboards.

## Retention and deletion

The server owner controls:

- Retention period
- Backup schedule
- Filesystem permissions
- Staff access
- Support disclosure
- Deletion after a player-data request, subject to the server's legal obligations

DragonAltar does not provide a one-player purge command. Player UUIDs can appear across current ownership, historical recovery, cooldown, ritual, and consequence files. A safe deletion requires:

1. Stop Paper.
2. Take a full private backup.
3. Determine whether the player currently holds or participates in recoverable state.
4. Reassign or safely resolve that state through supported commands first.
5. Review all DragonAltar data with the server owner's privacy process.
6. Restart and validate soul, event, ritual, cooldown, refund, and consequence state.

Do not remove a UUID from one file while leaving a live reservation, holder, limbo, refund, or cooldown reference elsewhere.

## Sharing support material

Before sharing anything:

- Remove player names and UUIDs
- Remove operational audit detail
- Remove server paths
- Remove IP addresses
- Remove hostnames and network details
- Remove plugin license keys or unrelated secrets
- Include only the smallest relevant excerpt

Prefer `/dragon system validate`, `/dragon altar validate`, and a clean reproduction description over a full data dump.

DragonAltar is owned by Silverfox0338.
