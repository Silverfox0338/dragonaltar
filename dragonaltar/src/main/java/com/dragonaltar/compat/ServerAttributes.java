package com.dragonaltar.compat;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;

/**
 * Attribute names changed in Paper 1.21.2; resolve either spelling at runtime.
 */
public final class ServerAttributes {
	public static final Attribute MAX_HEALTH = resolve("max_health", "generic.max_health");
	public static final Attribute MOVEMENT_SPEED = resolve("movement_speed", "generic.movement_speed");
	public static final Attribute ARMOR_TOUGHNESS = resolve("armor_toughness", "generic.armor_toughness");
	public static final Attribute ATTACK_DAMAGE = resolve("attack_damage", "generic.attack_damage");
	public static final Attribute KNOCKBACK_RESISTANCE = resolve("knockback_resistance",
			"generic.knockback_resistance");

	private ServerAttributes() {
	}

	private static Attribute resolve(String currentKey, String originalKey) {
		Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(currentKey));
		if (attribute == null)
			attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(originalKey));
		if (attribute == null)
			throw new IllegalStateException("Server does not provide the " + currentKey + " attribute");
		return attribute;
	}
}
