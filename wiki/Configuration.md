# Configuration Reference

DragonAltar 1.4.18 ships six editable YAML files. Each has a `config-version`. Runtime state is stored separately under `plugins/DragonAltar/data` and uses `data-version`.

> Do not edit runtime data while Paper is running. Do not replace installed configuration with fresh bundled files during an upgrade.

The JAR also contains `plugin.yml`, Paper's packaged plugin descriptor. It declares API version `1.21`, the `/dragon` command and `/dragonaltar` alias, all permission nodes, and the PlaceholderAPI, WorldEdit, and ScaledEnderDragon soft dependencies. Paper reads it from the JAR, so it is not copied out as an editable server configuration file. Its command and permission declarations are covered in [Commands](Commands) and [Permissions](Permissions).

## Units

| Name pattern | Unit |
|---|---|
| `*-ticks` | Server ticks, normally 20 per second |
| `*-seconds` | Seconds |
| `*-hours` | Real-time hours |
| Radius, range, distance | Blocks |
| Damage | Bukkit health points, where 2 points equal 1 heart |
| `additional-hearts` | Hearts |
| Fractions and chances | Values from 0 to 1 |
| Potion amplifiers | Bukkit zero-based amplifiers, so `1` displays as level II |

## `config.yml`

Shipped `config-version`: `2`.

### Server and safety

| Path | Default | Effect |
|---|---|---|
| `server-mode` | `BETA` | `BETA` or `PRODUCTION` |
| `safety.allow-destructive-commands-in-production` | `false` | Allows forced state and developer branches in production when deliberately enabled |

Operators do not bypass the production gate.

### Official event

| Path | Default | Accepted or practical range |
|---|---:|---|
| `event.end-world` | `world_the_end` | Configured End world name |
| `event.nearby-player-radius` | 128 | Greater than 0, at most 512 |
| `event.confirmation-seconds` | 30 | 1 to 300 |
| `event.scaled-dragon-reward-delay-ticks` | 40 | 0 to 12,000 |
| `event.altar-awakening-delay-ticks` | 80 | 0 to 12,000 |

The confirmation duration is also used by general destructive-command tokens.

### Eligibility

| Path | Default | Effect |
|---|---|---|
| `eligibility.allowed-game-modes` | `[SURVIVAL, ADVENTURE]` | Empty means any mode, though Spectator still fails the alive check |
| `eligibility.minimum-playtime-ticks` | 0 | Compared with Paper's `PLAY_ONE_MINUTE` statistic |
| `eligibility.required-permission` | Empty | Blank disables this requirement |
| `eligibility.exclusion-permission` | `dragonaltar.eligibility.excluded` | A player with this permission is excluded |
| `eligibility.recently-joined-grace-seconds` | 0 | Delay before a joining player can be selected |

Eligibility is shared by initial claims, reincarnation, limbo release, and automated transfer selection.

### Transfer

| Path | Default | Effect |
|---|---:|---|
| `transfer.natural-death-countdown-seconds` | 10 | Random reincarnation countdown, 0 to 3,600 |
| `transfer.dragonborn-killer-policy` | `RANDOM_ELIGIBLE` | Collision policy when the killer already has a soul |
| `transfer.combat-tag-seconds` | 15 | PvP logout tag, 0 to 3,600 |

Supported collision policies are `RANDOM_ELIGIBLE`, `OPEN_RITUAL_SLOT`, `SOUL_DORMANT`, and `PENDING_TRANSFER`.

### Instability and Fractured Souls

| Path | Default | Validation |
|---|---:|---|
| `instability.threshold` | 6 | Cannot be negative; fracture starts only after this count |
| `instability.fracture-chance` | 0.20 | 0 to 1 |
| `instability.teleport-min-seconds` | 45 | Positive |
| `instability.teleport-max-seconds` | 60 | At least the minimum, at most 86,400 |
| `instability.teleport-radius` | 32 | 8 to 256 |
| `instability.bossbar-coordinate-update-seconds` | 7 | Runtime clamps to at least 5 |

With the shipped threshold, Mother Soul cast 7 is the first cast eligible for the fracture roll.

### Mother Soul removal ritual

| Path | Default |
|---|---:|
| `forced-removal-ritual.participant-count` | 4 |
| `forced-removal-ritual.selection-timeout-seconds` | 60 |
| `forced-removal-ritual.animation-duration-ticks` | 160 |
| `forced-removal-ritual.backfire-limbo-hours` | 12 |

