# Public API

Retrieve `DragonAltarApi` from Bukkit's `ServicesManager`:

```java
DragonAltarApi api = Bukkit.getServicesManager().load(DragonAltarApi.class);
```

The service exposes event/altar state, immutable Dragonborn and soul views, centralized eligibility results, immutable ability metadata, Dragon Energy, and safe ability select/cast actions. Selection and casting still run the normal permission, ownership, cooldown, energy, and cancellable-event checks.

The main methods are `eventState()`, `altarState()`, `dragonborn()`, `soul(id)`, `soulOf(uuid)`, `eligibility(player)`, `abilityIds()`, `ability(id)`, `energy(player)`, `maximumEnergy()`, `selectAbility(player, id)`, and `castSelectedAbility(player)`. Returned collections and lineage lists are immutable, and soul mutation methods are package-private.

Canonical storage/API IDs remain stable: `soul-1` is Akuma, `soul-2` is Rev, and `soul-3` is Lamari. `abilityIds()` returns the full registry; selection rejects abilities that do not belong to the player's held soul.

PlaceholderAPI's `%dragonaltar_soul_id%` and `%dragonaltar_soul_name%` both return the public name. Administrative integrations that genuinely need the numbered persistence key can use `%dragonaltar_soul_internal_id%`.

Events live under `com.dragonaltar.api.event`. Event prepare, ritual start, soul reserve/transfer start, ability selection/cast, and energy change are cancellable. Cancellation occurs before the related durable ownership or energy mutation. Spawn, death, altar, ritual-complete, soul-create/transfer-complete, and Dragonborn gain/loss events are observational.

Published event classes are:

- `AncientDragonEventPrepareEvent`, `AncientDragonEventStartEvent`, `AncientDragonSpawnEvent`, and `AncientDragonDeathEvent`
- `DragonAltarAwakenEvent` and `DragonAltarDormantEvent`
- `DragonRitualStartEvent` and `DragonRitualCompleteEvent`
- `DragonSoulCreateEvent`, `DragonSoulReserveEvent`, `DragonSoulTransferStartEvent`, and `DragonSoulTransferCompleteEvent`
- `DragonbornGainEvent` and `DragonbornLoseEvent`
- `DragonAbilitySelectEvent`, `DragonAbilityCastEvent`, and `DragonEnergyChangeEvent`

Every event owns an independent Bukkit `HandlerList`.
