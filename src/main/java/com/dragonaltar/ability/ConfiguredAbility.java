package com.dragonaltar.ability;

import com.dragonaltar.soul.SoulIdentity;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Common metadata plumbing for the independently editable ability classes.
 */
public abstract class ConfiguredAbility implements DragonAbility {
    protected final AbilityService service;
    private final String id;
    private final String fallbackName;
    private final AbilityCategory category;
    private final Set<SoulIdentity> souls;
    private final boolean ultimate;

    protected ConfiguredAbility(AbilityService service, String id, String fallbackName,
                                AbilityCategory category, Set<SoulIdentity> souls, boolean ultimate) {
        this.service = service;
        this.id = id;
        this.fallbackName = fallbackName;
        this.category = category;
        this.souls = Set.copyOf(souls);
        this.ultimate = ultimate;
    }

    @Override public final String id() { return id; }
    @Override public final Component displayName() { return service.abilityName(id, fallbackName); }
    @Override public final AbilityCategory category() { return category; }
    @Override public final Set<SoulIdentity> souls() { return souls; }
    @Override public final boolean ultimate() { return ultimate; }

    @Override
    public final int energyCost() {
        return ultimate ? service.maxEnergy() : service.abilityInteger(id + ".energy", defaultEnergyCost());
    }

    @Override
    public final long cooldownMillis() {
        return service.abilityInteger(id + ".cooldown-seconds", defaultCooldownSeconds()) * 1000L;
    }

    @Override
    public AbilityResult activate(AbilityContext context) {
        activate(context.player());
        return AbilityResult.ok();
    }

    protected abstract int defaultEnergyCost();
    protected abstract int defaultCooldownSeconds();
    protected abstract void activate(Player player);
}
