package com.dragonaltar.api.event;
import org.bukkit.event.HandlerList;
public final class DragonAltarDormantEvent extends DragonAltarEvent {
    private static final HandlerList HANDLERS=new HandlerList();
    @Override public HandlerList getHandlers(){return HANDLERS;} public static HandlerList getHandlerList(){return HANDLERS;}
}
