package com.dragonaltar.compat;

import org.bukkit.attribute.Attribute;

/**
 * Attribute names changed in Paper 1.21.2; resolve either spelling at runtime.
 */
public final class ServerAttributes {
	public static final Attribute MAX_HEALTH = resolve("MAX_HEALTH", "GENERIC_MAX_HEALTH");
	public static final Attribute MOVEMENT_SPEED = resolve("MOVEMENT_SPEED", "GENERIC_MOVEMENT_SPEED");
	public static final Attribute ARMOR_TOUGHNESS = resolve("ARMOR_TOUGHNESS", "GENERIC_ARMOR_TOUGHNESS");
	public static final Attribute ATTACK_DAMAGE = resolve("ATTACK_DAMAGE", "GENERIC_ATTACK_DAMAGE");
	public static final Attribute KNOCKBACK_RESISTANCE = resolve("KNOCKBACK_RESISTANCE",
			"GENERIC_KNOCKBACK_RESISTANCE");

	private ServerAttributes() {
	}

	private static Attribute resolve(String currentName, String originalName) {
		try {
			return Attribute.valueOf(currentName);
		} catch (IllegalArgumentException ignored) {
			return Attribute.valueOf(originalName);
		}
	}
}
