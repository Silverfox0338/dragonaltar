# Ability Configuration

This is the complete tuning reference for the shipped `abilities.yml` in DragonAltar 1.4.18. The [Abilities](Abilities) and [Resonances](Resonances) pages explain what players experience.

## Reading the values

- Damage uses health points. Two health points equal one heart.
- Potion amplifiers are zero based. `0` gives level I, `1` gives level II, and so on.
- Tick values normally run at 20 ticks per second.
- Energy is measured against a maximum of 100.
- Particle values are requested counts. Global caps and player effect settings may reduce them.
- Sound names, bar colors, particles, materials, and effects must be valid Paper enum values.

Reloading this file rebuilds the ability services. A validation failure disables DragonAltar, so back up a live server before changing it.

## Global energy and presentation

Shipped `config-version`: `6`.

| Path | Shipped value | Purpose |
|---|---:|---|
| `energy.maximum` | 100 | Energy maximum; validation requires exactly 100 |
| `energy.regeneration` | 2 | Energy restored each interval |
| `energy.regeneration-interval-ticks` | 20 | Time between regeneration attempts |
| `energy.delay-after-cast-ticks` | 60 | Regeneration pause after a successful cast |
| `presentation.maximum-particles-per-emission` | 128 | Per-emission particle cap |
| `presentation.maximum-ring-points` | 48 | Particle ring point cap |
| `presentation.maximum-flame-arc-particles` | 16 | Flame arc point cap |
| `presentation.view-distance-blocks` | 48 | Presentation audience distance |

Validation caps an emission at 512 particles, a ring at 128 points, and view distance at 128 blocks. Ability radius and range values cannot exceed 64 blocks.

## Dragon Focus

| Path | Shipped value | Purpose |
|---|---|---|
| `focus.material` | `ECHO_SHARD` | Material used for a new Focus |
| `focus.name` | `<light_purple>Dragon Focus` | MiniMessage display name |
| `focus.soulbound` | `true` | Descriptive retained setting; not an off switch in 1.4.18 |
| `focus.non-droppable` | `true` | Descriptive retained setting; not an off switch in 1.4.18 |
| `focus.blocked-command-prefixes` | `ah sell`, `auction sell`, `auctionhouse sell`, `market sell`, `sell` | Commands blocked while a Focus is present |
| `focus.blocked-inventory-command-prefixes` | `sellall` | Commands that trigger an inventory scan |

Focus identity comes from persistent item data, not the visible name.

## Passives and named soul modifiers

| Path | Shipped value | Purpose |
|---|---:|---|
| `passives.additional-hearts` | 2 | Extra maximum-health hearts |
| `passives.slow-falling` | `true` | Slow Falling passive switch |
| `passives.neutral-endermen` | `true` | Ordinary Enderman neutrality switch |
| `passives.fire-damage-multiplier` | 0.5 | Retained key with no runtime read in 1.4.18 |
| `passives.particles` | `true` | Passive ambient particles |
| `named-souls.akuma.cold-temperature-threshold` | 0.15 | Cold-biome temperature ceiling |
| `named-souls.akuma.cold-speed-bonus` | 0.15 | Akuma cold-biome movement bonus |
| `named-souls.rev.dimension-energy-multiplier` | 1.5 | Rev energy multiplier outside the Overworld |
| `named-souls.lamari.armor-toughness` | 4 | Lamari armor-toughness bonus |

## Common ability fields

Each `abilities` entry has a MiniMessage `name`, `energy` cost, and `cooldown-seconds`. The registered defaults are:

| Ability path | Name | Energy | Cooldown |
|---|---|---:|---:|
| `abilities.wings` | Wings | 40 | 45 seconds |
| `abilities.roar` | Roar | 35 | 25 seconds |
| `abilities.akumas-trail` | Akuma's Trail | 25 | 12 seconds |
| `abilities.akumas-hush` | Akuma's Hush | 60 | 60 seconds |
| `abilities.absolute-zero` | Absolute Zero | 100 | 120 seconds |
| `abilities.revs-rend` | Rev's Rend | 25 | 12 seconds |
| `abilities.wrath-of-rev` | Wrath of Rev | 60 | 60 seconds |
| `abilities.infernos-wrath` | Inferno's Wrath | 100 | 120 seconds |
| `abilities.lamaris-fault` | Lamari's Fault | 30 | 18 seconds |
| `abilities.lamaris-reckoning` | Lamari's Reckoning | 60 | 60 seconds |
| `abilities.titans-bulwark` | Titan's Bulwark | 100 | 120 seconds |

