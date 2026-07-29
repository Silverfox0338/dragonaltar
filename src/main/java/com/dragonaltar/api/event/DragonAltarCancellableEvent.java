package com.dragonaltar.api.event;

import org.bukkit.event.Cancellable;

public abstract class DragonAltarCancellableEvent extends DragonAltarEvent implements Cancellable {
    private boolean cancelled;
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
