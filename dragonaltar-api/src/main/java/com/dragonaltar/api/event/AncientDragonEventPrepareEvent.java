package com.dragonaltar.api.event;
import org.bukkit.entity.Player;
import java.util.UUID;
import org.bukkit.event.HandlerList;
public final class AncientDragonEventPrepareEvent extends DragonAltarCancellableEvent {
	private static final HandlerList HANDLERS = new HandlerList();
	private final Player initiator;
	private final UUID sessionId;
	public AncientDragonEventPrepareEvent(Player initiator, UUID sessionId) {
		this.initiator = initiator;
		this.sessionId = sessionId;
	}
	public Player initiator() {
		return initiator;
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