`ultimate.shared-cooldown-seconds` is `120`. An ultimate still declares an energy cost of 100 and requires a full bar.

## Shared abilities

| Path | Shipped value | Purpose |
|---|---:|---|
| `abilities.wings.duration-seconds` | 8 | Temporary flight duration |
| `abilities.roar.radius` | 8 | Living-target search radius |
| `abilities.roar.weakness-seconds` | 5 | Weakness duration |

## Akuma's Trail

| Path | Shipped value | Purpose |
|---|---:|---|
| `abilities.akumas-trail.distance` | 10 | Trail distance |
| `abilities.akumas-trail.dash-strength` | 2.35 | Forward velocity |
| `abilities.akumas-trail.ice-duration-seconds` | 6 | Temporary ice lifetime |
| `abilities.akumas-trail.water-detection-radius` | 5 | Nearby-water scan radius |
| `abilities.akumas-trail.water-entry-seconds` | 3 | Water-entry support time |
| `abilities.akumas-trail.water-run-seconds` | 8 | Water-running window |
| `abilities.akumas-trail.water-run-speed-amplifier` | 2 | Water-run Speed amplifier |
| `abilities.akumas-trail.water-freeze-radius` | 2 | Water-freezing radius |
| `abilities.akumas-trail.brittle-slowness-amplifier` | 2 | Brittle Slowness amplifier |

## Akuma's Hush

| Path | Shipped value | Purpose |
|---|---:|---|
| `abilities.akumas-hush.radius` | 9 | Silence field radius |
| `abilities.akumas-hush.duration-seconds` | 10 | Field duration |
| `abilities.akumas-hush.brittle-extension-ticks` | 10 | Brittle time added by a qualifying hit |
| `abilities.akumas-hush.brittle-maximum-remaining-seconds` | 4 | Maximum remaining Brittle time after extension |

## Absolute Zero

Core combat settings:

| Path | Shipped value | Purpose |
|---|---:|---|
| `abilities.absolute-zero.radius` | 9 | Main radius |
| `abilities.absolute-zero.burst-damage` | 14 | Initial burst damage |
| `abilities.absolute-zero.frost-damage` | 2 | Frost damage |
| `abilities.absolute-zero.frost-duration-seconds` | 5 | Frost duration |
| `abilities.absolute-zero.trap-duration-seconds` | 4 | Ice trap duration |
| `abilities.absolute-zero.slowness-amplifier` | 4 | Slowness amplifier |
| `abilities.absolute-zero.mining-fatigue-amplifier` | 3 | Mining Fatigue amplifier |
| `abilities.absolute-zero.screen-freeze-ticks` | 60 | Caster freeze presentation time |
| `abilities.absolute-zero.brittle-duration-seconds` | 6 | Brittle duration |
| `abilities.absolute-zero.shatter-bonus-damage` | 4 | Brittle shatter bonus damage |
| `abilities.absolute-zero.shatter-particles` | 20 | Shatter particle count |

Presentation:

| Path | Shipped value |
|---|---:|
| `abilities.absolute-zero.presentation.impact-delay-ticks` | 12 |
| `abilities.absolute-zero.presentation.anticipation-particles` | 32 |
| `abilities.absolute-zero.presentation.impact-particles` | 96 |
| `abilities.absolute-zero.presentation.active-particles-per-target` | 6 |
| `abilities.absolute-zero.presentation.ending-particles` | 24 |
| `abilities.absolute-zero.presentation.shell-display-count` | 8 |
| `abilities.absolute-zero.presentation.shell-display-scale` | 0.42 |
| `abilities.absolute-zero.presentation.shell-orbit-radius` | 0.38 |
| `abilities.absolute-zero.presentation.shell-rotation-radians-per-tick` | 0.09 |
| `abilities.absolute-zero.presentation.shell-break-particles` | 20 |

Drawback:

