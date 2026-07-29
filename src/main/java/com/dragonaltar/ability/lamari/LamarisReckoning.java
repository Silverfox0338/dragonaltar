package com.dragonaltar.ability.lamari;

import com.dragonaltar.ability.AbilityCategory;
import com.dragonaltar.ability.AbilityContext;
import com.dragonaltar.ability.AbilityResult;
import com.dragonaltar.ability.AbilityService;
import com.dragonaltar.ability.ConfiguredAbility;
import com.dragonaltar.soul.SoulIdentity;
import org.bukkit.entity.Player;

import java.util.Set;

public final class LamarisReckoning extends ConfiguredAbility {
    public LamarisReckoning(AbilityService service) {
        super(service, "lamaris-reckoning", "<gold>Lamari's Reckoning", AbilityCategory.OFFENSE,
                Set.of(SoulIdentity.LAMARI), false);
    }

    @Override protected int defaultEnergyCost() { return 60; }
    @Override protected int defaultCooldownSeconds() { return 60; }
    @Override public AbilityResult canUse(AbilityContext context) {
        return service.grounded(context.player())
                ? AbilityResult.ok()
                : AbilityResult.fail("Lamari's Reckoning requires solid ground.");
    }
    @Override protected void activate(Player player) { service.activateLamarisReckoning(player); }
}
