# API Reference

DragonAltar 1.4.19 publishes API contract `2.0`. Load it through Bukkit:

```java
DragonAltarApi api = Bukkit.getServicesManager().load(DragonAltarApi.class);
if (api == null) {
    throw new IllegalStateException("DragonAltar API is unavailable");
}
```

All public API types are under `com.dragonaltar.api`, including its `addon`, `event`, and `model` subpackages. New add-ons should not import the implementation types exposed only by deprecated compatibility methods.

Parameters documented as `Player`, `Plugin`, `UUID`, ability id, or metadata are non-null unless the method explicitly says otherwise.

## `DragonAltarApi`

### Contract and snapshots

| Method | Parameters | Return | Notes |
|---|---|---|---|
| `String apiVersion()` | None | `2.0` | API contract version, independent of plugin release |
| `DragonEventInfo event()` | None | Immutable event snapshot | State, altar state, optional session UUID, optional canonical dragon UUID |
| `Optional<DragonRitualInfo> activeRitual()` | None | Active initial ritual | Empty when inactive; consumed item data is never included |
| `Collection<DragonSoulInfo> souls()` | None | Immutable list of all public soul snapshots | Exactly the currently created canonical souls |
| `Optional<DragonSoulInfo> soulInfo(String idOrName)` | Canonical id or Akuma, Rev, Lamari, case-insensitive names | One public soul snapshot | Null or blank returns empty |
| `Optional<DragonSoulInfo> soulInfoOf(UUID player)` | Player UUID | Public held soul | Empty for no public held soul |
| `DragonEligibilityInfo eligibilityInfo(Player player)` | Live Bukkit player | Immutable decision and all checks | Uses ordinary shared eligibility rules |

### Player ability views and actions

| Method | Parameters | Return | Notes |
|---|---|---|---|
| `Collection<String> availableAbilityIds(Player player)` | Player | Immutable available ids | Soul kit plus the currently unlocked resonance |
| `Optional<String> selectedAbility(Player player)` | Player | Selected id | Empty when the player has no public Dragonborn state |
| `long cooldownSeconds(Player player, String abilityId)` | Player, id | Remaining whole seconds | Includes shared ultimate or resonance group where relevant; rounds up |
| `DragonActionResult cast(Player player)` | Player | Success and message | Main thread; normal permission, state, event, energy, cooldown, and ability checks |
| `boolean openSoulHistory(Player player)` | Player | Whether GUI opened | Main thread; false when offline or missing `dragonaltar.use` |
| `Collection<String> abilityIds()` | None | Every registered ability id | Includes built-ins, resonances, and live add-on abilities |
| `Optional<DragonAbilityInfo> ability(String id)` | Ability id, case-insensitive | Public metadata | Empty when unknown; null also returns empty |
| `int energy(Player player)` | Player | Current energy | Returns 0 when no public Dragonborn state |
| `int maximumEnergy()` | None | Configured maximum | Must be 100 in a valid 1.4.19 configuration |
| `boolean selectAbility(Player player, String abilityId)` | Player, id | Whether final selection matches | Main thread; needs base permission, holder state, registered and available id; selection event may cancel |

`cast` returns the internal localization key for some built-in failures and a plain message for add-on failures. Treat the string as diagnostic unless your add-on owns the returned message.

### Add-on registry

| Method | Parameters | Return | Notes |
|---|---|---|---|
| `void registerAddon(Plugin owner, DragonAltarAddon addon)` | Enabled owner plugin, metadata | None | Main thread; validates id and uniqueness |
| `void registerAbility(Plugin owner, DragonAddonAbility ability)` | Registered owner, ability | None | Main thread; validates namespace, metadata, limits, souls, and registry uniqueness |
| `boolean unregisterAddon(Plugin owner)` | Owner plugin | True if removed | Main thread; null returns false; removes all owned abilities |
| `Collection<DragonAltarAddon> addons()` | None | Immutable live registration metadata | Registration order |

### Other supported views

| Method | Return | Notes |
|---|---|---|
| `String altarState()` | Altar enum name as text | Prefer `event().altarState()` when already reading the event |
| `Collection<UUID> dragonborn()` | Public holder UUIDs | Omits holders not suitable for public presentation |

### Deprecated compatibility methods

| Method | Current behavior | Replacement |
|---|---|---|
| `DragonEventState eventState()` | Returns an implementation enum | `event().state()` |
| `Optional<DragonSoul> soul(String id)` | Always empty | `soulInfo(String)` |
| `Optional<DragonSoul> soulOf(UUID player)` | Always empty | `soulInfoOf(UUID)` |
| `EligibilityService.Result eligibility(Player player)` | Returns an implementation type | `eligibilityInfo(Player)` |
| `AbilityResult castSelectedAbility(Player player)` | Runs the old internal result path | `cast(Player)` |

