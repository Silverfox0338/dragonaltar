package com.dragonaltar.api;

import com.dragonaltar.dragonevent.DragonEventState;
import com.dragonaltar.eligibility.EligibilityService;
import com.dragonaltar.soul.DragonSoul;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import com.dragonaltar.api.model.DragonAbilityInfo;
import com.dragonaltar.ability.AbilityResult;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface DragonAltarApi {
    DragonEventState eventState();
    String altarState();
    Collection<UUID> dragonborn();
    Optional<DragonSoul> soul(String id);
    Optional<DragonSoul> soulOf(UUID player);
    EligibilityService.Result eligibility(Player player);
    Collection<String> abilityIds();
    Optional<DragonAbilityInfo> ability(String id);
    int energy(Player player);
    int maximumEnergy();
    boolean selectAbility(Player player,String abilityId);
    AbilityResult castSelectedAbility(Player player);
}
