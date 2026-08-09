# Building a DragonAltar Add-on

DragonAltar 1.4.21 publishes API contract `3.0` through Bukkit's
`ServicesManager`. Build against `com.dragonaltar:dragonaltar-api:1.4.21` and use
only `com.dragonaltar.api` packages. Do not read DragonAltar data YAML, cast to
its implementation, or import gameplay implementation packages.

## License rules

DragonAltar is owned by Silverfox0338.

An independent add-on and every DragonAltar-related feature it provides must remain free. It may not:

- Sell or rent access
- Require a purchase or subscription
- Use a payment-linked license key
- Put commands, abilities, content, or access behind a paywall
- Offer paid, premium, or paid early-access DragonAltar features
- Bundle or redistribute the main DragonAltar plugin
- Imply sponsorship, endorsement, or maintenance by Silverfox0338

A voluntary donation is allowed only when it provides no DragonAltar feature, access, priority, advantage, content, or other benefit.

The public project page or README and the distributed JAR must visibly contain:

> DragonAltar is owned by Silverfox0338. This add-on is independently developed and is not the official DragonAltar plugin.

Put a copy in `src/main/resources/DRAGONALTAR-NOTICE.txt` so it is included in the JAR.

Read the repository `LICENSE.md` before distribution. Commercial use or another exception requires prior written permission from Silverfox0338.

## Maven setup

Use Java 21 and mark the standalone API and Paper as `provided`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>example</groupId>
  <artifactId>ember-tools</artifactId>
  <version>1.0.0</version>

  <properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <repositories>
    <repository>
      <id>github</id>
      <url>https://maven.pkg.github.com/silverfox0338/dragonaltar</url>
    </repository>
    <repository>
      <id>papermc</id>
      <url>https://repo.papermc.io/repository/maven-public/</url>
    </repository>
  </repositories>

  <dependencies>
    <dependency>
      <groupId>com.dragonaltar</groupId>
      <artifactId>dragonaltar-api</artifactId>
      <version>1.4.21</version>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>io.papermc.paper</groupId>
      <artifactId>paper-api</artifactId>
      <version>1.21.4-R0.1-SNAPSHOT</version>
      <scope>provided</scope>
    </dependency>
  </dependencies>
</project>
```

Do not shade DragonAltar into the add-on.

GitHub's Maven registry requires authentication. In `~/.m2/settings.xml`, add a
server with id `github`, your GitHub username, and a classic personal access
token with `read:packages`. A private repository/package also requires the
account to have read access. In GitHub Actions, a repository granted package
access can use `GITHUB_TOKEN` with `packages: read`.

## Bukkit metadata

Create `src/main/resources/plugin.yml`:

```yaml
name: EmberTools
version: 1.0.0
main: example.embertools.EmberToolsPlugin
api-version: '1.21'
depend: [DragonAltar]
authors: [ExampleAuthor]
description: A free independent add-on for DragonAltar
```

Use `depend`, not `softdepend`, when the add-on cannot run without the API.

## Complete registration example

```java
package example.embertools;

import com.dragonaltar.api.DragonAltarApi;
import com.dragonaltar.api.addon.DragonAddonAbility;
import com.dragonaltar.api.addon.DragonAltarAddon;
import com.dragonaltar.api.model.DragonActionResult;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

public final class EmberToolsPlugin extends JavaPlugin {
    private DragonAltarApi dragonAltar;

    @Override
    public void onEnable() {
        dragonAltar = Bukkit.getServicesManager().load(DragonAltarApi.class);
        if (dragonAltar == null) {
            throw new IllegalStateException("DragonAltar API is unavailable");
        }
        if (!dragonAltar.apiVersion().startsWith("3.")) {
            throw new IllegalStateException(
                    "EmberTools needs DragonAltar API 3.x, found " + dragonAltar.apiVersion());
        }

        dragonAltar.registerAddon(this, new DragonAltarAddon(
                "ember-tools",
                "Ember Tools",
                getPluginMeta().getVersion(),
                "ExampleAuthor",
                "A free movement ability for Rev"
        ));
        dragonAltar.registerAbility(this, new EmberStep());
    }

    private static final class EmberStep implements DragonAddonAbility {
        @Override
        public String id() {
            return "ember-tools:ember-step";
        }

        @Override
        public String displayName() {
            return "Ember Step";
        }

        @Override
        public Category category() {
            return Category.MOVEMENT;
        }

        @Override
        public int energyCost() {
            return 20;
        }

        @Override
        public long cooldownMillis() {
            return 8_000L;
        }

        @Override
        public Set<String> supportedSouls() {
            return Set.of("Rev");
        }

        @Override
        public DragonActionResult canUse(Context context) {
            return context.player().isOnGround()
                    ? DragonActionResult.ok()
                    : DragonActionResult.failure("You must be on the ground");
        }

