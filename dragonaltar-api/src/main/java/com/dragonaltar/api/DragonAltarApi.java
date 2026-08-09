package com.dragonaltar.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import com.dragonaltar.api.addon.DragonAddonAbility;
import com.dragonaltar.api.addon.DragonAddonItem;
import com.dragonaltar.api.addon.DragonAltarAddon;
import com.dragonaltar.api.model.DragonAbilityInfo;
import com.dragonaltar.api.model.DragonActionResult;
import com.dragonaltar.api.model.DragonEligibilityInfo;
import com.dragonaltar.api.model.DragonEventInfo;
import com.dragonaltar.api.model.DragonRitualInfo;
import com.dragonaltar.api.model.DragonSoulInfo;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface DragonAltarApi {
	/** Version of the public API contract, independent from the plugin release. */
	String apiVersion();

	DragonEventInfo event();
	Optional<DragonRitualInfo> activeRitual();
	Collection<DragonSoulInfo> souls();
	Optional<DragonSoulInfo> soulInfo(String idOrName);
	Optional<DragonSoulInfo> soulInfoOf(UUID player);
	DragonEligibilityInfo eligibilityInfo(Player player);

	Collection<String> availableAbilityIds(Player player);
	Optional<String> selectedAbility(Player player);
	long cooldownSeconds(Player player, String abilityId);
	DragonActionResult cast(Player player);
	boolean openSoulHistory(Player player);

	/**
	 * Registers an independent add-on. Registration must happen on the server
	 * thread.
	 */
	void registerAddon(Plugin owner, DragonAltarAddon addon);

	/**
	 * Registers an ability owned by an already registered add-on.
	 */
	void registerAbility(Plugin owner, DragonAddonAbility ability);

	/** Registers a soul-bound equipment definition owned by an add-on. */
	void registerItem(Plugin owner, DragonAddonItem item);

	/** Tags this stack with a registered add-on item id. */
	void tagSoulBound(ItemStack item, String itemId);

	/**
	 * Returns whether this stack carries a syntactically valid soul-bound item
	 * marker.
	 */
	boolean isSoulBound(ItemStack item);

	/** Returns the namespaced item id stored on this stack, if valid. */
	Optional<String> soulBoundItemId(ItemStack item);

	/** Returns all currently registered add-on item ids. */
	Collection<String> itemIds();

	boolean unregisterAddon(Plugin owner);
	Collection<DragonAltarAddon> addons();

	String altarState();
	Collection<UUID> dragonborn();
	Collection<String> abilityIds();
	Optional<DragonAbilityInfo> ability(String id);
	int energy(Player player);
	int maximumEnergy();
	boolean selectAbility(Player player, String abilityId);
}
