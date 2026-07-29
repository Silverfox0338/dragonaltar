# Rituals

DragonAltar has two different rituals:

- The initial Dragonborn ritual claims one of the three unclaimed souls at the official altar
- The Mother Soul removal ritual is a player-built four-person ceremony that removes an existing holder's soul

They use different structures, recipes, and recovery rules.

## Initial Dragonborn ritual

An eligible non-Dragonborn player right-clicks the configured altar interaction block. If the event has reached the awakened altar, an unclaimed soul remains, and no other initial ritual is active, the recipe screen opens.

The shipped recipe is:

| Offering | Amount | Default acceptance |
|---|---:|---|
| Elytra | 1 | Any durability, enchanted, renamed, or custom lore; equipped chest slot excluded |
| Nether Star | 1 | Any ordinary item stack of this material |
| Dragon's Breath | 16 | Any ordinary item stack of this material |
| End Crystal | 8 | Any ordinary item stack of this material |
| Echo Shard | 4 | Any ordinary item stack of this material |

The default Elytra policy consumes the most damaged acceptable Elytra first. Other supported policies are least damaged, first matching inventory slot, and lowest total enchantment level.

The shipped `accept-any-durability` and offering `durability-mode` keys do not add a separate runtime check in 1.4.18. Elytra durability is accepted in practice. Enchantment, custom name, custom lore, equipped-slot, blocked-PDC, and selection-priority settings are active.

## Offering modes

| Mode | Source order |
|---|---|
| `INVENTORY_CONSUME` | Player inventory only |
| `PEDESTAL_DEPOSIT` | Dropped items near configured pedestals only |
| `HYBRID` | Inventory first, then pedestal items for the remainder |

Pedestal item entities are found within 1.5 blocks of any configured pedestal.

The complete plan is checked before consumption. Immediately before removing items, DragonAltar verifies that every selected stack or entity still matches. If anything changed, it takes nothing.

## Initial ritual phases

| Phase | Default duration |
|---|---:|
| `OFFERINGS_ACCEPTED` | 40 ticks, 2 seconds |
| `ALTAR_CHARGING` | 100 ticks, 5 seconds |
| `SOUL_AWAKENING` | 80 ticks, 4 seconds |
| `PLAYER_BINDING` | 60 ticks, 3 seconds |
| `ASCENSION` | 60 ticks, 3 seconds |
| `COMPLETION` | 40 ticks, 2 seconds |

Movement is locked by default while still allowing the player to look around. Taking damage cancels the ritual by default. The player must begin within 4 blocks of the interaction point and remain inside the configured ritual radius.

During Ascension, the player is moved to the configured arrival point. Completion assigns the reserved soul and can also trigger the configured instability fracture check.

## Exact refunds

Before item removal, DragonAltar persists cloned copies of the exact selected items. That includes durability, enchantments, names, lore, and other item metadata.

If a ritual is canceled or the server restarts during an active ritual:

1. The soul reservation is released.
2. The persisted exact items are queued as a refund.
3. DragonAltar tries to return them on the next safe player opportunity.
4. Anything that does not fit remains pending.

Players can retry with:

```text
/dragon refunds
```

Free enough inventory space first. The command reports how many pending refund entries remain.

## Mother Soul removal ritual

The removal ritual uses a normal single chest. A double chest does not qualify. Closing a qualifying chest starts structure and participant checks.

### Chest offerings

| Offering | Amount | Notes |
|---|---:|---|
| Phantom Membrane | 128 | Exact material count |
| Potion of Weakness | 4 | Drinkable regular or long Weakness potions only |
| Nether Star | 1 | Exact material count |
| Netherite Block | 1 | Exact material count |

Splash and lingering Weakness potions do not count.

### Structure

Treat the chest block as `(0, 0, 0)`.

At one block below the chest:

- `(0, -1, 0)` is Crying Obsidian
- North, south, east, and west adjacent blocks are Obsidian
- All four diagonals are Polished Blackstone Bricks

At chest height:

- Each diagonal has a lit White Candle

At two blocks from the chest and one block down:

- North, south, east, and west are Soul Sand caller pads

One player must stand on each pad. The player who closed the chest must be one of those four and becomes the selection leader.

### Choosing and holding the ceremony

The leader receives a GUI listing the current Dragonborn. The default selection window is 60 seconds.

After a target is chosen:

- The chest is locked against normal changes
- All four callers must remain within 0.85 blocks of their exact pad position
- The ceremony runs for 160 ticks, 8 seconds, by default
- The altar, offerings, target, and callers are checked again at resolution
- Offerings are consumed only at resolution

If the target, callers, structure, or offerings change before resolution, the ceremony stops without consuming the recipe.

Every completed cast gives all four callers Weakness XVI for 12 real-time hours. The remaining duration is persisted across restart, join, and respawn.

## Backfire

Only Dragonborn callers other than the target count toward backfire:

| Dragonborn callers | Backfire chance |
|---:|---:|
| 0 | 0% |
| 1 | 25% |
| 2 or more | 50% |

On a one-caller backfire, the caller's soul enters limbo. On a successful backfire with two Dragonborn callers, the two caller souls and the target soul all enter limbo. That is the total blackout.

Backfire is checked before instability fracture and ordinary transfer.

## Instability and fracture

Every started Mother Soul ceremony increments one global persistent cast count. With the shipped threshold of 6, fracture becomes active starting with cast 7.

Once active, each ceremony that did not backfire has a 20% fracture chance by default. A fracture manifests the target soul as the hostile creature described in [Dragon Souls](Dragon-Souls).

The cast count is intentionally not exposed to ordinary player commands.

## Normal resolution

If the ritual neither backfires nor fractures, DragonAltar selects a random eligible online player other than the target and transfers the soul.

If no candidate exists, the exact pre-consumption chest contents are restored. If another plugin prevents assignment, the chest contents are also restored.

See [Configuration](Configuration) for tunables and [Troubleshooting](Troubleshooting) for stuck refunds, expired selections, and fracture recovery.
