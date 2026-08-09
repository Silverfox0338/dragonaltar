package com.dragonaltar.api.model;

import java.util.Map;

/** Immutable eligibility result suitable for add-ons. */
public record DragonEligibilityInfo(boolean eligible, Map<String, Boolean> checks) {
	public DragonEligibilityInfo {
		checks = Map.copyOf(checks);
	}
}
