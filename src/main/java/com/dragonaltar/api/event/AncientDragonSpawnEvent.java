package com.dragonaltar.api.event;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.HandlerList;
public final class AncientDragonSpawnEvent extends DragonAltarEvent {
	private static final HandlerList HANDLERS = new HandlerList();
	private final EnderDragon dragon;
	public AncientDragonSpawnEvent(EnderDragon dragon) {
		this.dragon = dragon;
	}
	public EnderDragon dragon() {
		return dragon;
	}
	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}
	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
