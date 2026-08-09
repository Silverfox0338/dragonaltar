package com.dragonaltar.ability.shared;

import com.dragonaltar.ability.AbilityCategory;
import com.dragonaltar.ability.AbilityService;
import com.dragonaltar.ability.ConfiguredAbility;
import org.bukkit.entity.Player;

import java.util.Set;

public final class Roar extends ConfiguredAbility {
	public Roar(AbilityService service) {
		super(service, "roar", "<red>Roar", AbilityCategory.OFFENSE, Set.of(), false);
	}

	@Override
	protected int defaultEnergyCost() {
		return 35;
	}
	@Override
	protected int defaultCooldownSeconds() {
		return 25;
	}
	@Override
	protected void activate(Player player) {
		service.activateRoar(player);
	}
}
