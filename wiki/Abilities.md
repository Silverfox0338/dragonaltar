# Dragonborn Abilities

Every Dragonborn has a Dragon Focus, 100 Dragon Energy, two shared abilities, two soul abilities, and one full-energy ultimate.

Server owners looking for every `abilities.yml` value can use the [Ability Configuration](Ability-Configuration) reference.

The shipped ability list also contains older `dash`, `sight`, and `resolve` configuration sections. They are not registered as castable abilities in 1.4.18.

## Dragon Energy

| Setting | Shipped value |
|---|---:|
| Maximum | 100 |
| Regeneration | 2 energy |
| Regeneration interval | 20 ticks, normally 1 second |
| Delay after a successful cast | 60 ticks, normally 3 seconds |

Rev regenerates at 1.5 times the normal amount in the Nether and End. With the shipped value, each 2-point regeneration step is rounded up to 3.

A normal ability requires enough energy. An ultimate requires the bar to be completely full, regardless of its displayed configured cost. Energy and cooldown begin only after the cast pipeline succeeds.

Another plugin can cancel selection, casting, or energy changes through the public events.

## Selection and casting

Hold the Dragon Focus:

- Scroll to cycle abilities in the default `LOCKED` mode
- In `SNEAK_SCROLL` mode, sneak and scroll to cycle; ordinary scrolling changes hotbar slots
- Press swap hands while standing to cycle ability category
- Sneak and press swap hands to open the ability menu
- Right-click to cast the selected ability

Run `/dragon abilities` to list the currently available ability ids. Run `/dragon focus` if the Focus is missing and one inventory slot is free.

The HUD shows energy, selected ability, ability cooldown, shared ultimate cooldown, available resonance, resonance cooldown, and short combat status details.

## Shared passives

All three souls receive:

- Two additional hearts, which adds 4 health points
- Permanent Slow Falling while the player setting is enabled
- Neutral Endermen
- Passive cosmetic particles unless disabled

The shipped `passives.fire-damage-multiplier` value is retained in `abilities.yml` but is not read by 1.4.18. Rev's actual fire immunity is handled by its named passive.

## Shared abilities

| Ability | Energy | Cooldown | Effect |
|---|---:|---:|---|
| Wings | 40 | 45 seconds | Grants temporary flight for 8 seconds, restores the previous flight state, then grants 5 seconds of Slow Falling |
| Roar | 35 | 25 seconds | Pushes nearby valid targets in an 8-block radius and gives Weakness I for 5 seconds |

Wings remembers whether flight was already available before the cast. It does not permanently grant creative-style flight.

## Akuma

### Frostveil

Akuma is immune to freezing damage and has freeze ticks cleared. In blocks with a temperature of 0.15 or colder, Akuma gains a 15% movement-speed modifier.

### Akuma's Trail

| Energy | Cooldown |
|---:|---:|
| 25 | 12 seconds |

The cast has two forms.

Near water, within the shipped 5-block search:

1. A 3-second entry window begins.
2. Touching water during that window starts an 8-second Frost Run.
3. Akuma gains Speed III.
4. Exposed water in a 2-block radius becomes temporary Frosted Ice.
5. DragonAltar restores tracked water when the run ends.

Away from water:

- Akuma dashes horizontally with 2.35 strength
- A 10-block line of thin Packed Ice displays appears for 6 seconds
- Nearby targets receive Slowness II for 3 seconds
- Targets already carrying Brittle receive Slowness III instead

The ice trail is display-only. It does not replace terrain.

### Akuma's Hush

| Energy | Cooldown |
|---:|---:|
| 60 | 60 seconds |

Hush fixes a 9-block field at the cast location for 10 seconds. Every 10 ticks, targets in the field receive a fresh 30-tick Slowness III effect.

If a target is already Brittle, each pulse extends that Brittle timer by 10 ticks, but never beyond 4 seconds remaining from the current pulse time.

### Absolute Zero

| Energy | Cooldown |
|---:|---:|
| Full 100 | 120 seconds plus shared ultimate cooldown |

After a 12-tick warning:

- Visible targets in a 9-block radius take 14 damage
- They become Brittle for 6 seconds
- They receive Slowness V and Mining Fatigue IV for 4 seconds
- Player screen freeze is raised for players who allow screen effects
- Ice displays surround surviving targets
- Targets take 2 damage once per second for 5 seconds

Brittle is consumed by the next positive damaging hit and adds 4 damage. It can trigger only once per application.

After the active effect, Akuma suffers Frostbite: Slowness II and Weakness I for 4 seconds, with Dragon Energy regeneration locked for 5 seconds.

## Rev

### Cinderborn

Rev is immune to fire, fire tick, lava, and hot-floor damage. Dragon Energy regeneration is multiplied by 1.5 in the Nether and End.

### Heat and Inferno Marks

Rev's three soul abilities share a pursuit system.