| Path | Shipped value |
|---|---|
| `abilities.absolute-zero.downside.name` | `Frostbite` |
| `abilities.absolute-zero.downside.duration-seconds` | 4 |
| `abilities.absolute-zero.downside.primary-amplifier` | 1 |
| `abilities.absolute-zero.downside.secondary-amplifier` | 0 |
| `abilities.absolute-zero.downside.energy-regeneration-lock-seconds` | 5 |

## Rev's Rend

| Path | Shipped value | Purpose |
|---|---:|---|
| `abilities.revs-rend.dash-strength` | 1.65 | Initial forward velocity |
| `abilities.revs-rend.vertical-lift` | 0.08 | Initial vertical velocity |
| `abilities.revs-rend.maximum-dash-ticks` | 16 | Maximum cross-detection time |
| `abilities.revs-rend.maximum-distance` | 9 | Maximum dash distance |
| `abilities.revs-rend.cross-detection-radius` | 1.35 | Crossed-target detection radius |
| `abilities.revs-rend.heat-per-cross` | 8 | Heat gained per valid crossing |
| `abilities.revs-rend.pressure-fire-ticks` | 30 | Crossing fire duration |
| `abilities.revs-rend.recast-window-ticks` | 36 | Follow-up cast window |
| `abilities.revs-rend.recast-range` | 14 | Follow-up target range |
| `abilities.revs-rend.recast-aim-cone-degrees` | 24 | Follow-up aim cone |
| `abilities.revs-rend.recast-surge-strength` | 1.45 | Follow-up movement strength |
| `abilities.revs-rend.recast-travel-ticks` | 8 | Follow-up travel time |
| `abilities.revs-rend.predators-claim-damage` | 6 | Finisher damage |
| `abilities.revs-rend.predators-claim-lift` | 0.22 | Finisher target lift |

Presentation:

| Path | Shipped value |
|---|---:|
| `abilities.revs-rend.presentation.dash-particles` | 5 |
| `abilities.revs-rend.presentation.cross-particles` | 12 |
| `abilities.revs-rend.presentation.recast-arc-particles` | 14 |
| `abilities.revs-rend.presentation.finisher-arc-particles` | 24 |

Sounds:

| Path | Shipped value |
|---|---|
| `abilities.revs-rend.sounds.recast-ready` | `ITEM_FIRECHARGE_USE` |
| `abilities.revs-rend.sounds.recast-ready-volume` | 0.75 |
| `abilities.revs-rend.sounds.recast-ready-pitch` | 1.3 |
| `abilities.revs-rend.sounds.recast-failed` | `BLOCK_NOTE_BLOCK_BASS` |
| `abilities.revs-rend.sounds.recast-failed-volume` | 0.65 |
| `abilities.revs-rend.sounds.recast-failed-pitch` | 0.7 |
| `abilities.revs-rend.sounds.recast-surge` | `ENTITY_BLAZE_SHOOT` |
| `abilities.revs-rend.sounds.recast-surge-volume` | 0.9 |
| `abilities.revs-rend.sounds.recast-surge-pitch` | 1.15 |
| `abilities.revs-rend.sounds.predators-claim` | `ENTITY_ENDER_DRAGON_GROWL` |
| `abilities.revs-rend.sounds.predators-claim-volume` | 1.0 |
| `abilities.revs-rend.sounds.predators-claim-pitch` | 1.45 |

## Wrath of Rev

| Path | Shipped value | Purpose |
|---|---:|---|
| `abilities.wrath-of-rev.radius` | 7 | Pulse radius |
| `abilities.wrath-of-rev.pulses` | 6 | Pulse count |
| `abilities.wrath-of-rev.pulse-interval-ticks` | 16 | Time between pulses |
| `abilities.wrath-of-rev.heat-per-distinct-target` | 6 | Heat gained once per target |
| `abilities.wrath-of-rev.pressure-damage` | 1.5 | Pressure hit damage |
| `abilities.wrath-of-rev.pressure-fire-ticks` | 30 | Pressure fire duration |
| `abilities.wrath-of-rev.pursuit-surge-heat-threshold` | 60 | Heat required for enhanced pursuit |
| `abilities.wrath-of-rev.pursuit-surge-range` | 11 | Pursuit target range |
| `abilities.wrath-of-rev.pursuit-surge-strength` | 0.65 | Pursuit horizontal velocity |
| `abilities.wrath-of-rev.pursuit-surge-lift` | 0.12 | Pursuit vertical velocity |
| `abilities.wrath-of-rev.fallback-speed-ticks` | 30 | Fallback Speed duration |
| `abilities.wrath-of-rev.fallback-speed-amplifier` | 1 | Fallback Speed amplifier |
| `abilities.wrath-of-rev.presentation.pulse-particles` | 34 | Particles requested per pulse |

