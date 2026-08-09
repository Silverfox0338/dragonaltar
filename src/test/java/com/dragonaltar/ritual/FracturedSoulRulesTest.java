package com.dragonaltar.ritual;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class FracturedSoulRulesTest {
	@Test
	void unloadedTrackedEntityWaitsInsteadOfSpawningReplacement() {
		assertTrue(FracturedSoulRules.waitForTrackedChunk(UUID.randomUUID(), false, false));
		assertFalse(FracturedSoulRules.waitForTrackedChunk(UUID.randomUUID(), false, true));
		assertFalse(FracturedSoulRules.waitForTrackedChunk(null, false, false));
	}

	@Test
	void persistedEntityRemainsCanonicalAmongDuplicates() {
		UUID tracked = UUID.randomUUID();
		UUID duplicate = UUID.randomUUID();
		assertEquals(tracked, FracturedSoulRules.canonical(tracked, List.of(duplicate, tracked)));
	}
}
