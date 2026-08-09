package com.dragonaltar.api.event;
import java.util.UUID;
import org.bukkit.event.HandlerList;
public final class AncientDragonEventStartEvent extends DragonAltarEvent {
	private static final HandlerList HANDLERS = new HandlerList();
	private final UUID sessionId;
	public AncientDragonEventStartEvent(UUID sessionId) {
		this.sessionId = sessionId;
	}
	public UUID sessionId() {
		return sessionId;
	}
	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}
	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
