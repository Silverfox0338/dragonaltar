package com.dragonaltar.soul;

import java.util.Locale;
import java.util.Set;

public enum SoulIdentity {
	AKUMA("soul-1", "Akuma"), REV("soul-2", "Rev"), LAMARI("soul-3", "Lamari");

	public static final int MAX_DRAGONBORN = 3;
	public static final Set<String> CANONICAL_IDS = Set.of("soul-1", "soul-2", "soul-3");

	private final String id;
	private final String displayName;

	SoulIdentity(String id, String displayName) {
		this.id = id;
		this.displayName = displayName;
	}

	public String id() {
		return id;
	}
	public String displayName() {
		return displayName;
	}

	public static SoulIdentity fromId(String id) {
		for (SoulIdentity identity : values())
			if (identity.id.equalsIgnoreCase(id))
				return identity;
		throw new IllegalArgumentException("Unknown soul: " + id);
	}

	public static SoulIdentity fromInput(String value) {
		for (SoulIdentity identity : values())
			if (identity.id.equalsIgnoreCase(value) || identity.displayName.equalsIgnoreCase(value)
					|| identity.name().equalsIgnoreCase(value))
				return identity;
		throw new IllegalArgumentException("Unknown soul: " + value + ". Use Akuma, Rev, or Lamari");
	}

	public static String displayName(String id) {
		try {
			return fromId(id).displayName;
		} catch (IllegalArgumentException ignored) {
			return id == null ? "Soul" : id.toLowerCase(Locale.ROOT);
		}
	}

	public static String replaceIds(String text) {
		if (text == null)
			return "";
		String result = text;
		for (SoulIdentity identity : values())
			result = result.replace(identity.id, identity.displayName);
		return result;
	}
}
