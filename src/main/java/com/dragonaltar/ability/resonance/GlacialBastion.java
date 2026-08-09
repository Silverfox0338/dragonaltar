package com.dragonaltar.ability.resonance;

import com.dragonaltar.ability.AbilityCategory;
import com.dragonaltar.ability.AbilityService;
import com.dragonaltar.ability.ConfiguredResonanceAbility;
import com.dragonaltar.ability.DragonResonance;
public final class GlacialBastion extends ConfiguredResonanceAbility {
	public GlacialBastion(AbilityService service) {
		super(service, DragonResonance.GLACIAL_BASTION, "<gradient:aqua:gray><bold>Glacial Bastion</bold></gradient>",
				AbilityCategory.DEFENSE);
	}
}