The implementation packages named in these signatures are not supported add-on APIs. They remain only for binary compatibility.

## Add-on types

### `DragonAltarAddon`

Record components and generated accessors:

```java
String id()
String name()
String version()
String author()
String description()
```

Its public constructor has the same five parameters. `id`, `name`, `version`, and `author` are trimmed and must be nonblank. A null description becomes an empty string and is trimmed.

The stricter lowercase id pattern, reserved namespace, uniqueness, owner, and live-plugin checks happen in `registerAddon`.

### `DragonAddonAbility`

Required methods:

| Method | Return | Validation |
|---|---|---|
| `id()` | Namespaced id | Registered add-on namespace plus a 2 to 48 character lowercase local id |
| `displayName()` | Plain display name | Nonblank |
| `category()` | `Category` | Non-null |
| `energyCost()` | Integer | 0 through maximum Dragon Energy |
| `cooldownMillis()` | Long | 0 through 24 hours |
| `supportedSouls()` | Set of public names or canonical ids | Non-null, non-empty, every value recognized |
| `activate(Context context)` | `DragonActionResult` | Non-null result expected |

Default methods:

| Method | Default |
|---|---|
| `boolean ultimate()` | `false` |
| `DragonActionResult canUse(Context context)` | `DragonActionResult.ok()` |

Nested public enum `DragonAddonAbility.Category` values:

```text
MOVEMENT
OFFENSE
SENSES
DEFENSE
```

Nested public record `DragonAddonAbility.Context`:

| Component | Type | Meaning |
|---|---|---|
| `player()` | `Player` | Casting player |
| `api()` | `DragonAltarApi` | Same public service |

The record has its generated constructor `Context(Player, DragonAltarApi)`, accessors, equality, hash code, and string representation.

## Model types

All records have their generated public canonical constructor, component accessors, equality, hash code, and string representation.

### `DragonAbilityInfo`

```java
String id()
String displayName()
String category()
int energyCost()
long cooldownMillis()
```

Built-in display names are converted to plain text. Category is an enum name string.

### `DragonActionResult`

```java
boolean success()
String message()
```

The constructor converts a null message to `""`.

Static factories:

| Method | Result |
|---|---|
| `DragonActionResult.ok()` | Successful result with empty message |
| `DragonActionResult.failure(String message)` | Failed result; null message normalizes to empty |

### `DragonEligibilityInfo`

```java
boolean eligible()
Map<String, Boolean> checks()
```

The constructor copies the map. Check names are:

```text
online
game-mode
minimum-playtime
required-permission
not-excluded
not-dragonborn
not-afk
not-vanished
alive
join-grace
```

### `DragonEventInfo`

Components:

```java
String state()
String altarState()
UUID sessionId()
UUID dragonId()
```

Convenience methods:

| Method | Return |
|---|---|
| `Optional<UUID> session()` | Optional wrapper for `sessionId` |
| `Optional<UUID> dragon()` | Optional wrapper for `dragonId` |

Event state names are listed in [Ancient Dragon Event](Ancient-Dragon-Event). Altar state names are `UNCONFIGURED`, `CONFIGURED`, `AWAKENING`, `ACTIVE`, `DORMANT`, and `DISABLED`.

### `DragonRitualInfo`

```java
UUID playerId()
String soulId()
String soulName()
String phase()
UUID sessionId()
```

Phase is one of `OFFERINGS_ACCEPTED`, `ALTAR_CHARGING`, `SOUL_AWAKENING`, `PLAYER_BINDING`, `ASCENSION`, or `COMPLETION`.

### `DragonSoulInfo`

Components:

```java
String id()
String name()
String status()
UUID holder()
Instant createdAt()
Instant limboReturnAt()
```

Convenience methods:

| Method | Return |
|---|---|
| `Optional<UUID> holderId()` | Optional wrapper for `holder` |
| `Optional<Instant> limboReturnTime()` | Optional wrapper for `limboReturnAt` |

Public status is `HELD`, `DORMANT`, `LIMBO`, or `FRACTURED`. `limboReturnAt` is populated only for Limbo. A soul hidden from public holder presentation is reported as `DORMANT` with null holder. Do not attempt to infer more detail.

## Event base types

### `DragonAltarEvent`

Abstract synchronous Bukkit `Event`. Its protected constructor calls `Event(false)`.

### `DragonAltarCancellableEvent`

Abstract subclass of `DragonAltarEvent` implementing Bukkit `Cancellable`.

