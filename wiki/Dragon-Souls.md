# Dragon Souls

DragonAltar creates exactly three persistent souls:

| Soul | Theme | Signature passive | Soul abilities |
|---|---|---|---|
| Akuma | Frost | Freeze immunity and cold-biome speed | Akuma's Trail, Akuma's Hush, Absolute Zero |
| Rev | Hunt and flame | Fire immunity and faster energy recovery in the Nether and End | Rev's Rend, Wrath of Rev, Inferno's Wrath |
| Lamari | Stone | Fall immunity and armor toughness | Lamari's Fault, Lamari's Reckoning, Titan's Bulwark |

Every Dragonborn also gets two additional hearts, optional permanent Slow Falling, neutral Endermen, Wings, Roar, a Dragon Focus, and the Dragon Energy HUD.

One player can hold at most one soul. A soul can have at most one holder.

## Public soul states

| State | What players should understand |
|---|---|
| Held | A player currently carries the soul |
| Dormant | The soul has no public holder and is waiting, reserved, choosing, silenced, not yet awakened, or otherwise unavailable |
| Limbo | The Mother Soul has removed it for a timed period |
| Fractured | The soul has manifested as a hostile Wither Skeleton that must be defeated |

Public views intentionally combine several internal recovery stages into Dormant. Do not use the public label to infer private maintenance activity.

## First claims

After the Ancient Dragon falls, all three souls begin unclaimed and the altar becomes active. An eligible non-Dragonborn player can right-click the configured interaction block, present the initial ritual offerings, and claim the next available soul.

The ritual reserves a soul before taking items. If another plugin cancels reservation or ritual start, no claim occurs. When the ritual completes, the holder is assigned, passives and Focus are applied, energy is filled, and Wings becomes the initial selected ability.

When all three initial souls are claimed, the event completes and the altar becomes dormant.

## PvP inheritance

If a Dragonborn dies to an eligible non-Dragonborn player, the killer directly inherits that soul. The transfer uses the normal eligibility checks and applies the new holder's passives immediately.

If the killer already holds a soul, `transfer.dragonborn-killer-policy` decides what happens:

| Policy | Result |
|---|---|
| `RANDOM_ELIGIBLE` | The soul becomes pending, then runs the configured random online countdown |
| `OPEN_RITUAL_SLOT` | The soul becomes unclaimed and the altar opens for another initial-style ritual |
| `SOUL_DORMANT` | The soul is silenced until staff restore it |
| `PENDING_TRANSFER` | The soul waits in pending state for join or startup recovery |

The shipped policy is `RANDOM_ELIGIBLE`.

## Natural death and reincarnation

A death without an eligible non-Dragonborn killer sends the soul to pending transfer. By default, the server announces a 10-second countdown and then chooses a random eligible online player.

If no candidate is available, the soul stays pending. It is not deleted. The next eligible player join or a startup pending-transfer pass can recover it.

## Combat logging

Player-versus-player damage creates a two-way combat tag for 15 seconds by default. If a Dragonborn disconnects during the tag:

- The eligible non-Dragonborn opponent receives the soul when available
- Otherwise the soul becomes pending

The pending case does not run the ordinary death countdown immediately. Join or startup recovery handles it.

## Eligibility

The same eligibility service is used by initial rituals, random reincarnation, limbo release, and most automated transfers. Every check must pass:

- Online
- In an allowed game mode
- At or above the minimum playtime
- Has the configured required permission, when one is set
- Does not have the exclusion permission
- Does not already hold a Dragon Soul
- Not marked AFK by common metadata
- Not marked vanished by common metadata
- Alive and not in Spectator mode
- Past the configured post-join grace period

The default allows Survival and Adventure, has no playtime or join delay, and uses `dragonaltar.eligibility.excluded` as the opt-out permission.

## Limbo and blackout

A Mother Soul backfire can send one or more souls into limbo for 12 hours by default. Limbo uses real elapsed time and survives restart.

When a limbo timer expires, the soul waits until an eligible online player exists. The former holder is excluded from that release. If no candidate is online, the public history says the soul is ready and awaiting an eligible player.

If two Dragonborn participate as non-target callers in the same removal ritual and the backfire roll succeeds, all three souls enter limbo. This is the total blackout rule.

## Fractured Souls

After instability becomes active, a successful removal ritual can fracture the target soul instead of transferring it. The manifested creature is a persistent Wither Skeleton with:

- A visible `Fractured <Soul>` name
- 150 health
- Increased movement speed
- 8 to 12 base attack damage
- Partial knockback resistance
- No drops or experience
- A global boss bar with periodically refreshed coordinates
- Safe teleports 8 to 32 blocks away every 45 to 60 seconds by default

Dragonborn can damage it, but the plugin blocks their killing blow. A non-Dragonborn player who lands the final blow claims the soul directly. Normal soul assignment rules still prevent an invalid duplicate holder. A death with no valid claimant causes the creature to return.

The entity and its next teleport time are persisted. On restart or chunk load, DragonAltar keeps one canonical creature and removes duplicates.

## Restart repair

DragonAltar repairs interrupted soul transitions conservatively:

- A reservation with no reserved player returns to unclaimed
- An interrupted transfer with no holder returns to pending
- Missing or stale active reservations are released
- Duplicate holders are repaired into pending recovery
- Limbo and fracture records are rebuilt or reconciled

The plugin never creates a fourth soul during repair.

## Public history

Players can run:

```text
/dragon history
```

The GUI shows all three souls, their public status, current holder when public, previous public holders, latest event, and limbo return time. Selecting a soul opens a paginated timeline with 45 events per page.

Chat history is available with:

```text
/dragon history <player>
```

Console `/dragon history` is an operational diagnostic and its output must remain private. Player-facing history uses neutral descriptions for maintenance changes and does not reveal private operators, private provenance, or hidden holder details.

See [Rituals](Rituals), [Abilities](Abilities), and [Data and Privacy](Data-and-Privacy).
