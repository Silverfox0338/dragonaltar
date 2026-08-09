package com.dragonaltar.api.addon;

import java.util.Objects;

/**
 * Metadata for an independent DragonAltar add-on.
 *
 * @param id
 *            lowercase namespace used by the add-on's abilities
 */
public record DragonAltarAddon(String id, String name, String version, String author, String description) {
	public DragonAltarAddon {
		id = require(id, "id");
		name = require(name, "name");
		version = require(version, "version");
		author = require(author, "author");
		description = Objects.requireNonNullElse(description, "").trim();
	}

	private static String require(String value, String field) {
		if (value == null || value.isBlank())
			throw new IllegalArgumentException(field + " cannot be blank");
		return value.trim();
	}
}
