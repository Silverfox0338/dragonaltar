package com.dragonaltar.ability.akuma;

import com.dragonaltar.ability.AbilityCategory;
import com.dragonaltar.ability.AbilityService;
import com.dragonaltar.ability.ConfiguredAbility;
import com.dragonaltar.soul.SoulIdentity;
import org.bukkit.entity.Player;

import java.util.Set;

public final class AkumasTrail extends ConfiguredAbility {
	public AkumasTrail(AbilityService service) {
		super(service, "akumas-trail", "<aqua>Akuma's Trail", AbilityCategory.MOVEMENT, Set.of(SoulIdentity.AKUMA),
				false);
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
		service.activateAkumasTrail(player);
	}
}
