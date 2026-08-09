package com.dragonaltar.eligibility;
import java.util.*;
public final class EligibilityRules {
	private EligibilityRules() {
	}
	public static EligibilityService.Result evaluate(EligibilitySnapshot s) {
		Map<String, Boolean> checks = new LinkedHashMap<>();
		checks.put("online", s.online());
		checks.put("game-mode", s.gameMode());
		checks.put("minimum-playtime", s.playtime());
		checks.put("required-permission", s.requiredPermission());
		checks.put("not-excluded", s.notExcluded());
		checks.put("not-dragonborn", s.notDragonborn());
		checks.put("not-afk", s.notAfk());
		checks.put("not-vanished", s.notVanished());
		checks.put("alive", s.alive());
		checks.put("join-grace", s.joinGrace());
		return new EligibilityService.Result(checks.values().stream().allMatch(Boolean::booleanValue), checks);
	}
}