Sounds:

| Path | Shipped value |
|---|---|
| `abilities.wrath-of-rev.sounds.pulse` | `ENTITY_BLAZE_SHOOT` |
| `abilities.wrath-of-rev.sounds.pulse-volume` | 0.7 |
| `abilities.wrath-of-rev.sounds.pulse-pitch` | 0.9 |
| `abilities.wrath-of-rev.sounds.pursuit-surge` | `ENTITY_BLAZE_SHOOT` |
| `abilities.wrath-of-rev.sounds.pursuit-surge-volume` | 0.9 |
| `abilities.wrath-of-rev.sounds.pursuit-surge-pitch` | 1.4 |

## Inferno's Wrath

| Path | Shipped value | Purpose |
|---|---:|---|
| `abilities.infernos-wrath.radius` | 9 | Initial impact radius |
| `abilities.infernos-wrath.impact-damage` | 6 | Initial impact damage |
| `abilities.infernos-wrath.pressure-fire-ticks` | 40 | Initial fire duration |
| `abilities.infernos-wrath.hunt-duration-ticks` | 200 | Hunt window |
| `abilities.infernos-wrath.initial-mobility-ticks` | 60 | Starting mobility time |
| `abilities.infernos-wrath.maximum-mobility-ticks` | 140 | Mobility time cap |
| `abilities.infernos-wrath.mobility-extension-ticks` | 20 | Mobility time gained from progress |
| `abilities.infernos-wrath.mobility-speed-amplifier` | 1 | Mobility Speed amplifier |
| `abilities.infernos-wrath.rampage-progress-per-hit` | 1 | Rampage progress per qualifying hit |
| `abilities.infernos-wrath.maximum-rampage` | 4 | Rampage cap |
| `abilities.infernos-wrath.rampage-grants-per-target` | 1 | Progress grants allowed from one target |

Presentation:

| Path | Shipped value |
|---|---:|
| `abilities.infernos-wrath.presentation.impact-delay-ticks` | 10 |
| `abilities.infernos-wrath.presentation.anticipation-particles` | 28 |
| `abilities.infernos-wrath.presentation.impact-particles` | 84 |
| `abilities.infernos-wrath.presentation.ring-points` | 32 |
| `abilities.infernos-wrath.presentation.ring-expand-ticks` | 30 |
| `abilities.infernos-wrath.presentation.ring-interval-ticks` | 5 |
| `abilities.infernos-wrath.presentation.rampage-arc-particles` | 10 |
| `abilities.infernos-wrath.presentation.ending-particles` | 24 |

Sounds:

| Path | Shipped value |
|---|---|
| `abilities.infernos-wrath.sounds.anticipation` | `ENTITY_BLAZE_AMBIENT` |
| `abilities.infernos-wrath.sounds.anticipation-volume` | 1.2 |
| `abilities.infernos-wrath.sounds.anticipation-pitch` | 0.55 |
| `abilities.infernos-wrath.sounds.impact` | `ENTITY_GENERIC_EXPLODE` |
| `abilities.infernos-wrath.sounds.impact-volume` | 1.25 |
| `abilities.infernos-wrath.sounds.impact-pitch` | 0.9 |
| `abilities.infernos-wrath.sounds.rampage-gain` | `ENTITY_BLAZE_SHOOT` |
| `abilities.infernos-wrath.sounds.rampage-gain-volume` | 0.65 |
| `abilities.infernos-wrath.sounds.rampage-gain-pitch` | 1.35 |
| `abilities.infernos-wrath.sounds.rampage-maximum` | `ENTITY_WITHER_SPAWN` |
| `abilities.infernos-wrath.sounds.rampage-maximum-volume` | 0.7 |
| `abilities.infernos-wrath.sounds.rampage-maximum-pitch` | 1.35 |
| `abilities.infernos-wrath.sounds.exhaustion` | `ENTITY_BLAZE_DEATH` |
| `abilities.infernos-wrath.sounds.exhaustion-volume` | 1.0 |
| `abilities.infernos-wrath.sounds.exhaustion-pitch` | 0.8 |

