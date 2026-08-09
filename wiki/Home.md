# DragonAltar 1.4.22

DragonAltar is a Paper plugin built around one server-wide Ancient Dragon story. Staff prepare an altar and the End fountain, players defeat the official respawned dragon, and three persistent Dragon Souls enter the world: Akuma, Rev, and Lamari.

Those souls do not disappear when a holder dies or leaves. They move through claims, PvP inheritance, reincarnation, the Mother Soul ritual, limbo, fracture, and restart recovery. Each holder gets a shared Dragonborn foundation plus a soul-specific ability kit.

## Requirements

- Paper or Purpur 1.21-1.21.11
- Java 21
- DragonAltar 1.4.22
- A configured End fountain and altar

PlaceholderAPI and ScaledEnderDragon are optional. Their exact 1.4.22 behavior is covered in [Integrations](Integrations).

Version 1.4.19 removes retired settings and unused integration declarations,
connects setup and recovery messages to live server events, and makes the
tested recovery rules part of the real event recovery path.

## What is included

- One protected vanilla Ender Dragon respawn sequence
- Exactly three named Dragon Souls
- Persistent holders, cooldowns, settings, rituals, and recovery state
- Akuma, Rev, and Lamari passives and abilities
- Shared Wings and Roar abilities
- Three full-energy ultimates
- Three pair resonances and Dragon Trinity
- An item-bound Dragon Focus and configurable HUD
- Exact initial-ritual refunds after cancellation or restart
- A player-built Mother Soul removal ritual
- Persistent limbo and Fractured Soul recovery
- Per-player particle, sound, title, screen effect, HUD, and Slow Falling controls
- A public Bukkit service and event API for free independent add-ons

DragonAltar does not place a physical altar structure for you. Its altar egg is a protected `BlockDisplay`, not a dragon egg block or obtainable item.

## Start here

### Players

- [Player Guide](Player-Guide)
- [Dragon Souls](Dragon-Souls)
- [Rituals](Rituals)
- [Abilities](Abilities)
- [Resonances](Resonances)
- [FAQ](FAQ)

### Server staff

- [Installation](Installation)
- [Altar Setup](Altar-Setup)
- [Ancient Dragon Event](Ancient-Dragon-Event)
- [Administrator Guide](Administrator-Guide)
- [Commands](Commands)
- [Permissions](Permissions)
- [Configuration](Configuration)
- [Troubleshooting](Troubleshooting)

### Add-on developers

- [Add-on Development](Add-on-Development)
- [API Reference](API-Reference)
- [Data and Privacy](Data-and-Privacy)

## A note about public soul information

Player commands, history screens, placeholders, and public API snapshots use a privacy-safe view. A soul that is not suitable for public holder display appears dormant with no holder. Public interfaces do not reveal private maintenance provenance or identify the people involved.

## License

DragonAltar is source-available under the PolyForm Noncommercial License 1.0.0 and the additional terms in the repository license.

DragonAltar is owned by Silverfox0338.

Independent add-ons must remain completely free. Paid, premium, subscription, paywalled, or purchase-gated DragonAltar features are prohibited. See [Add-on Development](Add-on-Development) before publishing one.