```java
boolean isCancelled()
void setCancelled(boolean cancelled)
```

Every concrete event implements `getHandlers()` and a static `getHandlerList()` using its own `HandlerList`. These are standard Bukkit event methods and are not repeated in each table below.

## Concrete events

| Event | Cancellable | Public constructor and payload accessors | Fired when |
|---|:---:|---|---|
| `AncientDragonEventPrepareEvent` | Yes | `(Player initiator, UUID sessionId)`, `initiator()`, `sessionId()` | Official preflight passed, before durable start |
| `AncientDragonEventStartEvent` | No | `(UUID sessionId)`, `sessionId()` | Official vanilla respawn is starting |
| `AncientDragonSpawnEvent` | No | `(EnderDragon dragon)`, `dragon()` | Canonical official dragon is accepted |
| `AncientDragonDeathEvent` | No | `(EnderDragon dragon, String method)`, `dragon()`, `method()` | Canonical death begins; method is normally `COMBAT` or `SED_KILL` |
| `DragonAltarAwakenEvent` | No | No-argument constructor, no payload | Altar state changes to Active |
| `DragonAltarDormantEvent` | No | No-argument constructor, no payload | Altar state changes to Dormant |
| `DragonRitualStartEvent` | Yes | `(Player player, String soulId)`, `player()`, `soulId()` | Initial ritual is ready to start |
| `DragonRitualCompleteEvent` | No | `(Player player, String soulId)`, `player()`, `soulId()` | Initial ritual completes |
| `DragonSoulCreateEvent` | No | `(String soulId)`, `soulId()` | A canonical soul record is created |
| `DragonSoulReserveEvent` | Yes | `(String soulId, UUID player)`, `soulId()`, `player()` | A soul is about to be reserved |
| `DragonSoulTransferStartEvent` | Yes | `(String soulId, UUID from, UUID to)`, `soulId()`, `from()`, `to()` | An existing holder is about to change |
| `DragonSoulTransferCompleteEvent` | No | `(String soulId, UUID from, UUID to)`, `soulId()`, `from()`, `to()` | Existing holder change completed |
| `DragonSoulTransferEvent` | Yes | Same transfer constructor and accessors | Compatibility pre-transfer event; cancellation prevents the transfer |
| `DragonbornGainEvent` | No | `(UUID player, String soulId)`, `player()`, `soulId()` | A player gains a soul |
| `DragonbornLoseEvent` | No | `(UUID player, String soulId)`, `player()`, `soulId()` | A player loses a soul |
| `DragonAbilitySelectEvent` | Yes | `(Player player, String abilityId)`, `player()`, `abilityId()` | Available ability selection is about to persist |
| `DragonAbilityCastEvent` | Yes | `(Player player, String abilityId)`, `player()`, `abilityId()` | Cast begins before cooldown and energy checks |
| `DragonEnergyChangeEvent` | Yes | `(Player player, int oldEnergy, int newEnergy)`, `player()`, `oldEnergy()`, `newEnergy()`, `newEnergy(int)` | Energy is about to change |

`DragonEnergyChangeEvent.newEnergy(int)` lets a listener replace the proposed value. DragonAltar clamps the final value back into 0 through maximum after event handlers return.

`DragonSoulTransferStartEvent` is dispatched first. If it is not cancelled, the compatibility `DragonSoulTransferEvent` is dispatched before the durable assignment. Cancelling either event prevents the transfer. `DragonSoulTransferCompleteEvent` is dispatched after a successful assignment.

Do not construct and call DragonAltar events to force gameplay. They are notifications and cancellation points around DragonAltar-owned state changes.

## `DragonAltarApiImpl`

`com.dragonaltar.api.DragonAltarApiImpl` is a public final service implementation and Bukkit listener. It implements every `DragonAltarApi` method above.

Additional public members:

| Member | Purpose |
|---|---|
| `DragonAltarApiImpl(DragonAltarPlugin plugin)` | Main-plugin bootstrap constructor |
| `void onPluginDisable(PluginDisableEvent event)` | Bukkit listener that unregisters a disabled owner |

Add-ons must not construct, cast to, subclass, or call implementation-only lifecycle members. Load `DragonAltarApi` from `ServicesManager`.

## Threading and immutability

Registration and player action methods that enforce threading throw `IllegalStateException` off the primary thread. Live snapshot methods also touch Bukkit and should be called on the primary thread.

Returned record values and collections are immutable snapshots or immutable copies. They do not update in place. Request a new snapshot when current state matters.

See [Add-on Development](Add-on-Development) for a working Maven project and the distribution rules.
