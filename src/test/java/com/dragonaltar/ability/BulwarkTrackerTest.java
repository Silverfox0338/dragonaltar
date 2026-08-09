package com.dragonaltar.ability;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BulwarkTrackerTest {
	@Test
	void activationAndDeactivationOwnChargeLifecycle() {
		BulwarkTracker tracker = new BulwarkTracker();
		UUID playerId = UUID.randomUUID();

		tracker.activate(playerId);
		tracker.charge(playerId, 7.5);

		assertTrue(tracker.active(playerId));
		assertEquals(7.5, tracker.deactivate(playerId));
		assertFalse(tracker.active(playerId));

		tracker.remove(playerId);
		assertEquals(0, tracker.charge(playerId));
	}
}
