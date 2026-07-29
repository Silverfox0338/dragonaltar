package com.dragonaltar.api.event;
import java.util.UUID;
import org.bukkit.event.HandlerList;
public final class DragonbornGainEvent extends DragonAltarEvent {
    private static final HandlerList HANDLERS=new HandlerList();
    private final UUID player; private final String soulId;
    public DragonbornGainEvent(UUID player,String soulId){this.player=player;this.soulId=soulId;}
    public UUID player(){return player;} public String soulId(){return soulId;}
    @Override public HandlerList getHandlers(){return HANDLERS;} public static HandlerList getHandlerList(){return HANDLERS;}
}
