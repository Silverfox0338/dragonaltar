package com.dragonaltar.ability;

import com.dragonaltar.api.event.DragonEnergyChangeEvent;
import com.dragonaltar.config.ConfigService;
import com.dragonaltar.player.PlayerDataService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class AbilityEnergyManager {
	private final ConfigService config;
	private final PlayerDataService players;
	private final Map<UUID, Integer> energy = new HashMap<>();
	private final Map<UUID, Long> regenerationBlockedUntil = new HashMap<>();

	AbilityEnergyManager(ConfigService config, PlayerDataService players) {
		this.config = config;
		this.players = players;
	}

	int current(Player player) {
		return energy.computeIfAbsent(player.getUniqueId(), id -> players.energy(id).orElse(maximum()));
	}

	int maximum() {
		return config.file("abilities.yml").getInt("energy.maximum", 100);
	}

	void set(Player player, int value, boolean persist) {
		int old = current(player);
		int bounded = bound(value);
		DragonEnergyChangeEvent event = new DragonEnergyChangeEvent(player, old, bounded);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return;
		}
		int stored = bound(event.newEnergy());
		energy.put(player.getUniqueId(), stored);
		if (persist) {
			players.energy(player.getUniqueId(), stored);
		}
	}

	boolean regenerationAllowed(UUID playerId, long now) {
		return regenerationBlockedUntil.getOrDefault(playerId, 0L) <= now;
	}

	void blockRegeneration(UUID playerId, long until) {
		regenerationBlockedUntil.merge(playerId, until, Math::max);
	}

	void persistAndRemove(UUID playerId) {
		Integer value = energy.remove(playerId);
		if (value != null) {
			players.energy(playerId, value);
		}
		regenerationBlockedUntil.remove(playerId);
	}

	void remove(UUID playerId) {
		energy.remove(playerId);
		regenerationBlockedUntil.remove(playerId);
	}

	void persistAllAndClear() {
		energy.forEach(players::energy);
		clear();
	}

	void clear() {
		energy.clear();
		regenerationBlockedUntil.clear();
	}

	private int bound(int value) {
		return Math.max(0, Math.min(maximum(), value));
	}
}
