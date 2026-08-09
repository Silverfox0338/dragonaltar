package com.dragonaltar.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dragonaltar.api.addon.DragonAddonAbility;
import com.dragonaltar.api.addon.DragonAltarAddon;
import com.dragonaltar.api.model.DragonActionResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

class AddonInputValidatorTest {
	private final AddonInputValidator validator = new AddonInputValidator();
	private final DragonAltarAddon addon = new DragonAltarAddon("example", "Example", "1.0.0", "Owner", "");

	@Test
	void acceptsBoundedNamespacedAbility() {
		assertDoesNotThrow(() -> validator.validateAbility(addon,
				ability("example:frost-step", 25, Duration.ofSeconds(10).toMillis(), Set.of("Akuma")), 100));
	}

	@Test
	void rejectsNamespaceEnergyCooldownAndSoulBoundaryBypasses() {
		assertThrows(IllegalArgumentException.class,
				() -> validator.validateAbility(addon, ability("other:frost-step", 25, 1, Set.of("Akuma")), 100));
		assertThrows(IllegalArgumentException.class,
				() -> validator.validateAbility(addon, ability("example:frost-step", 101, 1, Set.of("Akuma")), 100));
		assertThrows(IllegalArgumentException.class, () -> validator.validateAbility(addon,
				ability("example:frost-step", 25, Duration.ofHours(24).plusMillis(1).toMillis(), Set.of("Akuma")),
				100));
		assertThrows(IllegalArgumentException.class,
				() -> validator.validateAbility(addon, ability("example:frost-step", 25, 1, Set.of("unknown")), 100));
	}

	@Test
	void reservesCoreNamespaceAndRequiresCanonicalMarkerSyntax() {
		assertThrows(IllegalArgumentException.class, () -> validator
				.validateAddon(new DragonAltarAddon("dragonaltar", "Core impersonator", "1", "Owner", "")));
		assertDoesNotThrow(() -> validator.validateAddon(addon));
		org.junit.jupiter.api.Assertions.assertTrue(validator.validNamespacedItemId("example:frost-vestment"));
		org.junit.jupiter.api.Assertions.assertFalse(validator.validNamespacedItemId("example:Frost"));
	}

	private static DragonAddonAbility ability(String id, int energy, long cooldown, Set<String> souls) {
		return new DragonAddonAbility() {
			@Override
			public String id() {
				return id;
			}

			@Override
			public String displayName() {
				return "Frost Step";
			}

			@Override
			public Category category() {
				return Category.MOVEMENT;
			}

			@Override
			public int energyCost() {
				return energy;
			}

			@Override
			public long cooldownMillis() {
				return cooldown;
			}

			@Override
			public Set<String> supportedSouls() {
				return souls;
			}

			@Override
			public DragonActionResult activate(Context context) {
				return DragonActionResult.ok();
			}
		};
	}
}
