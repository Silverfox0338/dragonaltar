package com.dragonaltar.api.event;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.HandlerList;
public final class AncientDragonDeathEvent extends DragonAltarEvent {
    private static final HandlerList HANDLERS=new HandlerList();
    private final EnderDragon dragon; private final String method;
    public AncientDragonDeathEvent(EnderDragon dragon,String method){this.dragon=dragon;this.method=method;}
    public EnderDragon dragon(){return dragon;} public String method(){return method;}
    @Override public HandlerList getHandlers(){return HANDLERS;} public static HandlerList getHandlerList(){return HANDLERS;}
}