        @Override
        public DragonActionResult activate(Context context) {
            Player player = context.player();
            player.setVelocity(player.getLocation().getDirection().multiply(1.1).setY(.35));
            player.getWorld().spawnParticle(
                    Particle.FLAME, player.getLocation(), 12, .2, .1, .2, .02);
            return DragonActionResult.ok();
        }
    }
}
```

DragonAltar automatically removes the registration, abilities, and item definitions when Bukkit disables the owner plugin. Call `unregisterAddon(this)` manually only when features are being disabled while the Bukkit plugin remains enabled.

## Registration validation

Add-on ids:

- Must be 2 to 32 characters
- Must already be lowercase
- May use lowercase letters, digits, dots, hyphens, and underscores
- Must start with a lowercase letter or digit
- Cannot be `dragonaltar`
- Must be unique

The owner plugin must be non-null and enabled. One Bukkit plugin can register one add-on identity.

Custom ability ids use `<addon-id>:<local-id>`. The local part:

- Must be 2 to 48 characters
- Uses the same lowercase character set
- Must be unique in the whole live ability registry

The display name must not be blank. Category must not be null. Energy cost is 0 through the configured maximum, cooldown is 0 through 24 hours, and at least one valid soul is required. Soul values may be Akuma, Rev, Lamari, or their canonical ids.

## Soul-bound equipment

Implement `DragonAddonItem`, register it after the add-on metadata, and tag each
stack created by the add-on:

```java
dragonAltar.registerItem(this, new DragonAddonItem() {
    public String id() { return "ember-tools:frost-vestment"; }
    public String displayName() { return "Frost Vestment"; }
    public String soulId() { return "Akuma"; }
    public StripPolicy onSoulLoss() { return StripPolicy.UNEQUIP; }
});

ItemStack vestment = new ItemStack(Material.DIAMOND_CHESTPLATE);
dragonAltar.tagSoulBound(vestment, "ember-tools:frost-vestment");
```

Override `canEquip(Context)` for extra add-on rules, and listen to the
cancellable `DragonAddonItemEquipEvent` for cross-plugin vetoes. DragonAltar
checks soul ownership for helmet, chest, legs, feet, main hand, and off hand.
Tags remain recognizable while the add-on is disabled, but enforcement resumes
only after its item id is registered again.

`onSoulLoss()` may return `NONE`, `UNEQUIP`, `DROP`, or `DESTROY`. NONE preserves
manual handling. UNEQUIP uses a non-equipped inventory slot and falls back to a
drop if inventory is full. Policies also reconcile offline loss at login and
add-on re-registration. Use the `soulId()` carried by
`DragonSoulTransferEvent` and `DragonbornLoseEvent` for custom NONE behavior.

## Cast lifecycle

A custom ability runs inside the ordinary DragonAltar cast pipeline:

1. Player permission, holder, online, alive, and game-mode checks
2. Cancellable `DragonAbilityCastEvent`
3. Ability and shared-group cooldown checks
4. Energy or full-bar ultimate check
5. `canUse(Context)`
6. `activate(Context)`
7. Energy deduction, cooldown start, persistence, and regeneration delay only after success

Return `DragonActionResult.failure(message)` for an expected denial. A null result becomes a failed cast. Runtime exceptions are caught, logged server-side, and returned as a generic add-on failure.

If `ultimate()` returns true, DragonAltar requires the full energy bar and applies its persistent shared ultimate cooldown.

Do not block the server thread in either callback. Schedule slow storage or network work asynchronously, then return to the server thread before touching Bukkit players, worlds, entities, inventories, or the DragonAltar API.

## Reading public state

```java
dragonAltar.soulInfo("Rev").ifPresent(soul -> {
    getLogger().info(soul.name() + " is " + soul.status());
});

for (String id : dragonAltar.availableAbilityIds(player)) {
    getLogger().info(id);
}
```

Snapshot records and returned collections are immutable. A public soul can be `HELD`, `DORMANT`, `LIMBO`, or `FRACTURED`.

A soul omitted from public holder presentation appears `DORMANT` with an empty holder. Do not infer, reconstruct, log, or publish a hidden holder.

## Events

Register a normal Bukkit listener:

```java
package example.embertools;

import com.dragonaltar.api.DragonAltarApi;
import com.dragonaltar.api.event.DragonAbilityCastEvent;
import com.dragonaltar.api.event.DragonSoulTransferCompleteEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class DragonListener implements Listener {
    private final DragonAltarApi api;

    public DragonListener(DragonAltarApi api) {
        this.api = api;
    }

    @EventHandler
    public void onAbilityCast(DragonAbilityCastEvent event) {
        if (event.abilityId().equals("ember-tools:ember-step")
                && !event.player().isOnGround()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTransfer(DragonSoulTransferCompleteEvent event) {
        api.soulInfo(event.soulId()).ifPresent(soul -> {
            // Use only this privacy-safe snapshot for player-visible output.
        });
    }
}
```

Events are synchronous. Prepare, ritual start, soul reserve, transfer start, ability select, ability cast, and energy change are cancellable before their durable mutation.

Use the API snapshot for any player-visible message. Event payloads are integration signals, not permission to publish private operational context.

## Threading

DragonAltar enforces the primary server thread for:

- `registerAddon`
- `registerAbility`
- `registerItem`
- `tagSoulBound`
- `unregisterAddon`
- `cast`
- `openSoulHistory`
- `selectAbility`

Other calls read Bukkit players or live plugin state and should also be made on the server thread. Immutable values can be copied and processed elsewhere after the call returns.

## Lifecycle checklist

- Declare `depend: [DragonAltar]`
- Resolve the service during `onEnable`
- Check `apiVersion()` when a specific contract is required
- Register metadata before abilities
- Keep callbacks fast
- Return failed action results for normal denials
- Never edit DragonAltar data files
- Never expose a hidden holder or operational diagnostic output
- Let DragonAltar unregister on plugin disable
- Include the required ownership notice in both the project page and JAR
- Keep the add-on and all DragonAltar features free

See [API Reference](API-Reference) for every public type and method.
