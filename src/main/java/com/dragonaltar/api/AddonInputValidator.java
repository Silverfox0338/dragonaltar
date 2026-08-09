package com.dragonaltar.api;

import com.dragonaltar.api.addon.DragonAddonAbility;
import com.dragonaltar.api.addon.DragonAddonItem;
import com.dragonaltar.api.addon.DragonAltarAddon;
import com.dragonaltar.soul.SoulIdentity;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Pure validation for untrusted add-on metadata and registrations. */
final class AddonInputValidator {
	private static final Pattern ADDON_ID = Pattern.compile("[a-z0-9][a-z0-9._-]{1,31}");
	private static final Pattern ABILITY_PART = Pattern.compile("[a-z0-9][a-z0-9._-]{1,47}");
	private static final Pattern ITEM_PART = Pattern.compile("[a-z0-9][a-z0-9._-]{1,47}");
	private static final long MAX_COOLDOWN = Duration.ofHours(24).toMillis();

	String validateAddon(DragonAltarAddon addon) {
		Objects.requireNonNull(addon, "addon");
		String id = addon.id().toLowerCase(Locale.ROOT);
		if (!ADDON_ID.matcher(id).matches()) {
			throw new IllegalArgumentException(
					"Add-on id must be 2-32 lowercase letters, numbers, dots, dashes, or underscores");
		}
		if (id.equals("dragonaltar")) {
			throw new IllegalArgumentException("The dragonaltar namespace is reserved");
		}
		if (!id.equals(addon.id())) {
			throw new IllegalArgumentException("Add-on id must already be lowercase");
		}
		return id;
	}

	void validateAbility(DragonAltarAddon addon, DragonAddonAbility ability, int maximumEnergy) {
		Objects.requireNonNull(ability, "ability");
		validateNamespacedId(ability.id(), addon.id(), ABILITY_PART, "Ability");
		if (ability.displayName() == null || ability.displayName().isBlank()) {
			throw new IllegalArgumentException("Ability display name cannot be blank");
		}
		Objects.requireNonNull(ability.category(), "ability category");
		if (ability.energyCost() < 0 || ability.energyCost() > maximumEnergy) {
			throw new IllegalArgumentException("Ability energy cost must be between 0 and maximum Dragon Energy");
		}
		if (ability.cooldownMillis() < 0 || ability.cooldownMillis() > MAX_COOLDOWN) {
			throw new IllegalArgumentException("Ability cooldown must be between 0 and 24 hours");
		}
		Set<String> supported = Objects.requireNonNull(ability.supportedSouls(), "supported souls");
		if (supported.isEmpty()) {
			throw new IllegalArgumentException("Ability must support at least one soul");
		}
		supported.forEach(this::identity);
	}

	String validateItem(DragonAltarAddon addon, DragonAddonItem item) {
		Objects.requireNonNull(item, "item");
		validateNamespacedId(item.id(), addon.id(), ITEM_PART, "Item");
		if (item.displayName() == null || item.displayName().isBlank()) {
			throw new IllegalArgumentException("Item display name cannot be blank");
		}
		identity(item.soulId());
		return item.id();
	}

	boolean validNamespacedItemId(String id) {
		if (id == null) {
			return false;
		}
		int colon = id.indexOf(':');
		return colon > 0 && colon == id.lastIndexOf(':') && ADDON_ID.matcher(id.substring(0, colon)).matches()
				&& ITEM_PART.matcher(id.substring(colon + 1)).matches();
	}

	SoulIdentity identity(String value) {
		for (SoulIdentity identity : SoulIdentity.values()) {
			if (identity.id().equalsIgnoreCase(value) || identity.displayName().equalsIgnoreCase(value)) {
				return identity;
			}
		}
		throw new IllegalArgumentException("Unknown soul: " + value);
	}

	private static void validateNamespacedId(String id, String namespace, Pattern localPart, String type) {
		if (id == null || !id.startsWith(namespace + ":")) {
			throw new IllegalArgumentException(type + " id must use the " + namespace + ": namespace");
		}
		String localId = id.substring(namespace.length() + 1);
		if (!localPart.matcher(localId).matches()) {
			throw new IllegalArgumentException(
					type + " name must be 2-48 lowercase letters, numbers, dots, dashes, or underscores");
		}
	}
}