Drawback:

| Path | Shipped value |
|---|---|
| `abilities.infernos-wrath.downside.name` | `Burnout` |
| `abilities.infernos-wrath.downside.duration-seconds` | 4 |
| `abilities.infernos-wrath.downside.primary-amplifier` | 0 |
| `abilities.infernos-wrath.downside.secondary-amplifier` | 1 |
| `abilities.infernos-wrath.downside.energy-regeneration-lock-seconds` | 6 |

## Lamari's Fault and Reckoning

| Path | Shipped value | Purpose |
|---|---:|---|
| `abilities.lamaris-fault.slam-speed` | 2.4 | Downward slam velocity |
| `abilities.lamaris-fault.radius` | 7 | Landing effect radius |
| `abilities.lamaris-fault.fatigue-seconds` | 8 | Mining Fatigue duration |
| `abilities.lamaris-reckoning.radius` | 10 | Reckoning target radius |

## Titan's Bulwark

| Path | Shipped value | Purpose |
|---|---:|---|
| `abilities.titans-bulwark.duration-seconds` | 7 | Active time |
| `abilities.titans-bulwark.resistance-amplifier` | 3 | Active Resistance amplifier |
| `abilities.titans-bulwark.reflected-damage-fraction` | 0.4 | Incoming damage fraction reflected |
| `abilities.titans-bulwark.reflected-slowness-seconds` | 2 | Slowness time on an attacker |
| `abilities.titans-bulwark.stored-damage-fraction` | 0.35 | Incoming damage fraction stored |
| `abilities.titans-bulwark.stored-damage-cap` | 16 | Stored damage cap |
| `abilities.titans-bulwark.expiry-radius` | 4 | Expiry burst radius |
| `abilities.titans-bulwark.expiry-min-damage` | 1 | Minimum burst damage |
| `abilities.titans-bulwark.expiry-max-damage` | 6 | Maximum burst damage |
| `abilities.titans-bulwark.expiry-min-knockback` | 0.8 | Minimum burst knockback |
| `abilities.titans-bulwark.expiry-max-knockback` | 1.5 | Maximum burst knockback |
| `abilities.titans-bulwark.presentation.impact-delay-ticks` | 8 | Startup presentation delay |
| `abilities.titans-bulwark.presentation.active-particles` | 4 | Active particle count |
| `abilities.titans-bulwark.presentation.ending-particles` | 48 | Expiry particle count |

Drawback:

| Path | Shipped value |
|---|---|
| `abilities.titans-bulwark.downside.name` | `Stone Fatigue` |
| `abilities.titans-bulwark.downside.active-slowness-amplifier` | 1 |
| `abilities.titans-bulwark.downside.duration-seconds` | 4 |
| `abilities.titans-bulwark.downside.primary-amplifier` | 1 |
| `abilities.titans-bulwark.downside.secondary-amplifier` | 0 |
| `abilities.titans-bulwark.downside.energy-regeneration-lock-seconds` | 5 |

## Rev Hunt

Heat bar:

| Path | Shipped value |
|---|---|
| `rev-hunt.heat-bar.enabled` | `true` |
| `rev-hunt.heat-bar.title` | `<red><bold>REV HEAT</bold></red> <gray><heat>/<maximum> \| <tier></gray>` |
| `rev-hunt.heat-bar.stalking-color` | `YELLOW` |
| `rev-hunt.heat-bar.pursuing-color` | `RED` |
| `rev-hunt.heat-bar.predator-color` | `PURPLE` |
| `rev-hunt.heat-bar.overlay` | `PROGRESS` |

Heat rules:

