# Public API

DragonAltar publishes API contract version `3.0` through Bukkit's `ServicesManager`. Add-ons
should depend on the service interface and the `com.dragonaltar.api` packages,
not the main plugin class or its data files.

## Maven dependency

Released API artifacts use the same version as the DragonAltar plugin release
that provides them. For DragonAltar 1.4.23, the coordinates are
`com.dragonaltar:dragonaltar-api:1.4.23`.

Add the GitHub Packages repository and the API as a provided dependency:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/silverfox0338/dragonaltar</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.dragonaltar</groupId>
    <artifactId>dragonaltar-api</artifactId>
    <version>1.4.23</version>
    <scope>provided</scope>
  </dependency>
</dependencies>
```

GitHub's Maven registry requires authentication for package downloads, including
public packages. Put credentials in `~/.m2/settings.xml`, never in a project
POM:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_CLASSIC_PERSONAL_ACCESS_TOKEN</password>
    </server>
  </servers>
</settings>
```

Use a classic personal access token with `read:packages`. If DragonAltar is
private, the account also needs read access to the repository/package. In GitHub
Actions, a repository granted package access can use its built-in `GITHUB_TOKEN`
with `packages: read` instead. Add-ons normally also declare Paper API as a
provided dependency because the public API uses Bukkit/Paper types.

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
API 3.0 removed the deprecated raw `soul(...)`, `soulOf(...)`, `eventState()`,
`eligibility(...)`, and `castSelectedAbility(...)` signatures because their JVM
descriptors exposed implementation types. Their safe API-owned replacements are
`soulInfo(...)`, `soulInfoOf(...)`, `event()`, `eligibilityInfo(...)`, and
`cast(...)`.

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
an add-on's abilities and item definitions when Bukkit disables its owner plugin.

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

## Adding soul-bound equipment

Register an item definition after the add-on metadata, then tag each stack made
by the add-on with its registered id:

```java
DragonAddonItem vestment = new DragonAddonItem() {
    public String id() { return "ember-tools:frost-vestment"; }
    public String displayName() { return "Frost Vestment"; }
    public String soulId() { return "Akuma"; }
    public StripPolicy onSoulLoss() { return StripPolicy.UNEQUIP; }

    public DragonActionResult canEquip(Context context) {
        return context.player().getWorld().getEnvironment() == World.Environment.NORMAL
                ? DragonActionResult.ok()
                : DragonActionResult.failure("The vestment is dormant in this realm.");
    }
};
api.registerItem(this, vestment);

ItemStack stack = new ItemStack(Material.DIAMOND_CHESTPLATE);
api.tagSoulBound(stack, vestment.id());
```

Item ids follow the same add-on namespace rules as abilities. The PDC marker is
`dragonaltar:soul_bound_item`; use `isSoulBound`, `soulBoundItemId`, and
`itemIds` instead of reading it directly. DragonAltar checks helmet, chest,
legs, boots, main-hand, and off-hand player equip attempts. The player must hold
the registered soul, `canEquip` must succeed, and listeners must not cancel
`DragonAddonItemEquipEvent`.

The item and event contexts expose cloned stacks. Expected denials should return
a failed `DragonActionResult`; callback exceptions are logged and denied. Tags
survive an add-on being disabled, but enforcement is inactive until the item id
is registered again.

`onSoulLoss()` controls common lifecycle behavior. `NONE` is the default and
leaves handling to the add-on. `UNEQUIP` moves the stack to a non-equipped
inventory slot, `DROP` drops it at the player's location, and `DESTROY` removes
it permanently. UNEQUIP safely falls back to DROP when inventory is full and
never places an item back into the selected main-hand slot. DragonAltar applies
the policy on `DragonbornLoseEvent`, reconciles offline losses when the player
joins, and reconciles live players when an item definition is registered again.
Death and keep-inventory paths are handled without duplicating vanilla drops.

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
- add-on item equip
- ability select and cast
- Dragon Energy change

Events are server-side integration signals. An add-on must not publish, message,
log for players, or otherwise reveal administrative participants or private
custody. Use the snapshot API for anything players can see.

`DragonSoulTransferEvent` and `DragonbornLoseEvent` both expose `soulId()`, so
items using `NONE` can implement particle effects, lore changes, or other custom
transfer, Limbo, fracture, and loss behavior without inferring the soul from
player context.

## Compatibility

The Maven artifact version tracks the plugin release version: API artifact
`1.4.23` is supplied by plugin `1.4.23`. The runtime contract version returned by
`api.apiVersion()` is separate and is `3.0` for this release. New methods and
records may be added in a compatible `3.x` contract; removals, changed method
descriptors, or incompatible record changes require a new contract major. Add-ons
should compile against the oldest plugin-release artifact they support and may
check `apiVersion()` before using a newer contract feature.

Only types under `com.dragonaltar.api` are supported. Packages such as
`com.dragonaltar.soul`, `ability`, `eligibility`, `dragonevent`, `persistence`,
and `ritual` are implementation details and may change without notice.

See [ADDON-DEVELOPMENT.md](ADDON-DEVELOPMENT.md) for a complete Maven project and
the required add-on attribution.
