package com.dragonaltar.ability.resonance;

import com.dragonaltar.ability.AbilityCategory;
import com.dragonaltar.ability.AbilityService;
import com.dragonaltar.ability.ConfiguredResonanceAbility;
import com.dragonaltar.ability.DragonResonance;
public final class VolcanicAegis extends ConfiguredResonanceAbility {
    public VolcanicAegis(AbilityService service) {
        super(service, DragonResonance.VOLCANIC_AEGIS,
                "<gradient:red:gray><bold>Volcanic Aegis</bold></gradient>", AbilityCategory.DEFENSE);
    }
}
