package com.dragonaltar.api.event;
import java.util.UUID;
import org.bukkit.event.HandlerList;
public final class DragonSoulTransferStartEvent extends DragonAltarCancellableEvent {
	private static final HandlerList HANDLERS = new HandlerList();
	private final String soulId;
	private final UUID from;
	private final UUID to;
	public DragonSoulTransferStartEvent(String soulId, UUID from, UUID to) {
		this.soulId = soulId;
		this.from = from;
		this.to = to;
	}
	public String soulId() {
		return soulId;
	}
	public UUID from() {
		return from;
	}
	public UUID to() {
		return to;
	}
	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}
	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
