package com.dragonaltar.ability.shared;

import com.dragonaltar.ability.AbilityCategory;
import com.dragonaltar.ability.AbilityService;
import com.dragonaltar.ability.ConfiguredAbility;
import org.bukkit.entity.Player;

import java.util.Set;

public final class Wings extends ConfiguredAbility {
    public Wings(AbilityService service) {
        super(service, "wings", "<light_purple>Wings", AbilityCategory.MOVEMENT, Set.of(), false);
    }

    @Override protected int defaultEnergyCost() { return 40; }
    @Override protected int defaultCooldownSeconds() { return 45; }
    @Override protected void activate(Player player) { service.activateWings(player); }
}
