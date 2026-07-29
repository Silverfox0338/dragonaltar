package com.dragonaltar.api.model;

import java.util.Optional;
import java.util.UUID;

/** Immutable view of the Ancient Dragon event. */
public record DragonEventInfo(String state, String altarState, UUID sessionId, UUID dragonId) {
    public Optional<UUID> session() {
        return Optional.ofNullable(sessionId);
    }

    public Optional<UUID> dragon() {
        return Optional.ofNullable(dragonId);
    }
}