Version 1.4.18's structure and participant logic is fixed at four caller pads even though `participant-count` is shipped. The runtime message also reports four. The timeout, animation duration, and limbo hours are active.

### Developer branch

`developer.enabled-in-beta` defaults to `true`. When false, `/dragon dev` is blocked in Beta. In Production, every `/dragon dev` command is blocked unless the destructive production override is true, including read-only-looking diagnostics.

## `altar.yml`

Shipped `config-version`: `2`.

### Structure and protection keys

| Path | Default | 1.4.18 behavior |
|---|---:|---|
| `existing-structure` | `true` | Retained setting; no runtime branch reads it |
| `schematic.enabled` | `false` | Reported in validation output only |
| `schematic.required-for-event` | `false` | Retained; not enforced |
| `internal-protection.enabled` | `false` | Active switch for the cuboid protection listener |
| `internal-protection.required-for-event` | `false` | Retained; not enforced |
| `protection.enabled` | `false` | Legacy fallback only when `internal-protection.enabled` is absent |

The shipped file contains both protection switches. Since `internal-protection.enabled` is present, it wins.

### Saved locations

The file initially contains empty values for:

- `world`
- `altar-center`
- `ritual-center`
- `egg-display`
- `interaction`
- `arrival`
- `fountain`
- `crystals`
- `pedestals`
- `protection.pos1`
- `protection.pos2`

The setup service writes `world-uuid`, `world`, `x`, `y`, `z`, `yaw`, and `pitch` under each location.

### Egg display

| Path | Default | Active |
|---|---:|:---:|
| `display.material` | `DRAGON_EGG` | Yes |
| `display.glow` | `true` | Yes |
| `display.glow-color` | `AA00AA` | Yes |
| `display.bob-height` | 0.20 | No runtime read in 1.4.18 |
| `display.scale-pulse-amplitude` | 0.08 | No runtime read in 1.4.18 |
| `display.rotation-degrees-per-tick` | 2.0 | Yes |
| `display.idle-particle` | `DRAGON_BREATH` | Yes |
| `display.idle-particle-count` | 2 | Yes, 0 to 128 |
| `display.idle-particle-interval-ticks` | 10 | Yes, positive |
| `display.ambient-sound` | `minecraft:block.beacon.ambient` | Yes |
| `display.ambient-sound-interval-ticks` | 200 | Yes, positive |
| `display.heartbeat-sound` | `minecraft:entity.warden.heartbeat` | Yes |
| `display.heartbeat-interval-ticks` | 100 | Yes, positive |

The runtime task updates every 2 ticks. The configured rotation is multiplied by that interval.

### Recipe display

| Path | Default |
|---|---:|
| `recipe-display.enabled` | `true` |
| `recipe-display.offset.x` | 0.0 |
| `recipe-display.offset.y` | 1.8 |
| `recipe-display.offset.z` | 0.0 |
| `recipe-display.billboard` | `CENTER` |
| `recipe-display.shadowed` | `true` |
| `recipe-display.see-through` | `false` |
| `recipe-display.line-width` | 220 |
| `recipe-display.view-range` | 32.0 |
| `recipe-display.lines` | Shipped title, five recipe lines, prompt, and remaining soul count |

Line width must be 1 to 2,048. View range must be greater than 0 and at most 128.

The `%remaining_souls%` token in these lines is replaced directly. Text uses MiniMessage.

## `ritual.yml`

Shipped `config-version`: `2`.

### Offerings

`offering-mode` defaults to `INVENTORY_CONSUME`. Supported modes are `INVENTORY_CONSUME`, `PEDESTAL_DEPOSIT`, and `HYBRID`.

| Id | Material | Amount | Display |
|---|---|---:|---|
| `elytra` | `ELYTRA` | 1 | Elytra |
| `nether-star` | `NETHER_STAR` | 1 | Nether Star |
| `dragon-breath` | `DRAGON_BREATH` | 16 | Dragon's Breath |
| `end-crystals` | `END_CRYSTAL` | 8 | End Crystals |
| `echo-shards` | `ECHO_SHARD` | 4 | Echo Shards |

Each amount must be 1 to 2,304. Materials must be valid Paper material names.

