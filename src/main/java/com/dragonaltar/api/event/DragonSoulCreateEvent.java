package com.dragonaltar.api.event;
import org.bukkit.event.HandlerList;
public final class DragonSoulCreateEvent extends DragonAltarEvent {
    private static final HandlerList HANDLERS=new HandlerList();
    private final String soulId; public DragonSoulCreateEvent(String soulId){this.soulId=soulId;} public String soulId(){return soulId;}
    @Override public HandlerList getHandlers(){return HANDLERS;} public static HandlerList getHandlerList(){return HANDLERS;}
}
