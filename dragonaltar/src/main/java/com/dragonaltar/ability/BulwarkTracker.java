package com.dragonaltar.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

final class BulwarkTracker {
	private final Set<UUID> active = new HashSet<>();
	private final Map<UUID, Double> charge = new HashMap<>();

	void activate(UUID playerId) {
		active.add(playerId);
		charge.put(playerId, 0d);
	}

	boolean active(UUID playerId) {
		return active.contains(playerId);
	}

	double charge(UUID playerId) {
		return charge.getOrDefault(playerId, 0d);
	}

	void charge(UUID playerId, double value) {
		charge.put(playerId, value);
	}

	double deactivate(UUID playerId) {
		active.remove(playerId);
		Double stored = charge.remove(playerId);
		return stored == null ? 0d : stored;
	}

	void remove(UUID playerId) {
		active.remove(playerId);
		charge.remove(playerId);
	}

	void clear() {
		active.clear();
		charge.clear();
	}
}
