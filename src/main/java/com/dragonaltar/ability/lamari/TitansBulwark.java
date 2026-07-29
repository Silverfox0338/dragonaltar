package com.dragonaltar.ability.lamari;

import com.dragonaltar.ability.AbilityCategory;
import com.dragonaltar.ability.AbilityService;
import com.dragonaltar.ability.ConfiguredAbility;
import com.dragonaltar.soul.SoulIdentity;
import org.bukkit.entity.Player;

import java.util.Set;

public final class TitansBulwark extends ConfiguredAbility {
    public TitansBulwark(AbilityService service) {
        super(service, "titans-bulwark", "<gray><bold>Titan's Bulwark</bold>", AbilityCategory.DEFENSE,
                Set.of(SoulIdentity.LAMARI), true);
    }

    @Override protected int defaultEnergyCost() { return 100; }
    @Override protected int defaultCooldownSeconds() { return 120; }
    @Override protected void activate(Player player) { service.activateTitansBulwark(player); }
}
