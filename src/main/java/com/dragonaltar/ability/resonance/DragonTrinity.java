package com.dragonaltar.ability.resonance;

import com.dragonaltar.ability.AbilityCategory;
import com.dragonaltar.ability.AbilityService;
import com.dragonaltar.ability.ConfiguredResonanceAbility;
import com.dragonaltar.ability.DragonResonance;
public final class DragonTrinity extends ConfiguredResonanceAbility {
	public DragonTrinity(AbilityService service) {
		super(service, DragonResonance.DRAGON_TRINITY, "<gradient:aqua:red:gold><bold>Dragon Trinity</bold></gradient>",
				AbilityCategory.OFFENSE);
	}
}
