# Player Guide

This page is the practical version: how to claim a soul, use the Dragon Focus, read the HUD, and recover when something goes wrong.

## Claiming an unclaimed soul

You must be eligible, must not already be Dragonborn, and must bring the full recipe.

The default offerings are:

- 1 Elytra
- 1 Nether Star
- 16 Dragon's Breath
- 8 End Crystals
- 4 Echo Shards

Right-click the server's configured altar interaction block. Review the recipe window, then confirm the ritual. Stay in the ritual area and avoid damage until it finishes.

The altar is available only after the Ancient Dragon is defeated and while at least one soul remains unclaimed.

## Basic commands

| Command | What it does |
|---|---|
| `/dragon` | Shows event, setup, and your Dragonborn status |
| `/dragon status` | Same status summary |
| `/dragon help` | Opens the help menu |
| `/dragon abilities` | Lists abilities currently available to you |
| `/dragon focus` | Restores a missing Focus if you are Dragonborn and have space |
| `/dragon settings` | Opens accessibility and control settings |
| `/dragon history` | Opens the public Dragon Soul archive |
| `/dragon history <player>` | Prints that player's public Dragonborn history in chat |
| `/dragon refunds` | Retries pending exact-item ritual refunds |

Every command, including staff branches, first requires `dragonaltar.use`. It is granted to everyone by default.

## The Dragon Focus

The Focus is an Echo Shard by default. It is tagged to its owner and soul, has a glint, and includes soulbound lore.

DragonAltar protects it from:

- Dropping
- Pickup by another entity
- Item frames and armor stands
- Shift-moving or dragging into external inventories
- Hoppers and other inventory movers
- Ender Chest storage
- Cloning or collect-to-cursor actions
- Configured sell, auction, market, and bulk-sell commands

If a Focus escapes as an item entity, DragonAltar removes it and restores the owner's copy when possible. It also removes duplicates.

Free one inventory slot before `/dragon focus`. The plugin never drops a replacement on the ground.

## Focus controls

| Input | Result |
|---|---|
| Scroll while holding Focus, `LOCKED` mode | Cycle abilities and keep the Focus slot selected |
| Sneak and scroll while holding Focus, `SNEAK_SCROLL` mode | Cycle abilities |
| Scroll without sneaking, `SNEAK_SCROLL` mode | Normal hotbar scrolling |
| Swap hands while holding Focus | Cycle ability category |
| Sneak and swap hands while holding Focus | Open the ability menu |
| Right-click with your own Focus | Cast the selected ability |

The Focus cannot be used by another player.

## HUD

The default action-bar HUD shows:

- Dragon Energy and maximum
- Selected ability
- Selected ability cooldown
- Shared ultimate cooldown
- Current resonance and shared resonance cooldown
- Short status text such as Rev Hunt state

Rev also has a Heat boss bar with Stalking, Pursuing, and Predator tiers. Turning the HUD off hides that bar too.

## Settings

Run `/dragon settings` for the full GUI:

- HUD
- Slow Falling
- Passive particles
- Animation particles
- Sounds
- Titles
- Screen effects
- Selector mode
- Overall effect level
- Focus recovery

Direct command shortcuts:

```text
/dragon settings effects <full|reduced|minimal>
/dragon settings hud <on|off>
/dragon settings selector <locked|sneak-scroll>
/dragon settings slowfall <on|off>
```

Defaults are Full effects, HUD on, Locked selection, Slow Falling on, and all individual presentation options on.

Reduced effects use roughly half the configured animation particles. Minimal uses roughly 15% and suppresses passive particles. Gameplay calculations remain unchanged.

## Ability menu

`/dragon abilities` is a text list. To open the interactive ability menu, sneak and press swap hands while holding the Focus.

The menu shows:

- Ability name and category
- Energy cost
- Base cooldown
- Current remaining cooldown
- A Slow Falling toggle

Selecting an unavailable soul ability does nothing. Nearby resonances appear only while their holders meet the unlock rules.

## Soul history

`/dragon history` opens:

1. An overview of Akuma, Rev, and Lamari
2. Public current status and holder
3. Prior public holders
4. Latest public event
5. Limbo return information
6. A paginated timeline for each soul

Public history deliberately uses neutral maintenance descriptions and omits private provenance. A dormant soul with no public holder may cover more than one internal recovery condition.

## If you die

- An eligible non-Dragonborn PvP killer inherits your soul
- If your killer already has a soul, the server's configured collision policy applies
- Other deaths normally start a random reincarnation countdown
- Logging out during a combat tag can transfer the soul to the eligible opponent
- If no eligible player exists, the soul waits safely in pending state

## If your ritual items did not return

Run:

```text
/dragon refunds
```

Make room for every item. DragonAltar retains overflow as pending entries and tries again later.

## If your Focus is missing

1. Free an inventory slot.
2. Run `/dragon focus`.
3. If it still fails, ask staff to run `/dragon admin repair <player>`.

## Where to learn each kit

- [Abilities](Abilities)
- [Resonances](Resonances)
- [Dragon Souls](Dragon-Souls)
- [Rituals](Rituals)
- [FAQ](FAQ)
