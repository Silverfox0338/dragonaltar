package com.dragonaltar.api.event;
import java.util.UUID;
import org.bukkit.event.HandlerList;
public final class DragonSoulReserveEvent extends DragonAltarCancellableEvent {
	private static final HandlerList HANDLERS = new HandlerList();
	private final String soulId;
	private final UUID player;
	public DragonSoulReserveEvent(String soulId, UUID player) {
		this.soulId = soulId;
		this.player = player;
	}
	public String soulId() {
		return soulId;
	}
	public UUID player() {
		return player;
	}
	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}
	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
