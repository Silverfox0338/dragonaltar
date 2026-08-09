package com.dragonaltar.dragonevent;

import java.util.*;

public final class DragonEventTransitions {
	private static final Map<DragonEventState, Set<DragonEventState>> ALLOWED = Map.ofEntries(
			Map.entry(DragonEventState.NOT_STARTED, EnumSet.of(DragonEventState.PREPARING)),
			Map.entry(DragonEventState.PREPARING,
					EnumSet.of(DragonEventState.SUMMONING, DragonEventState.ABORTED, DragonEventState.NOT_STARTED,
							DragonEventState.RECOVERY_REQUIRED)),
			Map.entry(DragonEventState.SUMMONING,
					EnumSet.of(DragonEventState.ACTIVE, DragonEventState.ABORTED, DragonEventState.RECOVERY_REQUIRED)),
			Map.entry(DragonEventState.ACTIVE,
					EnumSet.of(DragonEventState.DEATH_SEQUENCE, DragonEventState.ABORTED,
							DragonEventState.RECOVERY_REQUIRED)),
			Map.entry(DragonEventState.DEATH_SEQUENCE,
					EnumSet.of(DragonEventState.DEFEATED, DragonEventState.RECOVERY_REQUIRED)),
			Map.entry(DragonEventState.DEFEATED,
					EnumSet.of(DragonEventState.ALTAR_AWAKENING, DragonEventState.ALTAR_ACTIVE,
							DragonEventState.RECOVERY_REQUIRED)),
			Map.entry(DragonEventState.ALTAR_AWAKENING,
					EnumSet.of(DragonEventState.ALTAR_ACTIVE, DragonEventState.RECOVERY_REQUIRED)),
			Map.entry(DragonEventState.ALTAR_ACTIVE,
					EnumSet.of(DragonEventState.COMPLETED, DragonEventState.RECOVERY_REQUIRED)),
			Map.entry(DragonEventState.ABORTED, EnumSet.of(DragonEventState.RECOVERY_REQUIRED)),
			Map.entry(DragonEventState.RECOVERY_REQUIRED, EnumSet.of(DragonEventState.SUMMONING,
					DragonEventState.ACTIVE, DragonEventState.ALTAR_ACTIVE, DragonEventState.ABORTED)));
	private DragonEventTransitions() {
	}
	public static boolean allows(DragonEventState from, DragonEventState to) {
		return from == to || ALLOWED.getOrDefault(from, Set.of()).contains(to);
	}
	public static void require(DragonEventState from, DragonEventState to) {
		if (!allows(from, to))
			throw new IllegalStateException("Invalid event transition: " + from + " -> " + to);
	}
}
