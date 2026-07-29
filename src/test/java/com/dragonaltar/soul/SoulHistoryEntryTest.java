package com.dragonaltar.soul;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class SoulHistoryEntryTest {
    @Test
    void parsesLegacyLineageWithoutMigration() {
        UUID oldHolder = UUID.randomUUID();
        UUID newHolder = UUID.randomUUID();
        SoulHistoryEntry entry = SoulHistoryEntry.parse(
                "2026-07-29T05:14:00Z|" + oldHolder + "|" + newHolder + "|PVP_INHERITANCE");

        assertNotNull(entry);
        assertEquals(oldHolder, entry.fromPlayer());
        assertEquals(newHolder, entry.toPlayer());
        assertEquals("PvP", entry.transferType());
        assertTrue(entry.callers().isEmpty());
        assertNull(entry.killer());
    }

    @Test
    void parsesMotherSoulCallerMetadata() {
        UUID callerA = UUID.randomUUID();
        UUID callerB = UUID.randomUUID();
        SoulHistoryEntry entry = SoulHistoryEntry.parse(
                "2026-07-29T05:14:00Z|-|MOTHER_SOUL_LIMBO|DRAGONBORN_CALLER_BACKFIRE;callers="
                        + callerA + "," + callerB);

        assertNotNull(entry);
        assertEquals("Mother Soul", entry.transferType());
        assertEquals("DRAGONBORN_CALLER_BACKFIRE", entry.reason());
        assertEquals(java.util.List.of(callerA, callerB), entry.callers());
    }

    @Test
    void parsesARelevantKiller() {
        UUID victim = UUID.randomUUID();
        UUID killer = UUID.randomUUID();
        SoulHistoryEntry entry = SoulHistoryEntry.parse(
                "2026-07-29T05:14:00Z|" + victim + "|DISABLED|DRAGONBORN_KILLER_DORMANT;killer=" + killer);

        assertNotNull(entry);
        assertEquals(killer, entry.killer());
        assertEquals("Became dormant after its holder was slain", entry.description());
    }

    @Test
    void retainsPrivateAdminProvenanceButPublicTextIsNeutral() {
        UUID oldHolder = UUID.randomUUID();
        UUID newHolder = UUID.randomUUID();
        UUID admin = UUID.randomUUID();
        SoulHistoryEntry entry = SoulHistoryEntry.parse(
                "2026-07-29T05:14:00Z|" + oldHolder + "|" + newHolder
                        + "|ADMIN_FORCE_TRANSFER;admin=" + admin);

        assertNotNull(entry);
        assertEquals(admin, entry.adminActor());
        assertEquals("Soul transfer", entry.transferType());
        assertEquals("Passed to a new holder", entry.description());
        assertFalse(entry.transferType().toLowerCase().contains("admin"));
        assertFalse(entry.description().toLowerCase().contains("staff"));
    }

    @Test
    void ignoresMalformedHistorySafely() {
        assertNull(SoulHistoryEntry.parse("not-a-history-entry"));
        assertNull(SoulHistoryEntry.parse(null));
    }
}
