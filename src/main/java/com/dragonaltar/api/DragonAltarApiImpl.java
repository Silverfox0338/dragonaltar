package com.dragonaltar.api;

import com.dragonaltar.DragonAltarPlugin;
import com.dragonaltar.eligibility.EligibilityService;
import com.dragonaltar.soul.DragonSoul;
import com.dragonaltar.api.model.DragonAbilityInfo;
import com.dragonaltar.ability.AbilityResult;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.*;

public final class DragonAltarApiImpl implements DragonAltarApi {
    private final DragonAltarPlugin plugin;
    public DragonAltarApiImpl(DragonAltarPlugin plugin) { this.plugin = plugin; }
    @Override public com.dragonaltar.dragonevent.DragonEventState eventState() { return plugin.dragonEvent().state(); }
    @Override public String altarState(){return plugin.dragonEvent().altarState().name();}
    @Override public Collection<UUID> dragonborn() { return plugin.souls().all().stream().map(DragonSoul::holder).filter(Objects::nonNull).toList(); }
    @Override public Optional<DragonSoul> soul(String id) { return plugin.souls().byId(id); }
    @Override public Optional<DragonSoul> soulOf(UUID player) { return plugin.souls().byHolder(player); }
    @Override public EligibilityService.Result eligibility(Player player) { return plugin.eligibility().check(player); }
    @Override public Collection<String> abilityIds() { return plugin.abilities().abilities().stream().map(a->a.id()).toList(); }
    @Override public Optional<DragonAbilityInfo> ability(String id){return plugin.abilities().abilities().stream().filter(a->a.id().equalsIgnoreCase(id)).findFirst().map(a->new DragonAbilityInfo(a.id(),PlainTextComponentSerializer.plainText().serialize(a.displayName()),a.category().name(),a.energyCost(),a.cooldownMillis()));}
    @Override public int energy(Player player) { return plugin.dragonborn().isDragonborn(player.getUniqueId())?plugin.abilities().current(player):0; }
    @Override public int maximumEnergy() { return plugin.abilities().maxEnergy(); }
    @Override public boolean selectAbility(Player player,String abilityId){if(!player.hasPermission("dragonaltar.use")||ability(abilityId).isEmpty()||!plugin.dragonborn().isDragonborn(player.getUniqueId()))return false;plugin.abilities().select(player,abilityId);return plugin.abilities().selected(player).equalsIgnoreCase(abilityId);}
    @Override public AbilityResult castSelectedAbility(Player player){return plugin.abilities().cast(player);}
}
