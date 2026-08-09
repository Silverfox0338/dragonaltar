package com.dragonaltar.persistence;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CooldownCodecTest {
	@Test
	void roundTripPreservesValidCooldownsAndRejectsInvalidValues() {
		Map<String, Long> original = Map.of("wings", 1234L, "dash", 5678L, "_shared_ultimate", 120_000L,
				"_shared_resonance", 720_000L);
		assertEquals(original, CooldownCodec.decode(CooldownCodec.encode(original)));
		Map<Object, Object> bad = new HashMap<>();
		bad.put("negative", -1L);
		bad.put("text", "x");
		bad.put(4, 5L);
		assertTrue(CooldownCodec.decode(bad).isEmpty());
	}
}
