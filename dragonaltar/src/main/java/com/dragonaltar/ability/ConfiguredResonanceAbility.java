package com.dragonaltar.ability;

import net.kyori.adventure.text.Component;

public abstract class ConfiguredResonanceAbility implements DragonAbility {
	protected final AbilityService service;
	private final DragonResonance resonance;
	private final String fallbackName;
	private final AbilityCategory category;

	protected ConfiguredResonanceAbility(AbilityService service, DragonResonance resonance, String fallbackName,
			AbilityCategory category) {
		this.service = service;
		this.resonance = resonance;
		this.fallbackName = fallbackName;
		this.category = category;
	}

	@Override
	public final String id() {
		return resonance.id();
	}
	@Override
	public final Component displayName() {
		return service.resonanceName(id(), fallbackName);
	}
	@Override
	public final AbilityCategory category() {
		return category;
	}
	@Override
	public final int energyCost() {
		return service.resonanceEnergy(resonance);
	}
	@Override
	public final long cooldownMillis() {
		return service.resonanceCooldownSeconds(resonance) * 1000L;
	}
	@Override
	public final java.util.Set<com.dragonaltar.soul.SoulIdentity> souls() {
		return resonance.souls();
	}
	@Override
	public final AbilityResult canUse(AbilityContext context) {
		return service.canUseResonance(context.player(), resonance);
	}
	@Override
	public final AbilityResult activate(AbilityContext context) {
		service.activateConfiguredResonance(context.player(), resonance);
		return AbilityResult.ok();
	}
}
