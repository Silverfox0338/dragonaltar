package com.dragonaltar.ability.rev;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class RevHeatBarManagerTest {
	@Test
	void progressIsBounded() {
		assertEquals(0f, RevHeatBarManager.progress(-5, 100));
		assertEquals(.5f, RevHeatBarManager.progress(50, 100));
		assertEquals(1f, RevHeatBarManager.progress(150, 100));
		assertEquals(0f, RevHeatBarManager.progress(50, 0));
	}

	@Test
	void tiersFollowConfiguredThresholds() {
		assertEquals(RevHeatBarManager.HeatTier.STALKING, RevHeatBarManager.tier(34, 35, 65));
		assertEquals(RevHeatBarManager.HeatTier.PURSUING, RevHeatBarManager.tier(35, 35, 65));
		assertEquals(RevHeatBarManager.HeatTier.PREDATOR, RevHeatBarManager.tier(65, 35, 65));
	}
}
