# Administrator Guide

This guide follows the normal operating path from installation to recovery. Exact syntax lives in [Commands](Commands), and every permission is listed in [Permissions](Permissions).

## Normal operating sequence

1. Install DragonAltar on Paper or Purpur 1.21-1.21.11 with Java 21.
2. Leave `server-mode: BETA` during setup and testing.
3. Complete the guided altar and fountain setup.
4. Run all three checks:

   ```text
   /dragon system validate
   /dragon altar validate
   /dragon system health
   ```

5. Create a stopped-server backup.
6. Preview and start the official event.
7. Watch the canonical dragon and event state.
8. Confirm that the altar awakens and all three initial rituals work.
9. Test natural death, PvP inheritance, pending transfer, refunds, and restart recovery.
10. Set `server-mode: PRODUCTION`.
11. Keep `safety.allow-destructive-commands-in-production: false`.

## Administrative branches

| Branch | Main job | Permission |
|---|---|---|
| `/dragon event` | Official event preview, start, inspection, and recovery | `dragonaltar.admin.event` |
| `/dragon setup` | Guided location staging and save | `dragonaltar.setup` |
| `/dragon altar` | Altar state and display operations | `dragonaltar.admin.altar` |
| `/dragon ritual` | Initial ritual inspection and recovery | `dragonaltar.admin.ritual` for mutations |
| `/dragon admin` and `/dragon soul` | Soul, player, ability, energy, cooldown, and refund work | `dragonaltar.admin.souls` or `dragonaltar.admin.abilities` |
| `/dragon protection` | Optional internal cuboid protection | `dragonaltar.admin.protection` |
| `/dragon system` | Validation, health, persistence, integrations, and entity checks | `dragonaltar.admin.system` |
| `/dragon dev` | Beta diagnostics, simulation, backups, direct repair, and reset | `dragonaltar.developer` |

The bare `/dragon admin` opens a player-only GUI and requires the parent `dragonaltar.admin`. Granular command branches can be granted without that GUI permission.

All branches still require `dragonaltar.use`.

## Safe confirmation

High-risk commands do not execute immediately. They print:

- Affected players
- Affected public souls
- Expected result
- Cooldown impact
- History impact
- Whether an automatic undo exists

They then issue a short-lived token:

```text
/dragon confirm <token>
```

Or cancel it:

```text
/dragon cancel
```

Tokens are bound to the command sender, operation, and arguments. They are single-use and expire after `event.confirmation-seconds`, 30 seconds by default. If relevant server state changes after the preview, confirmation is rejected and nothing runs.

The official event start uses its own form:

```text
/dragon event confirm-start <token>
```

Never copy a token from another operator or confirm a preview you did not read.

## Production safety

In `PRODUCTION` mode:

- Forced altar state changes are blocked
- Forced ritual completion is blocked
- The entire developer command branch is blocked

They remain blocked even for operators unless:

```yaml
safety:
  allow-destructive-commands-in-production: true
```

> Turn this override on only for a named repair, complete that repair, then turn it off and reload. It is not a normal operating mode.

Some ordinary administrative ownership operations are confirmation-protected but are not covered by the production forced-state gate. Restrict `dragonaltar.admin.souls` carefully.

## Backups

The safest backup is a complete copy of `plugins/DragonAltar` while Paper is stopped.

For an online operational snapshot:

```text
/dragon dev data backup
```

DragonAltar copies its known runtime data files into:

```text
plugins/DragonAltar/backups/<timestamp>
```

List or inspect backups with `/dragon dev data dump`. Restoring a named backup is destructive:

```text
/dragon dev data restore <name>
/dragon confirm <token>
```

Restore accepts only a direct named backup directory and copies known DragonAltar data files. It then reloads runtime data. It is not a substitute for a full configuration and plugin-version rollback.

## Event recovery

Before changing anything:

```text
/dragon event status
/dragon event dragon-info
/dragon event locate
/dragon system entities
/dragon system health
```

Then use `/dragon event rescan` or `/dragon event recover` only after reading the preview. Do not delete crystals or dragons first unless you have already identified them as developer test entities and have a backup.

See [Ancient Dragon Event](Ancient-Dragon-Event) for the evidence-based recovery table.

## Ritual and refund recovery

An active initial ritual is converted into an exact-item refund after restart. Check:

```text
/dragon ritual status
/dragon admin refunds inspect <player>
```

Ask the player to free inventory space and run `/dragon refunds`. If needed:

```text
/dragon admin refunds give <player>
```

The target must be online.

## Pending soul transfers

Pending does not mean lost. Check public state, eligibility, and online candidates:

```text
/dragon admin list
/dragon dev eligibility list
/dragon dev eligibility explain <player>
```

An eligible player join can claim one pending soul. Startup also attempts pending recovery. If deliberate intervention is needed, use the soul commands only after taking a backup and reading the confirmation preview.

## Repairing a Dragonborn

For missing passives, stale ability cache, missing Focus, or bad energy:

```text
/dragon admin repair <player>
```

The player must be online and already hold a soul. This reapplies passives, verifies the Focus, clears the player's ability cache, and fills Dragon Energy while preserving HUD preference.

`/dragon admin fix-passives <player>` only reapplies the passive state.

## Display recovery

Inspect first:

```text
/dragon altar egg inspect
/dragon altar recipe inspect
/dragon dev altar displays
```

Then reconcile:

```text
/dragon dev altar repair-displays
```

The repair keeps one correctly located plugin-owned egg and recipe display, removes tagged duplicates, and restores missing displays when the altar state calls for them.

## Logs and privacy

Configuration and gameplay data stay under `plugins/DragonAltar`. Operational audit files are local and should be restricted to trusted server owners. Never paste unredacted logs, data YAML, or diagnostic dumps into public support channels.

Player-visible commands, history, placeholders, and API snapshots are the approved public views. Do not build a public dashboard from runtime data files or diagnostic command output.

## Useful routine checks

```text
/dragon system version
/dragon system status
/dragon system validate
/dragon system health
/dragon system integrations
/dragon altar validate
/dragon event status
```

Continue with [Configuration](Configuration), [Commands](Commands), and [Troubleshooting](Troubleshooting).
