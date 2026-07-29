package com.dragonaltar.ability.resonance;

import com.dragonaltar.ability.AbilityCategory;
import com.dragonaltar.ability.AbilityService;
import com.dragonaltar.ability.ConfiguredResonanceAbility;
import com.dragonaltar.ability.DragonResonance;
public final class ThermalConvergence extends ConfiguredResonanceAbility {
    public ThermalConvergence(AbilityService service) {
        super(service, DragonResonance.THERMAL_CONVERGENCE,
                "<gradient:aqua:red><bold>Thermal Convergence</bold></gradient>", AbilityCategory.OFFENSE);
    }
}
