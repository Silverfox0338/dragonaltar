package com.dragonaltar.ability;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResonanceStateTest {
    @Test
    void ownsUnlockAndWardLifecycle() {
        ResonanceState state = new ResonanceState();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        state.register("glacial-bastion");
        state.setUnlocked(first, "glacial-bastion");
        state.setUnlocked(second, "glacial-bastion");
        state.grantWard(first, 2);

        assertTrue(state.isResonance("glacial-bastion"));
        assertEquals(1, state.consumeWard(first));
        assertEquals(0, state.consumeWard(first));

        state.retainUnlocked(Set.of(first));
        assertEquals("glacial-bastion", state.unlocked(first));
        assertNull(state.unlocked(second));
    }

    @Test
    void retaliationClaimEnforcesItsCooldownWindow() {
        ResonanceState state = new ResonanceState();
        UUID pair = UUID.randomUUID();

        assertTrue(state.claimRetaliation(pair, 100L, 200L));
        assertFalse(state.claimRetaliation(pair, 199L, 300L));
        assertTrue(state.claimRetaliation(pair, 200L, 300L));
    }
}
