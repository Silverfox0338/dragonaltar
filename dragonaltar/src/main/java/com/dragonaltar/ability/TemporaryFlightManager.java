package com.dragonaltar.ability;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class TemporaryFlightManager {
	private final Map<UUID, Boolean> previousFlight = new HashMap<>();

	boolean enable(Player player) {
		boolean previous = player.getAllowFlight();
		previousFlight.put(player.getUniqueId(), previous);
		player.setAllowFlight(true);
		player.setFlying(true);
		return previous;
	}

	void finish(Player player, boolean previous) {
		previousFlight.remove(player.getUniqueId());
		if (!player.isOnline()) {
			return;
		}
		restore(player, previous);
	}

	void restoreOnLogout(Player player) {
		Boolean previous = previousFlight.remove(player.getUniqueId());
		if (previous != null) {
			restore(player, previous);
		}
	}

	void restoreAll() {
		for (UUID playerId : previousFlight.keySet().toArray(UUID[]::new)) {
			Player player = org.bukkit.Bukkit.getPlayer(playerId);
			if (player != null && player.isOnline()) {
				restoreOnLogout(player);
			} else {
				previousFlight.remove(playerId);
			}
		}
	}

	private void restore(Player player, boolean previous) {
		player.setFlying(false);
		if (!previous && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
			player.setAllowFlight(false);
		}
	}
}
