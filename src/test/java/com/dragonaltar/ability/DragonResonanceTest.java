package com.dragonaltar.ability;

import com.dragonaltar.soul.SoulIdentity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DragonResonanceTest {
	@Test
	void everySoulPairHasItsOwnResonance() {
		assertEquals(DragonResonance.THERMAL_CONVERGENCE,
				DragonResonance.pair(SoulIdentity.AKUMA, SoulIdentity.REV).orElseThrow());
		assertEquals(DragonResonance.VOLCANIC_AEGIS,
				DragonResonance.pair(SoulIdentity.REV, SoulIdentity.LAMARI).orElseThrow());
		assertEquals(DragonResonance.GLACIAL_BASTION,
				DragonResonance.pair(SoulIdentity.AKUMA, SoulIdentity.LAMARI).orElseThrow());
		assertTrue(DragonResonance.pair(SoulIdentity.AKUMA, SoulIdentity.AKUMA).isEmpty());
	}

	@Test
	void trinityRequiresAllThreeSoulIdentities() {
		assertEquals(3, DragonResonance.DRAGON_TRINITY.souls().size());
		assertTrue(DragonResonance.DRAGON_TRINITY.souls()
				.containsAll(java.util.Set.of(SoulIdentity.AKUMA, SoulIdentity.REV, SoulIdentity.LAMARI)));
	}
}
