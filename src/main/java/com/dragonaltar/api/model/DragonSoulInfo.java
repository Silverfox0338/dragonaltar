package com.dragonaltar.api.model;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Public, immutable view of a Dragon Soul.
 *
 * <p>Administrative custody is deliberately redacted. When a soul is held by a
 * private staff account, this view reports it as dormant with no holder.</p>
 */
public record DragonSoulInfo(
        String id,
        String name,
        String status,
        UUID holder,
        Instant createdAt,
        Instant limboReturnAt
) {
    public Optional<UUID> holderId() {
        return Optional.ofNullable(holder);
    }

    public Optional<Instant> limboReturnTime() {
        return Optional.ofNullable(limboReturnAt);
    }
}
