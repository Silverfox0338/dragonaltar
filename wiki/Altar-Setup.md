# Setting Up the Altar

DragonAltar records exact staff-chosen locations. It does not build, paste, or replace your physical altar.

The guided setup keeps changes in a temporary session until you save them. A session expires after 15 minutes of inactivity.

## Required points

| Point | Setup command | Purpose |
|---|---|---|
| Altar center | `/dragon setup setaltarcenter` | Animation and administrative reference point |
| Ritual center | `/dragon setup setritualcenter` | Center of the initial claim ritual |
| Egg display | `/dragon setup setegg` | Anchor for the protected egg `BlockDisplay` and recipe text |
| Interaction | `/dragon setup setinteraction` | Block players right-click to open the initial ritual |
| Arrival | `/dragon setup setarrival` | Destination during the ritual's Ascension phase |
| End fountain | `/dragon setup setfountain` | Center of the official vanilla respawn sequence |
| North crystal | `/dragon setup setcrystal north` | North respawn crystal site |
| South crystal | `/dragon setup setcrystal south` | South respawn crystal site |
| East crystal | `/dragon setup setcrystal east` | East respawn crystal site |
| West crystal | `/dragon setup setcrystal west` | West respawn crystal site |

The ritual center falls back to the altar center during validation, but recording both makes later maintenance clearer.

Pedestals are required only when `ritual.yml` uses `PEDESTAL_DEPOSIT` or `HYBRID`. The internal protection corners are optional.

## Guided setup

1. Stand in the correct world and run:

   ```text
   /dragon setup begin
   ```

2. Record the altar points:

   ```text
   /dragon setup setaltarcenter
   /dragon setup setritualcenter
   /dragon setup setegg
   /dragon setup setinteraction
   /dragon setup setarrival
   ```

3. Travel to the End fountain and record it:

   ```text
   /dragon setup setfountain
   ```

4. Record all four crystal sites:

   ```text
   /dragon setup setcrystal north
   /dragon setup setcrystal south
   /dragon setup setcrystal east
   /dragon setup setcrystal west
   ```

5. Preview and inspect the staged values:

   ```text
   /dragon setup status
   /dragon setup preview
   /dragon setup validate
   ```

6. Save the staged setup. Saving issues a destructive-change preview because it overwrites saved locations:

   ```text
   /dragon setup save
   /dragon confirm <token>
   ```

7. Run validation again:

   ```text
   /dragon setup begin
   /dragon setup validate
   /dragon setup cancel
   /dragon altar validate
   ```

The second short setup session is needed because `/dragon setup validate` expects an active setup session. `/dragon altar validate` checks the saved setup without one.

## Exact coordinates

Point commands accept the player's current position or explicit coordinates:

```text
/dragon setup setegg <x> <y> <z> [yaw] [pitch]
/dragon setup setcrystal <north|south|east|west> [x y z [yaw] [pitch]]
```

The same coordinate form works for altar center, ritual center, interaction, arrival, fountain, and the setup `pos1` and `pos2` commands.

Locations store the world UUID, world name, x, y, z, yaw, and pitch. If a world was renamed, the UUID lets the plugin resolve it first.

## Fountain validation

The fountain must:

- Be in an End environment
- Match `event.end-world` when that setting is not blank
- Have bedrock somewhere within 6 blocks on x and z and from 3 blocks below to 4 blocks above the saved point

Each saved crystal point must:

- Be in the fountain's world
- Be 2 to 6 horizontal blocks from the fountain
- Be no more than 4 vertical blocks from it
- Actually lie in its named cardinal direction

The egg display point must not be inside a solid block. Duplicate plugin-owned altar displays also make setup validation fail.

## Pedestal setup

For `PEDESTAL_DEPOSIT` or `HYBRID`, give each configured requirement a pedestal entry:

```text
/dragon setup setpedestal <id>
```

This command records the player's current location. Remove a staged pedestal with:

```text
/dragon setup removepedestal <id>
```

Dropped item entities within 1.5 blocks of configured pedestal points are counted. The default `INVENTORY_CONSUME` mode needs no pedestals.

## Egg and recipe displays

The egg is a persistent, invulnerable `BlockDisplay`. The floating recipe is a `TextDisplay`. Both are tagged and reconciled so duplicate plugin displays can be removed.

Useful checks:

```text
/dragon altar egg inspect
/dragon altar recipe inspect
/dragon dev altar displays
/dragon dev altar repair-displays
```

The recipe display offset is relative to the egg. To move it to your exact current location:

```text
/dragon altar recipe move
/dragon confirm <token>
```

You may also provide `<x> <y> <z>` in the egg display's world.

## Internal protection

The optional cuboid protection blocks breaking, placing, fluids, fire spread, piston changes, explosion block damage, structure growth, unsafe altar interaction, and several entity placement or manipulation paths inside the region.

Set the corners with the guided setup `pos1` and `pos2` commands, or with the protection commands after setup.

The active 1.4.23 switch is:

```yaml
internal-protection:
  enabled: true
```

`/dragon protection enable|disable` updates `internal-protection.enabled`.

When `internal-protection.required-for-event` is true, official event validation requires protection to be enabled with both corners configured in the same world.

Players with `dragonaltar.protection.bypass` can toggle a per-session bypass with `/dragon protection bypass`. The bypass is not a persistent configuration change.

## Production readiness

Before the official event:

- `/dragon altar validate` reports ready
- `/dragon system validate` reports valid
- `/dragon system health` reports healthy
- The fountain and four crystal points have been checked on the live Paper build
- No dragon is already alive in the fountain world
- No Ender Dragon battle or respawn sequence is already active
- The four crystal sites are free
- The egg and recipe displays appear only once
- A stopped-server backup exists
- `server-mode` remains `BETA` until the full test is complete

Continue with [Ancient Dragon Event](Ancient-Dragon-Event).
