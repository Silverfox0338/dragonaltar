package com.dragonaltar.ability.rev;

import com.dragonaltar.ability.AbilityCategory;
import com.dragonaltar.ability.AbilityService;
import com.dragonaltar.ability.ConfiguredAbility;
import com.dragonaltar.soul.SoulIdentity;
import org.bukkit.entity.Player;

import java.util.Set;

public final class WrathOfRev extends ConfiguredAbility {
	public WrathOfRev(AbilityService service) {
		super(service, "wrath-of-rev", "<red>Wrath of Rev", AbilityCategory.OFFENSE, Set.of(SoulIdentity.REV), false);
	}

	@Override
	protected int defaultEnergyCost() {
		return 60;
	}
	@Override
	protected int defaultCooldownSeconds() {
		return 60;
	}
	@Override
	protected void activate(Player player) {
		service.activateWrathOfRev(player);
	}
}
