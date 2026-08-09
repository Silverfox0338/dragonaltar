package com.dragonaltar.soul;
import java.util.*;
public final class SoulRules {
	private SoulRules() {
	}
	public static void validate(Collection<DragonSoul> souls) {
		if (souls.size() > SoulIdentity.MAX_DRAGONBORN)
			throw new IllegalStateException("More than three Dragon Souls");
		Set<String> ids = new HashSet<>();
		Set<UUID> holders = new HashSet<>();
		for (DragonSoul soul : souls) {
			if (!SoulIdentity.CANONICAL_IDS.contains(soul.id()))
				throw new IllegalStateException("Non-canonical soul id");
			if (!ids.add(soul.id()))
				throw new IllegalStateException("Duplicate soul id");
			if (soul.holder() != null && !holders.add(soul.holder()))
				throw new IllegalStateException("Duplicate soul holder");
		}
		if (holders.size() > SoulIdentity.MAX_DRAGONBORN)
			throw new IllegalStateException("More than three Dragonborn");
	}
}