`id`, `material`, `amount`, and `display-name` are read. The shipped Elytra offering's `durability-mode: ANY` is retained but is not read by 1.4.18.

### Elytra matching

| Path | Default | Active |
|---|---:|:---:|
| `elytra.accept-any-durability` | `true` | Durability is accepted, but this boolean is not read |
| `elytra.accept-enchanted` | `true` | Yes |
| `elytra.accept-renamed` | `true` | Yes |
| `elytra.accept-custom-lore` | `true` | Yes |
| `elytra.include-equipped-chest-slot` | `false` | Yes |
| `elytra.consumption-priority` | `MOST_DAMAGED` | Yes |
| `elytra.blocked-pdc-keys` | `[]` | Yes |

Priority values are `MOST_DAMAGED`, `LEAST_DAMAGED`, `FIRST_MATCH`, and `LOWEST_ENCHANTMENT_VALUE`.

### Phases and failure

| Path | Default |
|---|---:|
| `phases.OFFERINGS_ACCEPTED` | 40 ticks |
| `phases.ALTAR_CHARGING` | 100 ticks |
| `phases.SOUL_AWAKENING` | 80 ticks |
| `phases.PLAYER_BINDING` | 60 ticks |
| `phases.ASCENSION` | 60 ticks |
| `phases.COMPLETION` | 40 ticks |
| `refund-on-cancel` | `true` |
| `restrict-movement` | `true` |
| `ritual-radius` | 8.0 |
| `failure.cancel-on-damage` | `true` |

Phase durations cannot be negative. Ritual radius must be positive.

### Cinematic

| Path | Default |
|---|---|
| `cinematic.boss-bar` | `true` |
| `cinematic.boss-bar-title` | `Dragon Ritual: {phase}` |
| `cinematic.boss-bar-color` | `PURPLE` |
| `cinematic.titles` | `true` |
| `cinematic.particle` | `DRAGON_BREATH` |
| `cinematic.particle-count` | 30 |

Particle count is 0 to 512. The boss-bar color and particle must be valid Paper enum values.

## `abilities.yml`

Shipped `config-version`: `6`.

### Energy and presentation caps

| Path | Default | Validation |
|---|---:|---|
| `energy.maximum` | 100 | Must remain exactly 100 |
| `energy.regeneration` | 2 | 0 to 100 |
| `energy.regeneration-interval-ticks` | 20 | 1 to 1,200 |
| `energy.delay-after-cast-ticks` | 60 | 0 to 72,000 |
| `presentation.maximum-particles-per-emission` | 128 | Positive, at most 512 |
| `presentation.maximum-ring-points` | 48 | Positive, at most 128 |
| `presentation.maximum-flame-arc-particles` | 16 | Positive |
| `presentation.view-distance-blocks` | 48 | Positive, at most 128 |

Target radius and range values are validated from 0 to 64. Runtime also hard-clamps target radius to 64 and each query to 128 entities.

### Focus

| Path | Default |
|---|---|
| `focus.material` | `ECHO_SHARD` |
| `focus.name` | `Dragon Focus` in light purple MiniMessage |
| `focus.soulbound` | `true` |
| `focus.non-droppable` | `true` |
| `focus.blocked-command-prefixes` | `ah sell`, `auction sell`, `auctionhouse sell`, `market sell`, `sell` |
| `focus.blocked-inventory-command-prefixes` | `sellall` |

The Focus protection listeners are always active for tagged Focus items. The two booleans are descriptive shipped settings and are not used as off switches in 1.4.18.

Command prefix matching is case-insensitive, collapses whitespace, accepts a leading slash, and also matches namespaced command labels.

### Passives

| Path | Default | Active |
|---|---:|:---:|
| `passives.additional-hearts` | 2 | Yes |
| `passives.slow-falling` | `true` | Yes |
| `passives.neutral-endermen` | `true` | Yes |
| `passives.fire-damage-multiplier` | 0.5 | No runtime read |
| `passives.particles` | `true` | Yes |
| `named-souls.akuma.cold-temperature-threshold` | 0.15 | Yes |
| `named-souls.akuma.cold-speed-bonus` | 0.15 | Yes |
| `named-souls.rev.dimension-energy-multiplier` | 1.5 | Yes |
| `named-souls.lamari.armor-toughness` | 4 | Yes |

### Registered ability defaults

