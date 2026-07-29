# Dragon Soul Resonances

When different Dragonborn are close together, their kits gain a hidden sixth ability. Pair resonances need the matching two souls. Dragon Trinity takes priority when all three are together.

## Unlock rules

- Required holders must be online
- They must be in the same world
- Every required pair must be within 50 blocks by default
- Trinity is checked before pair resonance
- If Trinity is not available, a player uses the pair for the nearest compatible Dragonborn

The resonance appears in the ordinary ability list and can be selected with the Focus.

At cast time, the exact required participants must still be available and in range. After activation, the running sequence stops early if a participant logs out, dies, or changes world. Moving beyond unlock range after the cast does not by itself stop the active sequence.

## Cost and cooldown

| Resonance | Souls | Caster energy | Cooldown |
|---|---|---:|---:|
| Thermal Convergence | Akuma and Rev | 70 | 720 seconds |
| Volcanic Aegis | Rev and Lamari | 70 | 720 seconds |
| Glacial Bastion | Akuma and Lamari | 70 | 720 seconds |
| Dragon Trinity | Akuma, Rev, and Lamari | 100 | 720 seconds |

Only the caster pays the energy cost. A successful cast starts the ability cooldown and one persistent shared resonance cooldown for every participant. If any required participant's shared group cooldown is active, the cast is refused.

The shared cooldown survives restart. Moving apart does not clear it.

## Thermal Convergence

Akuma and Rev combine frost and flame.

After a 20-tick build:

- Both holders receive Speed II and Fire Resistance for 6 seconds
- Visible enemies within 11 blocks take 12 damage
- Targets are pulled inward
- Targets burn for 3 seconds
- Targets receive Slowness III and Weakness II for 5 seconds
- Freeze ticks are applied when the target allows screen effects

The field pulses once per second for 2 damage, reapplying the pressure effects. At the end, Thermal Rupture deals 6 damage and pushes targets outward.

## Volcanic Aegis

Rev and Lamari forge a 10-second defensive offense.

After an 18-tick build:

- Both holders receive Resistance II, Absorption III, and Fire Resistance
- Visible enemies near either participant, within 6 blocks, take 10 damage and burn
- The area pulses every 40 ticks for 2 damage and fire

While the Aegis is active, an attacker can trigger 3 retaliation damage and fire. Retaliation for the same attacker and defender pair has a 30-tick cooldown.

The final Aegis Eruption hits visible enemies within 10 blocks of the team center for 8 damage, strong knockback, and fire.

## Glacial Bastion

Akuma and Lamari form a 10-second warded domain.

After an 18-tick build:

- Both holders receive Resistance II and Absorption III
- Each receives two Crystal Ward charges
- Each charge reduces one positive incoming hit by 50%, then breaks

Every second, visible enemies within 7 blocks of either participant take 1 damage and receive Slowness IV, Mining Fatigue III, and freeze pressure for 4 seconds.

The final Bastion Shatter hits visible enemies within 11 blocks of the team center for 8 damage and knockback.

Unused ward charges are removed when the sequence ends.

## Dragon Trinity

All three souls must be online, in one world, and mutually within 50 blocks.

After a 30-tick build:

- All three holders receive Strength II, Speed II, Resistance II, and Absorption IV for 12 seconds
- Visible enemies within 16 blocks take 18 damage
- Enemies are pulled inward and glow for 12 seconds

Every 60 ticks during the active phase:

- Enemies in the 16-block field take 4 damage and are pulled inward
- Each Dragonborn heals 2 health points
- Each Dragonborn regains 5 Dragon Energy

Dragonfall ends the sequence with 14 damage and strong knockback in an 18-block radius.

## Safety and accessibility

Resonance target queries use the same 64-block and 128-entity hard caps as ordinary abilities. Participants are never selected as targets.

HUD, sound, title, particle, and screen-effect preferences are honored. Turning presentation down does not change damage, defense, healing, energy, timing, or cooldowns.

See [Abilities](Abilities) for Focus controls and [Configuration](Configuration) for tuning details.
