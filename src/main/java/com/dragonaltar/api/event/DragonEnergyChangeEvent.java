package com.dragonaltar.api.event;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
public final class DragonEnergyChangeEvent extends DragonAltarCancellableEvent {
    private static final HandlerList HANDLERS=new HandlerList();
    private final Player player; private final int oldEnergy; private int newEnergy;
    public DragonEnergyChangeEvent(Player player,int oldEnergy,int newEnergy){this.player=player;this.oldEnergy=oldEnergy;this.newEnergy=newEnergy;}
    public Player player(){return player;} public int oldEnergy(){return oldEnergy;} public int newEnergy(){return newEnergy;} public void newEnergy(int value){newEnergy=value;}
    @Override public HandlerList getHandlers(){return HANDLERS;} public static HandlerList getHandlerList(){return HANDLERS;}
}
