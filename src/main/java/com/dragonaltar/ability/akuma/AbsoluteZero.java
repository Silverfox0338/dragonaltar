package com.dragonaltar.ability.akuma;

import com.dragonaltar.ability.AbilityCategory;
import com.dragonaltar.ability.AbilityService;
import com.dragonaltar.ability.ConfiguredAbility;
import com.dragonaltar.soul.SoulIdentity;
import org.bukkit.entity.Player;

import java.util.Set;

public final class AbsoluteZero extends ConfiguredAbility {
	public AbsoluteZero(AbilityService service) {
		super(service, "absolute-zero", "<aqua><bold>Absolute Zero</bold>", AbilityCategory.OFFENSE,
				Set.of(SoulIdentity.AKUMA), true);
	}

	@Override
	protected int defaultEnergyCost() {
		return 100;
	}
	@Override
	protected int defaultCooldownSeconds() {
		return 120;
	}
	@Override
	protected void activate(Player player) {
		service.activateAbsoluteZero(player);
	}
}
