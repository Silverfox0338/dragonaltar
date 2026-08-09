package com.dragonaltar.ability;

import com.dragonaltar.soul.SoulIdentity;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public enum DragonResonance {
	THERMAL_CONVERGENCE("thermal-convergence", "Thermal Convergence",
			Set.of(SoulIdentity.AKUMA, SoulIdentity.REV)), VOLCANIC_AEGIS("volcanic-aegis", "Volcanic Aegis",
					Set.of(SoulIdentity.REV, SoulIdentity.LAMARI)), GLACIAL_BASTION("glacial-bastion",
							"Glacial Bastion", Set.of(SoulIdentity.AKUMA, SoulIdentity.LAMARI)), DRAGON_TRINITY(
									"dragon-trinity", "Dragon Trinity",
									Set.of(SoulIdentity.AKUMA, SoulIdentity.REV, SoulIdentity.LAMARI));

	private final String id;
	private final String displayName;
	private final Set<SoulIdentity> souls;

	DragonResonance(String id, String displayName, Set<SoulIdentity> souls) {
		this.id = id;
		this.displayName = displayName;
		this.souls = souls;
	}

	public String id() {
		return id;
	}
	public String displayName() {
		return displayName;
	}
	public Set<SoulIdentity> souls() {
		return souls;
	}

	public static Optional<DragonResonance> pair(SoulIdentity first, SoulIdentity second) {
		if (first == second)
			return Optional.empty();
		Set<SoulIdentity> pair = Set.of(first, second);
		return Arrays.stream(values()).filter(value -> value.souls.size() == 2 && value.souls.equals(pair)).findFirst();
	}
}
