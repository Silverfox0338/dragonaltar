package com.dragonaltar.ability;

import com.dragonaltar.ability.akuma.AkumasTrail;
import com.dragonaltar.ability.shared.Roar;
import com.dragonaltar.ability.shared.Wings;
import com.dragonaltar.soul.SoulIdentity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ConfiguredAbilityTest {
	@Test
	void sharedAbilitiesSupportEveryDragonbornSoul() {
		DragonAbility wings = new Wings(null);
		DragonAbility roar = new Roar(null);

		for (SoulIdentity soul : SoulIdentity.values()) {
			assertTrue(wings.supports(soul), "Wings should support " + soul);
			assertTrue(roar.supports(soul), "Roar should support " + soul);
		}
	}

	@Test
	void soulAbilitiesRemainRestrictedToTheirKit() {
		DragonAbility trail = new AkumasTrail(null);

		assertTrue(trail.supports(SoulIdentity.AKUMA));
		assertFalse(trail.supports(SoulIdentity.REV));
		assertFalse(trail.supports(SoulIdentity.LAMARI));
	}
}
