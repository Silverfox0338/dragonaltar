# Troubleshooting

Start with read-only checks:

```text
/dragon system version
/dragon system status
/dragon system validate
/dragon system health
/dragon altar validate
/dragon event status
```

Do not start with a reset. Preserve evidence and take a stopped-server backup before any confirmed repair.

## Plugin disables during startup

DragonAltar disables itself when editable configuration fails validation.

1. Read every DragonAltar validation line in console.
2. Fix only the named path.
3. Check YAML indentation and quoting.
4. Restart Paper.
5. Run `/dragon system validate`.

Common causes:

- Invalid material, sound, or particle name
- Bad server mode or transfer policy
- Negative duration
- Radius above a hard ceiling
- Fractured teleport maximum below its minimum
- Energy maximum changed from 100
- Invalid Rev Heat threshold order
- Inferno mobility cap above Hunt duration
- Bulwark maximum below minimum
- Invalid ritual mode or Elytra priority
- Animation action misspelling

If `/dragon system reload` encounters invalid configuration, it can disable the plugin. Use a restart after correcting it.

## Setup says a point is missing

Confirm the setup session:

```text
/dragon setup status
```

Staged points are not saved until:

```text
/dragon setup save
/dragon confirm <token>
```

If the session expired after 15 minutes of inactivity, begin again and restage the missing values.

## Fountain validation fails

Check:

- Saved world is an End environment
- `event.end-world` matches the intended world
- Bedrock exists within 6 horizontal blocks and the allowed vertical search
- Crystal points are in the same world
- Each crystal is 2 to 6 horizontal blocks from the fountain
- Vertical difference is no more than 4 blocks
- North is actually north, and likewise for south, east, and west

Use explicit coordinates when standing on a block would save an imprecise player location.

## Egg location is invalid

The saved egg display point cannot be inside a solid block. Move it to clear space, save setup, and run:

```text
/dragon dev altar repair-displays
/dragon altar egg inspect
```

## Duplicate or missing displays

Inspect:

```text
/dragon altar egg inspect
/dragon altar recipe inspect
/dragon dev altar displays
```

Reconcile:

```text
/dragon dev altar repair-displays
```

If the altar is dormant or has no unclaimed souls, missing claim displays are expected. Use `/dragon altar status` before forcing a preview count.

## Protection will not enable

The shipped file contains both `internal-protection.enabled` and legacy `protection.enabled`. Version 1.4.18 reads the internal setting first.

Set:

```yaml
internal-protection:
  enabled: true
```

Then restart or reload and run:

```text
/dragon protection status
```

Also confirm both corners are in the same world.

## Official event will not start

Run:

```text
/dragon event preview
```

The preview reports preflight failures. Typical blockers:

- Existing soul records
- Invalid setup
- Invalid fountain or crystal ring
- Existing dragon
- Active End battle or respawn
- Occupied crystal site
- Event already started

Do not use a developer test dragon as a substitute for a failed official preflight.

## Event enters recovery required

Preserve loaded entities. Collect:

```text
/dragon event status
/dragon event dragon-info
/dragon event locate
/dragon system entities
/dragon system health
```

Then take a backup and preview:

```text
/dragon event rescan
```

or:

```text
/dragon event recover
```

One matching dragon can restore Active. Four matching event crystals can resume summoning. Multiple matching dragons or a partial crystal set requires manual review. Existing souls take priority and restore the altar path.

## Dragon died but altar did not awaken

Check whether it was the stored canonical dragon:

```text
/dragon event dragon-info
/dragon event status
```

Test dragons and unrelated dragons do not progress the official event. The normal death path also waits the configured reward and awakening delays.

If ScaledEnderDragon is installed, confirm it did not replace the vanilla respawn path and that the official dragon was still accepted by DragonAltar.

## Initial ritual will not open

The player must:

- Right-click the exact configured interaction block
- Wait until the official event is defeated
- Use an active altar with an unclaimed soul
- Not already be Dragonborn
- Pass every eligibility check
- Wait until no other initial ritual is active

Staff can inspect eligibility with:

