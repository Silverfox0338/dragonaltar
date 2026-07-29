# Configuration

DragonAltar creates six editable YAML files. Each contains `config-version`. Runtime ownership and recovery files use `data-version` and live under `data/`.

## Files

| File | Purpose |
|---|---|
| `config.yml` | Server mode, event timing, eligibility, transfer policy, instability, safety, and removal ritual |
| `altar.yml` | Recorded locations, optional protection, egg display, and recipe display |
| `ritual.yml` | Initial ritual recipe, Elytra rules, phases, refund behavior, and presentation |
| `abilities.yml` | Energy, passives, abilities, Rev Hunt, ultimates, resonances, targeting, and presentation caps |
| `animations.yml` | Ordered animation steps |
| `messages.yml` | MiniMessage player text |

Runtime files are `data/event.yml`, `altar-state.yml`, `souls.yml`, `players.yml`, `rituals.yml`, `cooldowns.yml`, `pending-transfers.yml`, and `consequences.yml`. They are written atomically through one serialized persistence queue. Do not edit them while Paper is running.

## Units and safe ranges

Names ending in `-ticks` use server ticks, where 20 ticks are normally one second. Names ending in `-seconds` use seconds. `backfire-limbo-hours` uses real-time hours. Distances and radii use blocks. Damage uses Bukkit health points. `additional-hearts` uses hearts. Fractions and chances use values from 0 to 1.

Startup validation rejects malformed materials, particles, sounds, modes, negative durations, invalid relationships, and dangerous values. Important ceilings include:

- Ability target radius or range: 64 blocks
- Entities processed by one ability query: 128
- Ability particles per staged emission: 512, with a conservative default cap of 128
- Ring points: 128
- Presentation view distance: 128 blocks
- Animation step time or duration: 72,000 ticks
- Ritual particle count: 512
- Event nearby-player radius: 512 blocks
- Fractured Soul teleport radius: 256 blocks

Runtime targeting clamps radii and target counts even after configuration has loaded. Invalid configuration during `/dragon system reload` disables DragonAltar and prints the rejected fields to the console.

## Conservative production baseline

The shipped files are the supported conservative defaults. After completing live testing, the minimum production change is:

```yaml
server-mode: PRODUCTION
safety:
  allow-destructive-commands-in-production: false
```

Keep the destructive override false for normal operation. Operators do not bypass it.

The default ability configuration keeps Dragon Energy at 100, bounds Rev marks and per-target gains, ignores weak-mob Heat farming, caps Inferno Hunt mobility and Rampage, limits particles, and uses persistent shared ultimate and resonance cooldowns.

`focus.blocked-command-prefixes` blocks held-item sale commands such as `/ah sell`. `focus.blocked-inventory-command-prefixes` blocks bulk commands such as `/sellall` whenever a Focus is present. Focus items are owner-bound, cannot enter external inventories, and duplicate or escaped copies are removed automatically.

`rev-hunt.heat-bar` controls Rev's dedicated Heat boss bar. The bar shows the current value, maximum, and Stalking, Pursuing, or Predator tier. Its three tier colors, title, overlay, and enabled state are configurable. Players who disable their HUD also hide the Heat bar.

## Migration behavior

On startup DragonAltar:

1. Saves a missing default file.
2. Loads the installed file as UTF-8.
3. Loads the packaged defaults.
4. Applies targeted migrations for known renamed settings.
5. Adds only missing default leaves.
6. Advances `config-version`.
7. Preserves administrator values already present at current paths.

Targeted migrations cover the original beta ritual recipe, former removal-ritual messages, HUD status and resonance fields, resonance renamed tunables, and Rev version 6 timing and combat settings. Known shipped defaults may advance to their replacement. Customized values are retained wherever a safe mapping exists.

Deleting a key intentionally causes the packaged default to return at the next load. To permanently disable a feature, use its documented boolean or zero value where validation permits.

## Upgrade

1. Stop Paper cleanly.
2. Copy the entire `plugins/DragonAltar` directory to a dated backup outside the server directory.
3. Keep the previous plugin JAR.
4. Replace only the JAR.
5. Start Paper and inspect the console for migration or validation messages.
6. Run `/dragon system validate`, `/dragon altar validate`, and `/dragon system health`.
7. Confirm ownership, pending transfers, cooldowns, displays, and refunds.
8. Complete live upgrade checks on a test server before returning to production.

Do not copy fresh bundled YAML over installed YAML. DragonAltar performs the merge.

## Rollback

1. Stop Paper.
2. Preserve the failed-upgrade directory for diagnosis.
3. Restore the previous JAR and the matching full `plugins/DragonAltar` backup.
4. Start Paper.
5. Validate the altar, soul ownership, event state, cooldowns, refunds, and consequence state.

Rolling back only the JAR after a data migration is unsupported. Restore matching configuration and data together. DragonAltar's `/dragon dev data backup` is useful for operational snapshots, but a stopped full-directory backup is the safest release rollback point.

## Important sections

`eligibility` is shared by rituals, reincarnation, pending transfers, and Fractured Soul claims. `allowed-game-modes`, playtime, grace time, required permission, and exclusion permission should be reviewed together.

`transfer` controls the natural-death countdown, combat tags, and the Dragonborn-killer collision policy. Supported policies are `RANDOM_ELIGIBLE`, `OPEN_RITUAL_SLOT`, `SOUL_DORMANT`, and `PENDING_TRANSFER`.

`rev-hunt` controls Heat, Inferno Marks, Predator's Claim, tracking, and sound stages. `abilities.revs-rend`, `abilities.wrath-of-rev`, and `abilities.infernos-wrath` control the three connected Rev systems. Do not raise per-target limits without multiplayer performance testing.

`resonances` controls the 50-block unlock range, energy costs, shared cooldowns, pulses, finishers, wards, retaliation, Trinity restoration, displays, and links.

`ritual.yml` supports `INVENTORY_CONSUME`, `PEDESTAL_DEPOSIT`, and `HYBRID`. Elytra selection supports `MOST_DAMAGED`, `LEAST_DAMAGED`, `FIRST_MATCH`, and `LOWEST_ENCHANTMENT_VALUE`.

`animations.yml` supports `SOUND`, particle types, title and action-bar types, boss bars, cosmetic lightning, display transforms, temporary visual player effects, broadcasts, and `WAIT`. Cosmetic lightning uses `strikeLightningEffect` and does not create damage or fire.
