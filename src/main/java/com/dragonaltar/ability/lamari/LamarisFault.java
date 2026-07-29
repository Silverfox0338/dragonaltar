package com.dragonaltar.ability.lamari;

import com.dragonaltar.ability.AbilityCategory;
import com.dragonaltar.ability.AbilityContext;
import com.dragonaltar.ability.AbilityResult;
import com.dragonaltar.ability.AbilityService;
import com.dragonaltar.ability.ConfiguredAbility;
import com.dragonaltar.soul.SoulIdentity;
import org.bukkit.entity.Player;

import java.util.Set;

public final class LamarisFault extends ConfiguredAbility {
    public LamarisFault(AbilityService service) {
        super(service, "lamaris-fault", "<dark_gray>Lamari's Fault", AbilityCategory.OFFENSE,
                Set.of(SoulIdentity.LAMARI), false);
    }

    @Override protected int defaultEnergyCost() { return 30; }
    @Override protected int defaultCooldownSeconds() { return 18; }
    @Override public AbilityResult canUse(AbilityContext context) {
        return context.player().isFlying()
                ? AbilityResult.ok()
                : AbilityResult.fail("Lamari's Fault must be cast while flying.");
    }
    @Override protected void activate(Player player) { service.activateLamarisFault(player); }
}
