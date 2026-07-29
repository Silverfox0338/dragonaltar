package com.dragonaltar.api.event;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
public final class DragonRitualCompleteEvent extends DragonAltarEvent {
    private static final HandlerList HANDLERS=new HandlerList();
    private final Player player; private final String soulId;
    public DragonRitualCompleteEvent(Player player,String soulId){this.player=player;this.soulId=soulId;}
    public Player player(){return player;} public String soulId(){return soulId;}
    @Override public HandlerList getHandlers(){return HANDLERS;} public static HandlerList getHandlerList(){return HANDLERS;}
}
