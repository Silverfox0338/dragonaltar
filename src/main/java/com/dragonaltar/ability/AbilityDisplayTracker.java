package com.dragonaltar.ability;

import com.dragonaltar.DragonAltarPlugin;
import com.dragonaltar.player.EffectMode;
import com.dragonaltar.player.PlayerDataService;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

final class AbilityDisplayTracker {
    private final DragonAltarPlugin plugin;
    private final PlayerDataService players;
    private final Set<Display> active = new HashSet<>();

    AbilityDisplayTracker(DragonAltarPlugin plugin, PlayerDataService players) {
        this.plugin = plugin;
        this.players = players;
    }

    <T extends Display> T track(T display) {
        active.add(display);
        refreshVisibility(display);
        return display;
    }

    void refreshVisibility(Display display) {
        for (Player viewer : display.getWorld().getPlayers()) {
            var settings = players.settings(viewer.getUniqueId());
            if (!settings.animationParticles() || settings.effects() == EffectMode.MINIMAL) {
                viewer.hideEntity(plugin, display);
            } else {
                viewer.showEntity(plugin, display);
            }
        }
    }

    void remove(Collection<? extends Entity> displays) {
        for (Entity display : displays) {
            if (display instanceof Display visual) {
                active.remove(visual);
            }
            if (display.isValid()) {
                display.remove();
            }
        }
    }

    void removeAll() {
        remove(new ArrayList<>(active));
    }
}
