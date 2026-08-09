package com.dragonaltar.ritual;

import com.dragonaltar.soul.SoulIdentity;

public final class AddonRules {
	private AddonRules() {
	}

	public static int backfireChancePercent(int dragonbornCallers) {
		int maximumNonTargetCallers = SoulIdentity.MAX_DRAGONBORN - 1;
		return Math.max(0, Math.min(maximumNonTargetCallers, dragonbornCallers) * 25);
	}

	public static boolean totalBlackoutBackfire(int dragonbornCallers) {
		return dragonbornCallers >= SoulIdentity.MAX_DRAGONBORN - 1;
	}

	public static boolean instabilityActive(long casts, long threshold) {
		return casts > Math.max(0, threshold);
	}
}
