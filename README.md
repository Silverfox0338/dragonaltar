# DragonAltar

DragonAltar is a Paper plugin for an SMP-wide Ancient Dragon storyline. It runs a protected vanilla Ender Dragon respawn ritual, awakens a configured altar, creates exactly three persistent Dragon Souls, and keeps Akuma, Rev, and Lamari circulating through rituals, inheritance, recovery, and reincarnation.

DragonAltar 1.4.18 targets Paper 1.21.4 and Java 21.

## Highlights

- Exactly three named Dragonborn: Akuma, Rev, and Lamari
- Persistent soul ownership, lineage, transfers, pending recovery, limbo, and fractured-soul recovery
- A one-time Ancient Dragon event based on Paper's vanilla respawn API
- Initial and forced-removal rituals with exact-item refunds and restart recovery
- Dragon Energy, passives, movement, combat abilities, ultimates, and multi-Dragonborn resonances
- Persistent shared ultimate and resonance cooldowns
- Per-player HUD, sound, title, screen-effect, particle, and minimal-effects controls
- Bounded targeting and presentation with no real explosions or permanent terrain damage
- Optional PlaceholderAPI, WorldEdit, and ScaledEnderDragon compatibility
- Versioned configuration that merges new defaults without replacing administrator edits

## The three Dragonborn

Akuma is the frost soul. Frostveil provides freeze protection and cold-biome mobility. Akuma's Trail, Akuma's Hush, and Absolute Zero provide movement, control, Brittle, and a bounded Shatter follow-up.

Rev is the hunt soul. Cinderborn provides fire protection and increased energy regeneration in the Nether and End. Rev's redesigned Heat, Hunt, Inferno Mark, Rampage, Rend recast, Wrath field, and Predator's Claim systems reward direct pursuit while enforcing target cooldowns, mark caps, line of sight, and anti-farm limits.

Lamari is the stone soul. Stoneheart provides fall protection and armor toughness. Lamari's Fault, Lamari's Reckoning, and Titan's Bulwark provide landing pressure, knockup control, guarded reflection, and a capped stored-damage release.

All three retain Wings and Roar. Their fifth abilities consume the full 100-point energy bar and share one persistent ultimate cooldown. Nearby pairs unlock Thermal Convergence, Volcanic Aegis, or Glacial Bastion. All three together unlock Dragon Trinity. Resonances share a persistent cooldown across every participant.

## Requirements

- Paper 1.21.4
- Java 21
- Optional: PlaceholderAPI 2.11.6 or compatible
- Optional: WorldEdit 7.3.10 or compatible
- Optional: ScaledEnderDragon

Paper and optional plugin APIs are provided dependencies and are not bundled in the DragonAltar JAR.

## Installation

1. Stop the Paper server.
2. Copy `DragonAltar-1.4.18.jar` into the server's `plugins` directory.
3. Start the server once to create configuration and data directories.
4. Review `plugins/DragonAltar/config.yml` and the other generated YAML files.
5. Run `/dragon setup begin` and record the altar, interaction, ritual, arrival, fountain, and crystal locations.
6. Run `/dragon setup save`, then `/dragon setup validate`.
7. Validate the setup and gameplay flow on a test server before production.
8. After testing, set `server-mode: PRODUCTION`. Keep destructive production commands disabled unless they are deliberately needed.

DragonAltar does not paste or replace a physical altar. The altar egg is a protected `BlockDisplay`, not an obtainable egg block.

## First-time setup

The guided setup records exact coordinates, world names, world UUIDs, yaw, and pitch. The minimum production setup is:

- Egg display
- Interaction point
- Ritual center
- Arrival point
- End fountain
- North, south, east, and west crystal positions

The End fountain and crystal ring are validated before the official event starts. A schematic and DragonAltar's internal cuboid protection are optional and disabled by default.

