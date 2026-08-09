package com.dragonaltar.ability.rev;

import com.dragonaltar.ability.AbilityCategory;
import com.dragonaltar.ability.AbilityService;
import com.dragonaltar.ability.ConfiguredAbility;
import com.dragonaltar.soul.SoulIdentity;
import org.bukkit.entity.Player;

import java.util.Set;

public final class RevsRend extends ConfiguredAbility {
	public RevsRend(AbilityService service) {
		super(service, "revs-rend", "<gold>Rev's Rend", AbilityCategory.MOVEMENT, Set.of(SoulIdentity.REV), false);
	}

	@Override
	protected int defaultEnergyCost() {
		return 25;
	}
	@Override
	protected int defaultCooldownSeconds() {
		return 12;
	}
	@Override
	protected void activate(Player player) {
		service.activateRevsRend(player);
	}
}