| Ability id | Energy | Cooldown |
|---|---:|---:|
| `wings` | 40 | 45 seconds |
| `roar` | 35 | 25 seconds |
| `akumas-trail` | 25 | 12 seconds |
| `akumas-hush` | 60 | 60 seconds |
| `absolute-zero` | Full bar | 120 seconds |
| `revs-rend` | 25 | 12 seconds |
| `wrath-of-rev` | 60 | 60 seconds |
| `infernos-wrath` | Full bar | 120 seconds |
| `lamaris-fault` | 30 | 18 seconds |
| `lamaris-reckoning` | 60 | 60 seconds |
| `titans-bulwark` | Full bar | 120 seconds |

`ultimate.shared-cooldown-seconds` defaults to 120.

The `abilities.dash`, `abilities.sight`, and `abilities.resolve` sections are retained from older layouts but are not registered in 1.4.18. See [Abilities](Abilities) for every active combat, duration, damage, radius, and drawback default.

The full leaf-by-leaf reference, including presentation, sound, Rev Hunt, drawback, and resonance tuning, is on [Ability Configuration](Ability-Configuration).

### Rev Hunt

| Section | Important defaults |
|---|---|
| `rev-hunt.heat-bar` | Enabled, `REV HEAT` title, yellow/red/purple tiers, progress overlay |
| `rev-hunt.heat` | Maximum 100, 4 per direct marked hit, 20-tick target delay, player cap 20, mob cap 3, mob health floor 12, decay 2 per second after 100 ticks |
| `rev-hunt.mark` | 120-tick duration, 180-tick remaining cap, 8 targets |
| `rev-hunt.finisher` | 120-tick armed duration, reset Heat when consumed |
| `rev-hunt.tracking` | 24-block range, 4 cues, 4 particles each, 1.8-block directional cue |
| `rev-hunt.sounds` | Tier gain and finisher-ready sounds, volumes, and pitches |

Heat mobility and tracking thresholds default to 35 and 65. The threshold order is validated.

### Resonances

`resonances.unlock-range-blocks` defaults to 50. All four cooldowns default to 720 seconds. Pair costs are 70 and Dragon Trinity costs 100.

| Resonance | Active time | Main radii | Main damage |
|---|---:|---|---|
| Thermal Convergence | 6 seconds | 11 | 12 initial, 2 pulses, 6 finisher |
| Volcanic Aegis | 10 seconds | 6 pulse, 10 finisher | 10 initial, 2 pulses, 3 retaliation, 8 finisher |
| Glacial Bastion | 10 seconds | 7 domain, 11 finisher | 1 pulses, 8 finisher |
| Dragon Trinity | 12 seconds | 16 field, 18 finisher | 18 initial, 4 pulses, 14 finisher |

See [Resonances](Resonances) for buffs, debuffs, wards, healing, energy restoration, and shared cooldown behavior.

## `animations.yml`

Shipped `config-version`: `1`.

The file contains these definitions:

| Id | Shipped steps |
|---|---|
| `altar-awaken` | Dragon growl, portal ring, cosmetic lightning |
| `egg-deplete` | Dragon Breath spiral, scale egg display to zero |
| `pvp-transfer` | Dragon growl, temporary wings, wait |
| `egg-idle` | Portal ring |
| `egg-claim` | Amethyst chime, Dragon Breath spiral |
| `soul-depart` | Soul Fire Flame spiral |
| `soul-arrive` | Cosmetic lightning, Dragon Breath ring |
| `natural-transfer` | Soul spiral, temporary wings |
| `ritual-start` | Beacon sound, visual levitation, enchantment ring |
| `ritual-egg` | Display move, rotate, and return |
| `ritual-complete` | Cosmetic lightning, dragon growl, temporary wings |

Supported action types:

```text
SOUND
PARTICLE
PARTICLE_RING
PARTICLE_SPIRAL
LIGHTNING_EFFECT
TITLE
SUBTITLE
ACTION_BAR
BROADCAST
BOSS_BAR
PLAYER_GLOW
PLAYER_LEVITATE
TEMPORARY_WINGS
DISPLAY_MOVE
DISPLAY_SCALE
DISPLAY_ROTATE
DISPLAY_FADE
WAIT
```

