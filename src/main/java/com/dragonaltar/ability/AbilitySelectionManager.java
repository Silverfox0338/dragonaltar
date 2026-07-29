package com.dragonaltar.ability;

import com.dragonaltar.player.PlayerDataService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class AbilitySelectionManager {
    private final PlayerDataService players;
    private final Map<UUID, String> selections = new HashMap<>();

    AbilitySelectionManager(PlayerDataService players) {
        this.players = players;
    }

    String selected(UUID playerId, String fallback) {
        return selections.computeIfAbsent(playerId, id -> players.selection(id, fallback));
    }

    void select(UUID playerId, String abilityId) {
        selections.put(playerId, abilityId);
        players.setSelection(playerId, abilityId);
    }

    void remove(UUID playerId) {
        selections.remove(playerId);
    }

    void clear() {
        selections.clear();
    }
}
