package example.addon;

import com.dragonaltar.api.DragonAltarApi;
import com.dragonaltar.api.addon.DragonAddonAbility;
import com.dragonaltar.api.addon.DragonAltarAddon;
import com.dragonaltar.api.event.DragonAbilityCastEvent;
import com.dragonaltar.api.model.DragonActionResult;
import com.dragonaltar.api.model.DragonSoulInfo;
import java.util.Optional;
import java.util.Set;
import org.bukkit.entity.Player;

/**
 * Compile-only proof that an external package needs no DragonAltar
 * implementation classes.
 */
public final class ExternalAddonFixture implements DragonAddonAbility {
	public DragonAltarAddon metadata() {
		return new DragonAltarAddon("fixture", "Fixture", "1.0.0", "DragonAltar", "API compilation test");
	}

	public Optional<DragonSoulInfo> soul(DragonAltarApi api, Player player) {
		return api.soulInfoOf(player.getUniqueId());
	}

	public String castingAbility(DragonAbilityCastEvent event) {
		return event.abilityId();
	}

	@Override
	public String id() {
		return "fixture:ability";
	}

	@Override
	public String displayName() {
		return "Fixture Ability";
	}

	@Override
	public Category category() {
		return Category.MOVEMENT;
	}

	@Override
	public int energyCost() {
		return 1;
	}

	@Override
	public long cooldownMillis() {
		return 1_000L;
	}

	@Override
	public Set<String> supportedSouls() {
		return Set.of("Akuma");
	}

	@Override
	public DragonActionResult activate(Context context) {
		return DragonActionResult.ok();
	}
}