| Path | Shipped value | Purpose |
|---|---:|---|
| `rev-hunt.heat.maximum` | 100 | Heat cap |
| `rev-hunt.heat.damage-marked-gain` | 4 | Heat for damaging a marked target |
| `rev-hunt.heat.per-target-gain-cooldown-ticks` | 20 | Per-target gain delay |
| `rev-hunt.heat.maximum-gains-per-player` | 20 | Direct gains allowed from one player |
| `rev-hunt.heat.maximum-gains-per-mob` | 3 | Direct gains allowed from one mob |
| `rev-hunt.heat.minimum-mob-max-health` | 12 | Minimum mob maximum health |
| `rev-hunt.heat.decay-delay-ticks` | 100 | Delay before decay |
| `rev-hunt.heat.decay-interval-ticks` | 20 | Time between decay steps |
| `rev-hunt.heat.decay-amount` | 2 | Heat removed per step |
| `rev-hunt.heat.mobility-threshold` | 35 | Mobility tier threshold |
| `rev-hunt.heat.tracking-threshold` | 65 | Tracking tier threshold |
| `rev-hunt.heat.threshold-speed-ticks` | 30 | Threshold Speed refresh time |
| `rev-hunt.heat.threshold-speed-amplifier` | 0 | Threshold Speed amplifier |

Mark, finisher, and tracking:

| Path | Shipped value | Purpose |
|---|---:|---|
| `rev-hunt.mark.duration-ticks` | 120 | Initial mark duration |
| `rev-hunt.mark.maximum-remaining-ticks` | 180 | Mark extension cap |
| `rev-hunt.mark.maximum-targets` | 8 | Marked target cap |
| `rev-hunt.finisher.armed-duration-ticks` | 120 | Predator's Claim window |
| `rev-hunt.finisher.reset-heat-on-consume` | `true` | Reset Heat on finisher use |
| `rev-hunt.tracking.range` | 24 | Tracking range |
| `rev-hunt.tracking.maximum-target-cues` | 4 | Direction cue cap |
| `rev-hunt.tracking.particles-per-cue` | 4 | Flame points per cue |
| `rev-hunt.tracking.directional-cue-distance` | 1.8 | Cue distance from Rev |
| `rev-hunt.tracking.flame-arc-height` | 0.7 | Cue arc height |

Sounds:

| Path | Shipped value |
|---|---|
| `rev-hunt.sounds.tier-gain` | `ITEM_FIRECHARGE_USE` |
| `rev-hunt.sounds.tier-gain-volume` | 0.75 |
| `rev-hunt.sounds.tier-gain-pitch` | 1.25 |
| `rev-hunt.sounds.finisher-ready` | `ENTITY_WITHER_SPAWN` |
| `rev-hunt.sounds.finisher-ready-volume` | 0.65 |
| `rev-hunt.sounds.finisher-ready-pitch` | 1.45 |

## Resonance globals

| Path | Shipped value | Purpose |
|---|---:|---|
| `resonances.unlock-range-blocks` | 50 | Unlock and availability range |
| `resonances.ending-particles` | 32 | Default ending particle count |

Pair resonances cost 70 energy. Dragon Trinity costs 100. Every resonance cooldown ships at 720 seconds.

## Thermal Convergence

| Group | Shipped settings |
|---|---|
| Identity | `name: Thermal Convergence`; `energy: 70`; `cooldown-seconds: 720` |
| Timing and area | `radius: 11`; `active-seconds: 6`; `pulse-interval-ticks: 20`; `impact-delay-ticks: 20` |
| Damage and movement | `initial-damage: 12`; `pulse-damage: 2`; `finisher-damage: 6`; `pull-strength: 0.45`; `finisher-knockback: 1.1` |
| Effects | `debuff-seconds: 5`; `slowness-amplifier: 2`; `weakness-amplifier: 1`; `fire-seconds: 3`; `freeze-ticks: 50`; `speed-amplifier: 1` |
| Displays | `displays-per-player: 3`; `display-scale: 0.34`; `display-orbit-radius: 0.75`; `link-points: 14` |
| Particles | `active-particles: 12`; `impact-particles: 88`; `pulse-particles: 36`; `flash-particles: 2`; `finisher-particles: 64` |

Every setting in this table is below `resonances.thermal-convergence`.

## Volcanic Aegis

