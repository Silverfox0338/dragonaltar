package com.dragonaltar.ability.rev;

import com.dragonaltar.ability.AbilityCategory;
import com.dragonaltar.ability.AbilityService;
import com.dragonaltar.ability.ConfiguredAbility;
import com.dragonaltar.soul.SoulIdentity;
import org.bukkit.entity.Player;

import java.util.Set;

public final class InfernosWrath extends ConfiguredAbility {
    public InfernosWrath(AbilityService service) {
        super(service, "infernos-wrath", "<red><bold>Inferno's Wrath</bold>", AbilityCategory.OFFENSE,
                Set.of(SoulIdentity.REV), true);
    }

    @Override protected int defaultEnergyCost() { return 100; }
    @Override protected int defaultCooldownSeconds() { return 120; }
    @Override protected void activate(Player player) { service.activateInfernosWrath(player); }
}