The initial ritual consumes the configured recipe. Its default accepts one Elytra, one Nether Star, 16 Dragon's Breath, eight End Crystals, and four Echo Shards. Interrupted consumption is persisted and refunded exactly, including item metadata. Use `/dragon refunds` if inventory capacity prevented a complete refund.

## Commands and permissions

The primary command is `/dragon`; `/dragonaltar` is an alias.

Common player commands:

- `/dragon status`
- `/dragon abilities`
- `/dragon focus`
- `/dragon settings`
- `/dragon history [player]`
- `/dragon refunds`
- `/dragon help`

Administrative branches cover event, setup, altar, ritual, soul, ability, protection, system, and developer operations. Destructive operations use short-lived, sender-bound confirmation tokens. See [COMMANDS.md](COMMANDS.md) for the complete command tree and [PERMISSIONS.md](PERMISSIONS.md) for every permission.

## Configuration and upgrades

The bundled defaults are conservative and versioned. On load, missing keys are merged into the installed files. Existing administrator values are preserved. Targeted migrations replace only known former shipped defaults or renamed settings; customized values at the new paths win.

Runtime ownership is stored separately under `plugins/DragonAltar/data/`. Do not edit runtime data while the server is running. Back up both configuration and `data/` before an upgrade.

For configuration sections, units, production guidance, migrations, upgrade steps, and rollback steps, see [CONFIGURATION.md](CONFIGURATION.md).

## Accessibility

`/dragon settings` exposes:

- Full, reduced, or minimal effects
- HUD on or off
- Locked or sneak-scroll selection
- Slow Falling on or off
- Independent passive particles, animation particles, sounds, titles, and screen effects through the settings GUI

Minimal effects suppress nonessential visuals while retaining combat state in the HUD. Gameplay calculations do not depend on a player's presentation settings.

## Optional integrations

PlaceholderAPI exposes public soul and status placeholders. Public soul placeholders return Akuma, Rev, or Lamari; the numbered persistence key is available only through the explicitly named internal placeholder.

WorldEdit is optional. DragonAltar does not require it for setup or event validation.

ScaledEnderDragon is detected by plugin name. DragonAltar starts the real vanilla respawn sequence and leaves scaling, combat, and rewards to the integration. Remove obtainable dragon egg rewards from ScaledEnderDragon's rewards configuration.

See [API.md](API.md) for the Bukkit service, immutable state queries, events, and
custom ability hooks. [ADDON-DEVELOPMENT.md](ADDON-DEVELOPMENT.md) contains a
complete starter add-on.

## Building

```text
mvn clean package
```

The release JAR is written to `target/DragonAltar-1.4.18.jar`. Maven compiles with `--release 21`, runs the JUnit suite, filters only `plugin.yml`, and produces deterministic archive timestamps.

## License and add-ons

DragonAltar is source-available under the noncommercial terms in
[LICENSE.md](LICENSE.md). Independent add-ons may use the published API when the
add-on and all of its DragonAltar features remain free and the required
Silverfox0338 ownership notice is displayed. Paid, premium, subscription, and
paywalled DragonAltar features require separate written permission.

## Data and privacy

DragonAltar does not include telemetry, analytics, an update checker, or outbound service calls. It stores gameplay and configuration data locally. See [PRIVACY.md](PRIVACY.md).

## Issue reports

When reporting an issue, include:

- DragonAltar version
- Paper build and Java version
- Relevant console exception and DragonAltar audit entries
- Reproduction steps
- Whether the issue occurs on a clean default configuration
- Installed optional integrations

Remove player-identifying data and secrets before sharing logs or data files.

## Known limitations and release gates

- A live Paper server is required to verify fountain recognition, respawn beams, display interpolation, inventory behavior, chunk transitions, integrations, shutdown cleanup, and multiplayer performance.

See [RELEASE-NOTES.md](RELEASE-NOTES.md) and [CHANGELOG.md](CHANGELOG.md) for release details.
