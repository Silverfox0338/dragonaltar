package com.dragonaltar.api.model;

import java.util.UUID;

/** Immutable view of the active ritual. Consumed inventory data is never exposed. */
public record DragonRitualInfo(UUID playerId, String soulId, String soulName, String phase, UUID sessionId) {}