| Group | Shipped settings |
|---|---|
| Identity | `name: Volcanic Aegis`; `energy: 70`; `cooldown-seconds: 720` |
| Timing and area | `active-seconds: 10`; `pulse-radius: 6`; `pulse-interval-ticks: 40`; `finisher-radius: 10`; `impact-delay-ticks: 18` |
| Damage | `initial-damage: 10`; `pulse-damage: 2`; `finisher-damage: 8`; `finisher-knockback: 1.6` |
| Buffs and fire | `fire-seconds: 6`; `resistance-amplifier: 1`; `absorption-amplifier: 2` |
| Retaliation | `retaliation-damage: 3`; `retaliation-fire-seconds: 3`; `retaliation-cooldown-ticks: 30` |
| Displays | `displays-per-player: 5`; `display-scale: 0.42`; `display-orbit-radius: 0.72` |
| Particles | `active-particles-per-player: 8`; `impact-particles: 64`; `pulse-particles: 30`; `retaliation-particles: 18`; `finisher-particles: 72` |

Every setting in this table is below `resonances.volcanic-aegis`.

## Glacial Bastion

| Group | Shipped settings |
|---|---|
| Identity | `name: Glacial Bastion`; `energy: 70`; `cooldown-seconds: 720` |
| Timing and area | `active-seconds: 10`; `domain-radius: 7`; `pulse-interval-ticks: 20`; `finisher-radius: 11`; `impact-delay-ticks: 18` |
| Damage | `pulse-damage: 1`; `finisher-damage: 8`; `finisher-knockback: 0.9` |
| Effects | `debuff-seconds: 4`; `resistance-amplifier: 1`; `absorption-amplifier: 2`; `slowness-amplifier: 3`; `mining-fatigue-amplifier: 2`; `freeze-ticks: 80` |
| Ward | `ward-charges: 2`; `ward-damage-reduction-fraction: 0.5` |
| Displays | `displays-per-player: 5`; `display-scale: 0.4`; `display-orbit-radius: 0.78` |
| Particles | `active-particles-per-player: 8`; `impact-particles: 72`; `pulse-particles: 32`; `ward-break-particles: 28`; `finisher-particles: 80` |

Every setting in this table is below `resonances.glacial-bastion`.

## Dragon Trinity

| Group | Shipped settings |
|---|---|
| Identity | `name: Dragon Trinity`; `energy: 100`; `cooldown-seconds: 720` |
| Timing and area | `radius: 16`; `active-seconds: 12`; `pulse-interval-ticks: 60`; `finisher-radius: 18`; `impact-delay-ticks: 30` |
| Damage and movement | `initial-damage: 18`; `pulse-damage: 4`; `finisher-damage: 14`; `finisher-knockback: 2`; `pull-strength: 0.65` |
| Team effects | `reveal-seconds: 12`; `healing-per-pulse: 2`; `energy-per-pulse: 5`; `strength-amplifier: 1`; `speed-amplifier: 1`; `resistance-amplifier: 1`; `absorption-amplifier: 3` |
| Displays | `sigil-displays: 9`; `sigil-radius: 2.3`; `display-scale: 0.5`; `link-points: 16` |
| Particles | `anticipation-particles: 96`; `active-particles: 18`; `impact-particles: 128`; `pulse-particles: 72`; `flash-particles: 3`; `finisher-particles: 128` |

Every setting in this table is below `resonances.dragon-trinity`.

## Retained ability sections

These entries still migrate and validate, but DragonAltar 1.4.18 does not register them as castable abilities:

| Path | Shipped settings |
|---|---|
| `abilities.dash` | `name: Dash`; `energy: 20`; `cooldown-seconds: 8`; `strength: 1.8` |
| `abilities.sight` | `name: Sight`; `energy: 25`; `cooldown-seconds: 30`; `duration-seconds: 12`; `radius: 24` |
| `abilities.resolve` | `name: Resolve`; `energy: 35`; `cooldown-seconds: 35`; `resistance-seconds: 5`; `removed-effects: [POISON, WITHER, WEAKNESS, SLOWNESS, MINING_FATIGUE, BLINDNESS, NAUSEA, HUNGER, DARKNESS]` |

Changing retained values does not add these abilities to a player's registry.

See [Configuration](Configuration) for the other shipped files and [Troubleshooting](Troubleshooting) for safe reload checks.
