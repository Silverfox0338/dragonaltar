# Public API

DragonAltar publishes API version `2.0` through Bukkit's `ServicesManager`. Add-ons
should depend on the service interface and the `com.dragonaltar.api` packages,
not the main plugin class or its data files.

```java
DragonAltarApi api = Bukkit.getServicesManager().load(DragonAltarApi.class);
if (api == null) {
    throw new IllegalStateException("DragonAltar API is unavailable");
}
```

Use `depend: [DragonAltar]` in the add-on's `plugin.yml` so the service exists
before the add-on enables.

## Read-only state

The API provides immutable records and immutable collection results:

- `apiVersion()` returns the public contract version.
- `event()` returns the Ancient Dragon and altar states plus optional session and
  canonical dragon UUIDs.
- `activeRitual()` returns the ritual player, public soul identity, phase, and
  session. Consumed inventory is private.
- `souls()`, `soulInfo(idOrName)`, and `soulInfoOf(uuid)` return safe soul
  snapshots.
- `dragonborn()` returns public holder UUIDs.
- `eligibilityInfo(player)` returns the normal ritual eligibility decision and
  its individual checks.
- `abilityIds()` and `ability(id)` inspect the full registry.
- `availableAbilityIds(player)`, `selectedAbility(player)`, and
  `cooldownSeconds(player, id)` inspect a player's usable abilities.
- `energy(player)` and `maximumEnergy()` inspect Dragon Energy.

Soul snapshots accept both canonical ids (`soul-1`, `soul-2`, `soul-3`) and the
public names Akuma, Rev, and Lamari. Administrative custody is deliberately
redacted: a soul privately held by staff appears dormant with no holder. The API
does not expose lineage, admin actors, hidden transfer counts, or ritual items.
The old raw `soul(...)` methods remain only for binary compatibility and always
return an empty result.

## Safe player actions

- `selectAbility(player, id)` uses the normal ownership and selection checks.
- `cast(player)` uses the normal permission, ownership, cooldown, energy,
  cancellable-event, and ability checks.
- `openSoulHistory(player)` opens the player-safe history interface when the
  player is online and has `dragonaltar.use`.

These calls are not administrative shortcuts and do not bypass DragonAltar's
safeguards.

## Registering an add-on

Register once from the add-on's `onEnable` method:

```java
DragonAltarAddon details = new DragonAltarAddon(
        "ember-tools",
        "Ember Tools",
        getPluginMeta().getVersion(),
        "ExampleAuthor",
        "Small extras for Rev"
);
api.registerAddon(this, details);
```

Add-on ids are lowercase namespaces between 2 and 32 characters. Registering and
unregistering must happen on the server thread. DragonAltar automatically removes
an add-on's abilities when Bukkit disables its owner plugin.

`api.addons()` returns the currently registered add-on metadata.

## Adding an ability

Implement `DragonAddonAbility`, then register it after the add-on metadata:

```java
api.registerAbility(this, new DragonAddonAbility() {
    public String id() { return "ember-tools:ember-step"; }
    public String displayName() { return "Ember Step"; }
    public Category category() { return Category.MOVEMENT; }
    public int energyCost() { return 20; }
    public long cooldownMillis() { return 8_000L; }
    public Set<String> supportedSouls() { return Set.of("Rev"); }

    public DragonActionResult activate(Context context) {
        Player player = context.player();
        player.setVelocity(player.getLocation().getDirection().multiply(1.1));
        return DragonActionResult.ok();
    }
});
```

Ability ids must use the registered add-on namespace. Energy cost is bounded by
the configured maximum, cooldown is bounded to 24 hours, and at least one valid
soul is required. Activation runs inside DragonAltar's normal cast pipeline.
DragonAltar deducts energy and starts the cooldown only after a successful
result. Exceptions are contained, logged, and reported as a failed cast.

An add-on ability can override `canUse(Context)` and `ultimate()`. Ultimate
abilities use DragonAltar's full-energy and shared ultimate-cooldown rules.

## Bukkit events

Events live under `com.dragonaltar.api.event`. Event prepare, ritual start, soul
reserve and transfer start, ability selection and cast, and energy change are
cancellable. Cancellation occurs before the related durable mutation.

Published event families include:

- Ancient Dragon prepare, start, spawn, and death
- altar awaken and dormant
- ritual start and complete
- soul create, reserve, transfer start, and transfer complete
- Dragonborn gain and loss
- ability select and cast
- Dragon Energy change

Events are server-side integration signals. An add-on must not publish, message,
log for players, or otherwise reveal administrative participants or private
custody. Use the snapshot API for anything players can see.

## Compatibility

The plugin release and API contract have separate versions. Check
`api.apiVersion()` when an add-on requires a new contract. New methods and records
may be added in a compatible `2.x` API release; removals or incompatible record
changes require a new major API version.

See [ADDON-DEVELOPMENT.md](ADDON-DEVELOPMENT.md) for a complete Maven project and
the required add-on attribution.
