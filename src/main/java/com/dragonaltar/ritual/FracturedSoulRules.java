package com.dragonaltar.ritual;

import java.util.Collection;
import java.util.Comparator;
import java.util.UUID;

final class FracturedSoulRules {
	private FracturedSoulRules() {
	}

	static boolean waitForTrackedChunk(UUID trackedEntityId, boolean trackedEntityLoaded, boolean trackedChunkLoaded) {
		return trackedEntityId != null && !trackedEntityLoaded && !trackedChunkLoaded;
	}

	static UUID canonical(UUID trackedEntityId, Collection<UUID> loadedEntityIds) {
		if (trackedEntityId != null && loadedEntityIds.contains(trackedEntityId))
			return trackedEntityId;
		return loadedEntityIds.stream().min(Comparator.comparing(UUID::toString)).orElse(null);
	}
}
