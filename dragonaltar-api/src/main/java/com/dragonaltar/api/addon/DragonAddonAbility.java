package com.dragonaltar.api.addon;

import com.dragonaltar.api.DragonAltarApi;
import com.dragonaltar.api.model.DragonActionResult;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * A safe custom ability supplied by a registered add-on.
 *
 * <p>
 * The id must be namespaced with the add-on id, for example
 * {@code my-addon:frost-step}. Soul values may be canonical ids or public
 * names.
 * </p>
 */
public interface DragonAddonAbility {
	String id();
	String displayName();
	Category category();
	int energyCost();
	long cooldownMillis();
	Set<String> supportedSouls();

	default boolean ultimate() {
		return false;
	}

	default DragonActionResult canUse(Context context) {
		return DragonActionResult.ok();
	}

	DragonActionResult activate(Context context);

	enum Category {
		MOVEMENT, OFFENSE, SENSES, DEFENSE
	}

	record Context(Player player, DragonAltarApi api) {
	}
}
