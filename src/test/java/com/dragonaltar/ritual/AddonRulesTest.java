package com.dragonaltar.ritual;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddonRulesTest {
    @Test void eachDragonbornCallerAddsTwentyFivePercent() {
        assertEquals(0, AddonRules.backfireChancePercent(0));
        assertEquals(25, AddonRules.backfireChancePercent(1));
        assertEquals(50, AddonRules.backfireChancePercent(2));
        assertEquals(50, AddonRules.backfireChancePercent(3));
        assertEquals(50, AddonRules.backfireChancePercent(9));
    }

    @Test void twoCallersTriggerTheThreeSoulBlackoutRule() {
        assertFalse(AddonRules.totalBlackoutBackfire(0));
        assertFalse(AddonRules.totalBlackoutBackfire(1));
        assertTrue(AddonRules.totalBlackoutBackfire(2));
    }

    @Test void instabilityStartsOnlyPastThreshold() {
        assertFalse(AddonRules.instabilityActive(6, 6));
        assertTrue(AddonRules.instabilityActive(7, 6));
    }
}
