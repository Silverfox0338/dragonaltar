package com.dragonaltar.ability;

import com.dragonaltar.player.PlayerDataService;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

final class AbilityCooldownTracker {
	static final String ULTIMATE_GROUP = "_shared_ultimate";
	static final String RESONANCE_GROUP = "_shared_resonance";

	private final Function<UUID, Map<String, Long>> loader;
	private final BiConsumer<UUID, Map<String, Long>> saver;
	private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

	AbilityCooldownTracker(PlayerDataService players) {
		this(players::cooldowns, players::cooldowns);
	}

	AbilityCooldownTracker(Function<UUID, Map<String, Long>> loader, BiConsumer<UUID, Map<String, Long>> saver) {
		this.loader = loader;
		this.saver = saver;
	}

	Map<String, Long> view(Player player) {
		return Collections.unmodifiableMap(mutable(player.getUniqueId()));
	}

	Map<String, Long> mutable(UUID playerId) {
		return cooldowns.computeIfAbsent(playerId, loader);
	}

	boolean active(UUID playerId, String key, long now) {
		return mutable(playerId).getOrDefault(key, 0L) > now;
	}

	long remainingSeconds(UUID playerId, String key, long now) {
		long ready = mutable(playerId).getOrDefault(key, 0L);
		return Math.max(0, (long) Math.ceil((ready - now) / 1000d));
	}

	void start(UUID playerId, String key, long readyAt) {
		mutable(playerId).put(key, readyAt);
	}

	void startAndPersist(UUID playerId, String key, long readyAt) {
		start(playerId, key, readyAt);
		persist(playerId);
	}

	void persist(UUID playerId) {
		saver.accept(playerId, mutable(playerId));
	}

	void clear(Player player, String ability, boolean ultimate, boolean resonance) {
		Map<String, Long> values = mutable(player.getUniqueId());
		if (ability.equalsIgnoreCase("all")) {
			values.clear();
		} else {
			values.remove(ability);
			if (ultimate) {
				values.remove(ULTIMATE_GROUP);
			}
			if (resonance) {
				values.remove(RESONANCE_GROUP);
			}
		}
		persist(player.getUniqueId());
	}

	void remove(UUID playerId) {
		cooldowns.remove(playerId);
	}

	void clear() {
		cooldowns.clear();
	}
}
