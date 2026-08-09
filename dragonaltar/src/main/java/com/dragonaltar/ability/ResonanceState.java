package com.dragonaltar.ability;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class ResonanceState {
	private final Set<String> abilityIds = new HashSet<>();
	private final Map<UUID, String> unlocked = new HashMap<>();
	private final Map<UUID, Integer> glacialWardCharges = new HashMap<>();
	private final Map<UUID, Long> volcanicAegisUntil = new HashMap<>();
	private final Map<UUID, Long> volcanicRetaliationReady = new HashMap<>();

	void register(String abilityId) {
		abilityIds.add(abilityId);
	}

	boolean isResonance(String abilityId) {
		return abilityIds.contains(abilityId);
	}

	String unlocked(UUID playerId) {
		return unlocked.get(playerId);
	}

	void setUnlocked(UUID playerId, String abilityId) {
		if (abilityId == null) {
			unlocked.remove(playerId);
		} else {
			unlocked.put(playerId, abilityId);
		}
	}

	void retainUnlocked(Set<UUID> playerIds) {
		unlocked.keySet().removeIf(id -> !playerIds.contains(id));
	}

	void grantWard(UUID playerId, int charges) {
		glacialWardCharges.put(playerId, charges);
	}

	int consumeWard(UUID playerId) {
		int remaining = Math.max(0, glacialWardCharges.getOrDefault(playerId, 0) - 1);
		if (remaining == 0) {
			glacialWardCharges.remove(playerId);
		} else {
			glacialWardCharges.put(playerId, remaining);
		}
		return remaining;
	}

	int wardCharges(UUID playerId) {
		return glacialWardCharges.getOrDefault(playerId, 0);
	}

	void clearWard(UUID playerId) {
		glacialWardCharges.remove(playerId);
	}

	void grantAegis(UUID playerId, long expiresAt) {
		volcanicAegisUntil.put(playerId, expiresAt);
	}

	boolean hasAegis(UUID playerId, long now) {
		return volcanicAegisUntil.getOrDefault(playerId, 0L) > now;
	}

	void clearAegis(UUID playerId) {
		volcanicAegisUntil.remove(playerId);
	}

	boolean claimRetaliation(UUID key, long now, long readyAt) {
		if (volcanicRetaliationReady.getOrDefault(key, 0L) > now) {
			return false;
		}
		volcanicRetaliationReady.put(key, readyAt);
		return true;
	}

	void remove(UUID playerId) {
		unlocked.remove(playerId);
		glacialWardCharges.remove(playerId);
		volcanicAegisUntil.remove(playerId);
		volcanicRetaliationReady.remove(playerId);
	}

	void clear() {
		unlocked.clear();
		glacialWardCharges.clear();
		volcanicAegisUntil.clear();
		volcanicRetaliationReady.clear();
	}
}
