package com.dragonaltar.soul;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SoulIdentityTest {
	@Test
	void mapsInternalIdsToPublicNames() {
		assertEquals("Akuma", SoulIdentity.displayName("soul-1"));
		assertEquals("Rev", SoulIdentity.displayName("soul-2"));
		assertEquals("Lamari", SoulIdentity.displayName("soul-3"));
	}

	@Test
	void acceptsPublicNamesAndCanonicalIdsForDevelopmentCommands() {
		assertEquals(SoulIdentity.AKUMA, SoulIdentity.fromInput("Akuma"));
		assertEquals(SoulIdentity.REV, SoulIdentity.fromInput("rev"));
		assertEquals(SoulIdentity.LAMARI, SoulIdentity.fromInput("soul-3"));
		assertThrows(IllegalArgumentException.class, () -> SoulIdentity.fromInput("unknown"));
	}

	@Test
	void replacesIdsInsidePersistedHistoryText() {
		assertEquals("time|GAIN|Rev", SoulIdentity.replaceIds("time|GAIN|soul-2"));
		assertFalse(SoulIdentity.replaceIds("soul-1 soul-2 soul-3").contains("soul-"));
	}
}
