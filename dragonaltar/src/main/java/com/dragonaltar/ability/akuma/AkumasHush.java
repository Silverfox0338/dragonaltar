package com.dragonaltar.ability.akuma;

import com.dragonaltar.ability.AbilityCategory;
import com.dragonaltar.ability.AbilityService;
import com.dragonaltar.ability.ConfiguredAbility;
import com.dragonaltar.soul.SoulIdentity;
import org.bukkit.entity.Player;

import java.util.Set;

public final class AkumasHush extends ConfiguredAbility {
	public AkumasHush(AbilityService service) {
		super(service, "akumas-hush", "<blue>Akuma's Hush", AbilityCategory.OFFENSE, Set.of(SoulIdentity.AKUMA), false);
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
		service.activateAkumasHush(player);
	}
}
