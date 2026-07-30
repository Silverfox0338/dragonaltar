# Integrations

DragonAltar declares PlaceholderAPI and ScaledEnderDragon as soft dependencies. Neither is required for the core event, altar, souls, rituals, or abilities.

Use:

```text
/dragon system integrations
```

The command lists optional plugins that were enabled under the expected plugin names.

## PlaceholderAPI

When PlaceholderAPI is enabled during DragonAltar startup, the `dragonaltar` expansion registers and persists across PlaceholderAPI reloads.

| Placeholder | Result |
|---|---|
| `%dragonaltar_event_state%` | Official event enum name |
| `%dragonaltar_event_completed%` | `true` only when event state is exactly `COMPLETED` |
| `%dragonaltar_altar_state%` | Altar enum name |
| `%dragonaltar_souls_unclaimed%` | Count of souls in the unclaimed state |
| `%dragonaltar_souls_pending%` | Count of souls in pending transfer state |
| `%dragonaltar_dragonborn_count%` | Count of soul records with a holder |
| `%dragonaltar_is_dragonborn%` | Whether the subject player currently has a held soul |
| `%dragonaltar_soul_id%` | Subject's public soul name, despite the legacy placeholder name |
| `%dragonaltar_soul_name%` | Subject's public soul name |
| `%dragonaltar_soul_internal_id%` | Subject's persisted soul id |
| `%dragonaltar_energy%` | Subject's Dragon Energy, or `0` when not Dragonborn |
| `%dragonaltar_energy_max%` | Configured maximum Dragon Energy |
| `%dragonaltar_selected_ability%` | Subject's selected ability id |
| `%dragonaltar_selected_ability_cooldown%` | Remaining effective cooldown seconds |
| `%dragonaltar_combat_tagged%` | Whether the subject has an active PvP tag |
| `%dragonaltar_combat_seconds%` | Remaining PvP tag seconds |

Player-scoped ability placeholders are intended for Dragonborn subjects.

The PlaceholderAPI expansion reads live service state directly rather than using the privacy-filtered snapshot API. Keep player-scoped placeholders in the subject's own HUD. Do not use them to build a public cross-player holder or diagnostic board. For public websites or broad in-game displays, use the [API Reference](API-Reference) snapshot methods and their redacted holder behavior.

Unknown placeholder parameters return `null`, which lets PlaceholderAPI continue its normal fallback behavior.

## ScaledEnderDragon

DragonAltar starts the real vanilla Ender Dragon respawn. This allows ScaledEnderDragon to observe the dragon through its normal server hooks.

DragonAltar does not configure ScaledEnderDragon's scaling, combat, loot, or rewards. Those remain owned by that plugin.

At startup, DragonAltar warns when ScaledEnderDragon is detected:

- Keep the vanilla respawn path enabled
- Remove obtainable dragon egg rewards from ScaledEnderDragon's rewards configuration

DragonAltar watches player and console commands for `sed kill`. If the canonical dragon dies within 10 seconds, its completion method is recorded as `SED_KILL`; the marker is consumed. Other deaths are recorded as `COMBAT`.

This marker does not make an unrelated dragon canonical and does not bypass the official event session checks.

## Compatibility checks

After adding or updating an optional plugin:

1. Restart Paper.
2. Run `/dragon system integrations`.
3. Run `/dragon system health`.
4. Test the exact integration on a non-production server.
5. For PlaceholderAPI, test both a Dragonborn subject and a non-Dragonborn subject.
6. For ScaledEnderDragon, verify respawn beams, scaling, death, rewards, and altar awakening on the live Paper build.

Optional APIs are not bundled in the DragonAltar JAR.