```text
/dragon dev eligibility explain <player>
```

## Recipe says an item is missing

For Elytra, review:

- Enchantment acceptance
- Renamed-item acceptance
- Custom-lore acceptance
- Equipped chest-slot setting
- Blocked PDC keys
- Offering mode

Use:

```text
/dragon dev ritual test-elytra <player>
/dragon dev ritual recipe-plan <player>
```

The target must be online.

For pedestal modes, confirm dropped item entities are within 1.5 blocks of a configured pedestal. Items inside containers do not count as pedestal deposits.

## Ritual was interrupted and items are missing

Free inventory space and run:

```text
/dragon refunds
```

Staff can check and retry:

```text
/dragon admin refunds inspect <player>
/dragon admin refunds give <player>
```

The exact-item refund includes metadata. Overflow remains pending rather than being dropped.

## Mother Soul chest does nothing

The trigger runs when a player closes a qualifying single chest.

Confirm:

- It is not half of a double chest
- All four offering counts are present
- Weakness potions are drinkable regular or long variants
- The foundation blocks are exact
- All four White Candles are lit
- All four Soul Sand pads exist
- One player stands on each pad
- The closing player is one of the four callers
- At least one soul currently has a holder

## Mother Soul selection expires

The leader has 60 seconds by default. No offerings are consumed on expiry. Close the chest again after all callers are ready.

## Fractured Soul seems missing

The tracked chunk may be unloaded. DragonAltar waits for a known unloaded tracked entity rather than immediately spawning a duplicate.

Check the public boss bar and allow the tracked chunk to load. On chunk load, DragonAltar reconciles duplicates and keeps one canonical creature.

Do not summon a replacement manually.

## Fractured Soul returns after death

It returns when:

- There was no player killer
- A current Dragonborn would have landed the killing blow
- Normal soul assignment rejected the claimant

A non-Dragonborn player must land the accepted final blow and must not already hold a soul.

## Soul remains pending

Pending is recoverable. Check that at least one online player passes:

```text
/dragon dev eligibility list
/dragon dev eligibility explain <player>
```

Natural deaths use the countdown. Combat-log failures and the `PENDING_TRANSFER` collision policy can wait for join or startup recovery instead.

## Focus is missing

1. Ask the player to free an inventory slot.
2. Have them run `/dragon focus`.
3. If needed, run `/dragon admin repair <player>`.

The Focus is not restored into a full inventory and is never dropped on the ground.

## Ability will not cast

Check:

- Player is online, alive, and not Spectator
- `dragonaltar.use` is granted
- The selected ability belongs to the player's soul
- Current energy is sufficient
- Own and shared cooldowns are ready
- Lamari's Fault is cast while flying
- Lamari's Reckoning is cast on solid ground
- Resonance participants are still online, in the same world, in range, and off shared resonance cooldown
- Another plugin did not cancel selection or casting

Use:

```text
/dragon admin energy view <player>
/dragon admin cooldown view <player>
/dragon dev input status <player>
```

## Add-on registration fails

Match the error to these checks:

- Owner plugin is enabled
- Registration is on the primary server thread
- One add-on registration per owner
- Unique 2 to 32 character lowercase add-on id
- Namespace is not `dragonaltar`
- Metadata fields are nonblank
- Add-on is registered before its abilities
- Ability id starts with the exact add-on namespace and colon
- Local ability id is 2 to 48 lowercase characters
- Display name and category are present
- Energy is 0 to 100
- Cooldown is 0 through 24 hours
- At least one valid soul is listed
- Ability id does not collide with a built-in, resonance, or another add-on

Do not cast to `DragonAltarApiImpl`. Load `DragonAltarApi` from `ServicesManager` and declare `depend: [DragonAltar]`.

## Safe support bundle

Provide:

- DragonAltar 1.4.18
- Paper build
- Java version
- Reproduction steps
- Relevant configuration validation message
- Whether optional integrations are installed
- Whether the issue reproduces with shipped defaults on a test server

Remove names, UUIDs, paths, audit detail, network information, and unrelated secrets.
