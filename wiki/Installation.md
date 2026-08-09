# Installing DragonAltar

DragonAltar 1.4.21 requires Paper 1.21.4 and Java 21. Paper and optional plugin APIs are not bundled in the DragonAltar JAR.

## Fresh installation

1. Stop the Paper server.
2. Confirm that the server actually starts with Java 21.
3. Put `DragonAltar-1.4.21.jar` in the server's `plugins` directory.
4. Start the server once.
5. Confirm that the console reports DragonAltar 1.4.21 and does not report a configuration validation failure.
6. Stop the server and review the six generated configuration files under `plugins/DragonAltar`.
7. Start the server and complete [Altar Setup](Altar-Setup).
8. Run `/dragon system validate`, `/dragon setup validate`, and `/dragon system health`.
9. Test the full event and ritual flow away from production.
10. Set `server-mode: PRODUCTION` only after the live test passes.

The generated directories include `data`, `backups`, and `logs`.

## Required and optional plugins

| Component | Required | What 1.4.21 does |
|---|---:|---|
| Paper 1.21.4 | Yes | Provides the server API and vanilla Ender Dragon respawn controls |
| PlaceholderAPI | No | Registers the `%dragonaltar_...%` placeholders |
| ScaledEnderDragon | No | Lets its normal scaling and rewards observe DragonAltar's vanilla respawned dragon |

Use `/dragon system integrations` to see which optional plugins were enabled when DragonAltar checked the server.

## First startup

DragonAltar loads all six editable YAML files before it starts gameplay services:

- `config.yml`
- `altar.yml`
- `ritual.yml`
- `abilities.yml`
- `animations.yml`
- `messages.yml`

It merges missing defaults and runs strict validation. If validation fails during startup, DragonAltar disables itself and logs every rejected field. Fix the listed values, then restart. See [Configuration](Configuration) for accepted values and safe ranges.

The initial altar locations are blank. This is expected. Use `/dragon setup begin` rather than typing serialized locations by hand.

## Upgrading to 1.4.21

> Stop the server before copying or restoring DragonAltar data. Do not replace installed YAML files with fresh bundled copies.

1. Stop Paper cleanly.
2. Copy the entire `plugins/DragonAltar` directory to a dated backup outside the live server directory.
3. Keep the previous DragonAltar JAR with that backup.
4. Replace only the plugin JAR.
5. Start Paper and read the DragonAltar startup messages.
6. Run `/dragon system version` and confirm `1.4.21`.
7. Run `/dragon system validate`.
8. Run `/dragon altar validate`.
9. Run `/dragon system health`.
10. Check the altar displays, all public soul statuses, pending refunds, and expected cooldown behavior.

Missing settings are added automatically. Existing settings at current paths are preserved. Known renamed settings receive targeted migration where a safe mapping exists.

Older installed YAML files may still contain settings retired in 1.4.19 because
DragonAltar does not delete administrator entries during a normal upgrade.
Those entries are ignored. After making a backup, compare the files with the
[Configuration](Configuration) reference and remove retired entries if desired.

## Rollback

Rolling back only the JAR is unsafe after data or configuration migration.

1. Stop Paper.
2. Preserve the failed-upgrade directory for diagnosis.
3. Restore the previous JAR and its matching full `plugins/DragonAltar` backup.
4. Start Paper.
5. Validate the event, altar, public soul state, cooldowns, refunds, and consequence state.

The developer backup command is useful for an operational snapshot, but a full stopped-server directory backup is the safest rollback point.

## Build from source

From the repository root:

```text
mvn clean package
```

The verified JAR is written to:

```text
dragonaltar/target/DragonAltar-1.4.21.jar
```

Maven compiles with Java release 21 and runs the JUnit suite.

## Next step

Continue with [Altar Setup](Altar-Setup). Do not start the official event until setup validation reports ready.
