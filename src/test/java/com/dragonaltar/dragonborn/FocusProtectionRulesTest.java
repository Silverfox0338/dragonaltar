package com.dragonaltar.dragonborn;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class FocusProtectionRulesTest {
	private static final List<String> BLOCKED = List.of("ah sell", "auctionhouse sell", "sell");

	@Test
	void blocksConfiguredSaleCommandsCaseInsensitively() {
		assertTrue(FocusProtectionRules.blocksCommand("/AH   SELL 25", BLOCKED));
		assertTrue(FocusProtectionRules.blocksCommand("auctionhouse sell", BLOCKED));
		assertTrue(FocusProtectionRules.blocksCommand("/auction-plugin:ah sell 25", BLOCKED));
		assertTrue(FocusProtectionRules.blocksCommand("/sell hand", BLOCKED));
	}

	@Test
	void doesNotBlockUnrelatedOrPrefixLookalikeCommands() {
		assertFalse(FocusProtectionRules.blocksCommand("/ah browse", BLOCKED));
		assertFalse(FocusProtectionRules.blocksCommand("/seller info", BLOCKED));
		assertFalse(FocusProtectionRules.blocksCommand("/dragon abilities", BLOCKED));
	}
}