Every step uses `at-tick`. Time and duration must be 0 to 72,000 ticks. Particle count must be 0 to 512. Particle and sound identifiers are validated. Cosmetic lightning uses `strikeLightningEffect`, so it does not deal damage or create fire.

Unknown or invalid runtime steps are logged and skipped.

## `messages.yml`

Shipped `config-version`: `7`.

Ordinary localized text uses MiniMessage. Replacement values supplied by the plugin are inserted as unparsed text, which prevents names and values from injecting MiniMessage tags.

The shipped keys are grouped below.

| Group | Keys |
|---|---|
| General command | `prefix`, `no-permission`, `player-only`, `command-error`, `help`, `player-status` |
| Setup and event | `setup-incomplete`, `event-started`, `event-defeated`, `event-recovery-required` |
| Souls and transfer | `dragonborn-gain`, `dragonborn-transfer`, `no-eligible-recipient`, `natural-death`, `natural-countdown`, `reincarnation`, `combat-log-transfer` |
| Focus and settings | `focus-restored`, `focus-unavailable`, `focus-protected`, `focus-inventory-full`, `settings-updated`, `settings-status` |
| Initial ritual | `ritual-refund`, `ritual-error` |
| Abilities | `abilities-list`, `ability-cooldown`, `ability-ultimate-cooldown`, `ability-full-energy`, `ability-energy`, `ability-cancelled`, `ability-not-dragonborn`, `ability-unavailable`, `ultimate-downside` |
| Resonances and HUD | `ability-resonance-cooldown`, `ability-resonance-lost`, `energy-hud`, `resonance-unlocked`, `resonance-lost`, `resonance-cast` |
| Mother Soul | `removal-ritual-invalid-altar`, `removal-ritual-participants`, `removal-ritual-no-targets`, `removal-ritual-busy`, `removal-ritual-expired`, `removal-ritual-invocation`, `removal-ritual-cleansing-complete`, `removal-ritual-backfire`, `all-dragonborn-silenced` |
| Limbo and fracture | `limbo-soul-released`, `fractured-soul-manifest`, `fractured-soul-claimed`, `fractured-soul-already-bound` |
| History and display | `history-entry`, `history-empty`, `egg-hologram`, `protection-bypass` |

In 1.4.18, `prefix` is not automatically prepended by `MessageService`. The shipped `player-only`, `setup-incomplete`, `event-started`, `ritual-error`, `event-recovery-required`, and `egg-hologram` entries have no direct call path. The recipe display uses `altar.yml` lines instead.

## Runtime data files

DragonAltar writes:

- `data/event.yml`
- `data/altar-state.yml`
- `data/souls.yml`
- `data/players.yml`
- `data/rituals.yml`
- `data/cooldowns.yml`
- `data/pending-transfers.yml`
- `data/consequences.yml`

Writes use one serialized asynchronous queue, a temporary file, and a replace move. Shutdown flushes pending work.

## Validation ceilings

| Value | Ceiling |
|---|---:|
| Ability radius or range | 64 blocks |
| Entities processed by a target query | 128 |
| Particles in one staged emission | 512 |
| Ring points | 128 |
| Presentation view distance | 128 blocks |
| Animation step time or duration | 72,000 ticks |
| Ritual particles | 512 |
| Event nearby-player radius | 512 blocks |
| Fractured Soul teleport radius | 256 blocks |

## Reload behavior

`/dragon system reload` reloads the editable files and rebuilds services. If validation fails, DragonAltar disables itself and prints the errors. Prefer a full restart after large configuration changes.

## Migration behavior

On load, DragonAltar:

1. Creates a missing file from its packaged default.
2. Loads installed UTF-8 YAML.
3. Loads the packaged defaults.
4. Applies known targeted migrations.
5. Adds only missing default leaves.
6. Advances `config-version`.
7. Preserves values already present at current paths.

Targeted migrations cover:

- The original one-Nether-Star beta ritual recipe
- Former Mother Soul message defaults
- Ability HUD status and resonance fields
- Renamed resonance tunables
- Rev version 6 timing and combat settings

Deleting a key causes its packaged default to return at the next load. Use a documented boolean or allowed zero value to disable a feature.

## Production baseline

```yaml
server-mode: PRODUCTION
safety:
  allow-destructive-commands-in-production: false
```

Keep the override false during normal service. See [Administrator Guide](Administrator-Guide) for backups and [Troubleshooting](Troubleshooting) for rejected values.