| Rule | Shipped value |
|---|---:|
| Maximum Heat | 100 |
| Mark duration | 120 ticks |
| Maximum mark remaining time | 180 ticks |
| Maximum marked targets | 8 |
| Direct hit Heat | 4 |
| Heat gain cooldown per target | 20 ticks |
| Maximum gains from one player | 20 |
| Maximum gains from one mob | 3 |
| Minimum mob maximum health | 12 |
| Decay delay after gain | 100 ticks |
| Decay rate | 2 every 20 ticks |

Only direct player melee and player projectile damage against a valid marked target generates the ordinary 4 Heat. Fire ticks, scripted ability damage, and reflected damage do not.

Heat tiers:

- 0 to 34: Stalking
- 35 to 64: Pursuing, with a short Speed I cue on crossing the threshold
- 65 to 99: Predator, with tracking cues toward marked prey
- 100: Predator's Claim becomes armed

The dedicated Heat boss bar follows the player's HUD setting.

### Rev's Rend

| Energy | Cooldown |
|---:|---:|
| 25 | 12 seconds |

Rend launches Rev along the look direction for up to 16 ticks or 9 blocks. Crossing a valid target within 1.35 blocks:

- Applies or refreshes an Inferno Mark
- Adds 8 Heat, subject to Heat limits
- Applies 30 fire ticks
- Opens a 36-tick recast window

Cast Rend again during that window while aiming within 24 degrees of a visible marked target up to 14 blocks away. The recast surges toward that target. It does not consume another ability cost or start another cooldown.

If Predator's Claim is armed, a successful marked recast consumes it, deals 6 extra damage, lifts the target, and resets Heat by default.

### Wrath of Rev

| Energy | Cooldown |
|---:|---:|
| 60 | 60 seconds |

Wrath produces 6 pulses, one every 16 ticks, in a 7-block radius. Each visible valid target:

- Is marked on first contact
- Grants 6 Heat on that first contact, subject to Heat limits
- Takes 1.5 damage per pulse
- Receives 30 fire ticks

If Rev has at least 60 Heat on the final pulse, Rev surges toward the nearest visible marked target within 11 blocks. If none is reachable, Rev receives Speed II for 30 ticks.

### Inferno's Wrath

| Energy | Cooldown |
|---:|---:|
| Full 100 | 120 seconds plus shared ultimate cooldown |

After a 10-tick warning, visible valid prey within 9 blocks take 6 damage, burn, and receive Inferno Marks. A 200-tick Hunt begins.

Rev starts with 60 ticks of Speed II and Fire Resistance. During Hunt:

- The mobility window can grow by 20 ticks per Rampage grant
- It is capped at 140 ticks total
- Each marked target can grant Rampage only once
- Four Rampage points arm Predator's Claim
- Heat reaching 100 can also arm Predator's Claim
- The claim expires within its configured 120-tick window and never beyond Hunt
- A marked Rend recast consumes the claim

At the end of Hunt, unused claim state is cleared. Rev suffers Burnout: Slowness I and Weakness II for 4 seconds, with energy regeneration locked for 6 seconds.

## Lamari

### Stoneheart

Lamari is immune to fall damage and receives 4 armor toughness.

### Lamari's Fault

| Energy | Cooldown |
|---:|---:|
| 30 | 18 seconds |

Fault can be cast only while the player is in Bukkit's flying state. It turns flight off and drives Lamari downward at 2.4 speed.

On landing, a 7-block earth pulse:

- Pushes valid targets strongly outward
- Applies Mining Fatigue II for 8 seconds

The landing watcher expires after roughly 4 seconds if no ground contact is found.

### Lamari's Reckoning

| Energy | Cooldown |
|---:|---:|
| 60 | 60 seconds |

Reckoning requires solid ground. It launches valid targets within 10 blocks upward and slightly outward.

### Titan's Bulwark

| Energy | Cooldown |
|---:|---:|
| Full 100 | 120 seconds plus shared ultimate cooldown |

For 7 seconds, Lamari receives Resistance IV and active Slowness II.

While Bulwark is active:

- Melee attackers receive 40% of the final incoming damage as reflected damage
- The attacker receives Slowness II for 2 seconds
- Lamari stores 35% of damage prevented by defenses
- Stored charge is capped at 16 damage

At expiry, a visible-target burst within 4 blocks deals 1 to 6 damage and applies 0.8 to 1.5 knockback, scaled by stored charge.

Lamari then suffers Stone Fatigue: Mining Fatigue II and Weakness I for 4 seconds, with energy regeneration locked for 5 seconds.

## Cooldown rules

Each ability has its own persistent cooldown timestamp. All three built-in ultimates also use one persistent shared 120-second ultimate cooldown. Casting any ultimate starts both its own cooldown and the shared group cooldown.

Cooldowns survive logout and restart. Remaining seconds round upward in player and API views.

Resonance cooldowns have a separate group described in [Resonances](Resonances).

## Target and terrain safety

Ability target queries clamp radius to 64 blocks and process at most 128 living entities. Dead, invalid, removed, spectator, and armor-stand targets are excluded.

DragonAltar ability effects do not create real explosions. Temporary water freezing is tracked and reverted. Ability blocks and armor are displays rather than permanent terrain.

See [Player Guide](Player-Guide) for controls and [Configuration](Configuration) for every shipped default.
