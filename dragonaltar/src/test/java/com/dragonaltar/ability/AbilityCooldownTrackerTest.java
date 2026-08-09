package com.dragonaltar.ability;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbilityCooldownTrackerTest {
	@Test
	void loadsOnceAndRoundsRemainingTimeUp() {
		UUID playerId = UUID.randomUUID();
		int[] loads = {0};
		AbilityCooldownTracker tracker = new AbilityCooldownTracker(id -> {
			loads[0]++;
			return new HashMap<>(Map.of("dash", 2_001L));
		}, (id, values) -> {
		});

		assertEquals(2, tracker.remainingSeconds(playerId, "dash", 1L));
		assertEquals(1, tracker.remainingSeconds(playerId, "dash", 1_001L));
		assertEquals(1, loads[0]);
	}

	@Test
	void tracksAndPersistsSharedCooldowns() {
		UUID playerId = UUID.randomUUID();
		Map<UUID, Map<String, Long>> persisted = new HashMap<>();
		AbilityCooldownTracker tracker = new AbilityCooldownTracker(id -> new HashMap<>(),
				(id, values) -> persisted.put(id, new HashMap<>(values)));

		tracker.start(playerId, "ultimate", 5_000L);
		tracker.startAndPersist(playerId, AbilityCooldownTracker.ULTIMATE_GROUP, 10_000L);

		assertTrue(tracker.active(playerId, "ultimate", 4_999L));
		assertFalse(tracker.active(playerId, "ultimate", 5_000L));
		assertEquals(10_000L, persisted.get(playerId).get(AbilityCooldownTracker.ULTIMATE_GROUP));
	}
}
