package com.dragonaltar.api.event;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
public final class DragonAbilitySelectEvent extends DragonAltarCancellableEvent {
    private static final HandlerList HANDLERS=new HandlerList();
    private final Player player; private final String abilityId;
    public DragonAbilitySelectEvent(Player player,String abilityId){this.player=player;this.abilityId=abilityId;}
    public Player player(){return player;} public String abilityId(){return abilityId;}
    @Override public HandlerList getHandlers(){return HANDLERS;} public static HandlerList getHandlerList(){return HANDLERS;}
}
