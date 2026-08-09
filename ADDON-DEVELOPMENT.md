# Building a DragonAltar add-on

This is the smallest complete add-on project. It reads the public soul state and
registers one ability without reaching into DragonAltar internals.

## 1. Add the API dependency

Released API artifacts are available from GitHub Packages. Add this repository
and use `dragonaltar-api` as a provided dependency; do not depend on the
`dragonaltar` implementation artifact:

```xml
<properties>
  <maven.compiler.release>21</maven.compiler.release>
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
```

`provided` is important: do not shade or bundle DragonAltar into an add-on.
The API artifact version matches its plugin release (`1.4.21` here), while
`api.apiVersion()` reports the independent public contract version (`3.0`).

GitHub's Maven registry requires authentication. Store a GitHub username and a
classic personal access token with `read:packages` in `~/.m2/settings.xml`; the
server id must match the `github` repository id above:

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

If the repository/package is private, that GitHub account must also have read
access. A GitHub Actions workflow in a repository granted package access can use
its `GITHUB_TOKEN` with `packages: read` instead of storing a personal token.

## 2. Describe the Bukkit plugin

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

## 3. Register with DragonAltar

```java
package example.embertools;

import com.dragonaltar.api.DragonAltarApi;
import com.dragonaltar.api.addon.DragonAddonAbility;
import com.dragonaltar.api.addon.DragonAddonItem;
import com.dragonaltar.api.addon.DragonAltarAddon;
import com.dragonaltar.api.model.DragonActionResult;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

public final class EmberToolsPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        DragonAltarApi api = Bukkit.getServicesManager().load(DragonAltarApi.class);
        if (api == null) {
            throw new IllegalStateException("DragonAltar API is unavailable");
        }

        api.registerAddon(this, new DragonAltarAddon(
                "ember-tools",
                "Ember Tools",
                getPluginMeta().getVersion(),
                "ExampleAuthor",
                "A free independent DragonAltar add-on"
        ));
        api.registerAbility(this, new EmberStep());
        api.registerItem(this, new FrostVestment());

        ItemStack vestment = new ItemStack(Material.DIAMOND_CHESTPLATE);
        api.tagSoulBound(vestment, "ember-tools:frost-vestment");
        // Give or craft the tagged stack through the add-on's normal gameplay.
    }

    private static final class EmberStep implements DragonAddonAbility {
        @Override public String id() { return "ember-tools:ember-step"; }
        @Override public String displayName() { return "Ember Step"; }
        @Override public Category category() { return Category.MOVEMENT; }
        @Override public int energyCost() { return 20; }
        @Override public long cooldownMillis() { return 8_000L; }
        @Override public Set<String> supportedSouls() { return Set.of("Rev"); }

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
            player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 12, .2, .1, .2, .02);
            return DragonActionResult.ok();
        }
    }

    private static final class FrostVestment implements DragonAddonItem {
        @Override public String id() { return "ember-tools:frost-vestment"; }
        @Override public String displayName() { return "Frost Vestment"; }
        @Override public String soulId() { return "Akuma"; }
        @Override public StripPolicy onSoulLoss() { return StripPolicy.UNEQUIP; }

        @Override
        public DragonActionResult canEquip(Context context) {
            return context.player().hasPermission("embertools.frost-vestment")
                    ? DragonActionResult.ok()
                    : DragonActionResult.failure("You cannot wear the Frost Vestment");
        }
    }
}
```

DragonAltar removes the ability and item registrations automatically during plugin disable. Calling
`unregisterAddon(this)` manually is only needed when an add-on disables its own
features while the Bukkit plugin remains enabled.

## 4. Include the ownership notice

The add-on README or project page and the distributed jar must visibly contain:

> DragonAltar is owned by Silverfox0338. This add-on is independently developed
> and is not the official DragonAltar plugin.

Place a text copy in `src/main/resources/DRAGONALTAR-NOTICE.txt` so Maven includes
it in the add-on jar. Add-ons and all DragonAltar-related features must remain
free. Review [LICENSE.md](LICENSE.md) before distribution.

## Integration rules

- Use only `com.dragonaltar.api` types. Internal packages may change without
  notice.
- Compile against the oldest plugin-release API artifact you support. Compatible
  additions stay within contract `3.x`; breaking API changes increment the
  contract major returned by `apiVersion()`.
- Keep registration and Bukkit world changes on the server thread.
- Use immutable soul snapshots for player-visible information.
- Never expose administrative event participants or private custody.
- Return a failed `DragonActionResult` instead of throwing for an expected
  gameplay denial.
- Register item definitions before calling `tagSoulBound`. Tags remain on stacks
  across restarts; enforcement resumes when the owning add-on registers the id.
- Choose `NONE`, `UNEQUIP`, `DROP`, or `DESTROY` from `onSoulLoss`. `NONE` keeps
  manual handling; UNEQUIP falls back to dropping when no safe inventory slot
  exists.
- Listen to `DragonAddonItemEquipEvent` for additional equip vetoes. Items using
  `NONE` can use `DragonSoulTransferEvent` or `DragonbornLoseEvent` for custom
  effects when their soul moves.
- Avoid blocking work in ability callbacks; schedule slow work asynchronously
  and return to the server thread before using Bukkit world APIs.
