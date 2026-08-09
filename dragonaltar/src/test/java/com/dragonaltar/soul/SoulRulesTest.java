package com.dragonaltar.soul;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SoulRulesTest {
	@Test
	void hardCodesThreeAsTheMaximum() {
		assertEquals(3, SoulIdentity.MAX_DRAGONBORN);
		assertEquals(Set.of("soul-1", "soul-2", "soul-3"), SoulIdentity.CANONICAL_IDS);
	}
	@Test
	void rejectsMoreThanThree() {
		List<DragonSoul> list = new ArrayList<>();
		list.add(DragonSoul.unclaimed("soul-1"));
		list.add(DragonSoul.unclaimed("soul-2"));
		list.add(DragonSoul.unclaimed("soul-3"));
		list.add(DragonSoul.unclaimed("soul-1"));
		assertThrows(IllegalStateException.class, () -> SoulRules.validate(list));
	}
	@Test
	void rejectsOnePlayerHoldingTwoSouls() {
		UUID player = UUID.randomUUID();
		DragonSoul a = DragonSoul.unclaimed("soul-1"), b = DragonSoul.unclaimed("soul-2");
		a.assign(player, "TEST");
		b.assign(player, "TEST");
		assertThrows(IllegalStateException.class, () -> SoulRules.validate(List.of(a, b)));
	}
	@Test
	void acceptsThreeUniqueSoulHolders() {
		DragonSoul a = DragonSoul.unclaimed("soul-1"), b = DragonSoul.unclaimed("soul-2"),
				c = DragonSoul.unclaimed("soul-3");
		a.assign(UUID.randomUUID(), "TEST");
		b.assign(UUID.randomUUID(), "TEST");
		c.assign(UUID.randomUUID(), "TEST");
		assertDoesNotThrow(() -> SoulRules.validate(List.of(a, b, c)));
	}
}
