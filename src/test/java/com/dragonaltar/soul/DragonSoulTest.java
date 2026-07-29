package com.dragonaltar.soul;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DragonSoulTest {
    @Test void onlyCanonicalIdsAreAccepted() {
        assertThrows(IllegalArgumentException.class,()->new DragonSoul("soul-4",DragonSoulState.UNCLAIMED,null,null,Instant.now(),null,0,0,List.of()));
    }
    @Test void reservationAndAssignmentAreAtomicDomainTransitions() {
        UUID player=UUID.randomUUID();DragonSoul soul=DragonSoul.unclaimed("soul-1");
        soul.reserve(player);assertEquals(DragonSoulState.RITUAL_RESERVED,soul.state());assertEquals(player,soul.reservedFor());
        soul.assign(player,"RITUAL");assertEquals(DragonSoulState.HELD,soul.state());assertEquals(player,soul.holder());assertNull(soul.reservedFor());
        assertEquals(1,soul.lineage().size());
    }
    @Test void transferPreservesIdentityAndBuildsLineage() {
        UUID first=UUID.randomUUID(),second=UUID.randomUUID();DragonSoul soul=DragonSoul.unclaimed("soul-2");
        soul.assign(first,"CLAIM");soul.assign(second,"PVP");
        assertEquals("soul-2",soul.id());assertEquals(second,soul.holder());assertEquals(1,soul.generation());assertEquals(1,soul.transferCount());assertEquals(2,soul.lineage().size());
    }
    @Test void pendingNeverDestroysSoul() {
        DragonSoul soul=DragonSoul.unclaimed("soul-3");soul.assign(UUID.randomUUID(),"CLAIM");soul.pending("NATURAL");
        assertEquals(DragonSoulState.TRANSFER_PENDING,soul.state());assertNull(soul.holder());assertFalse(soul.lineage().isEmpty());
    }
    @Test void interruptedReservationWithoutPlayerRepairsToUnclaimed(){
        DragonSoul soul=new DragonSoul("soul-1",DragonSoulState.RITUAL_RESERVED,null,null,Instant.now(),null,0,0,List.of());
        assertTrue(soul.repair());assertEquals(DragonSoulState.UNCLAIMED,soul.state());assertFalse(soul.lineage().isEmpty());
    }
    @Test void interruptedTransferWithoutHolderRepairsToPending(){
        DragonSoul soul=new DragonSoul("soul-1",DragonSoulState.TRANSFER_ANIMATING,null,null,Instant.now(),null,1,1,List.of());
        assertTrue(soul.repair());assertEquals(DragonSoulState.TRANSFER_PENDING,soul.state());assertNull(soul.holder());
    }
    @Test void fracturedAndLimboStatesCannotLookHeldOrPending(){
        DragonSoul soul=DragonSoul.unclaimed("soul-1");UUID holder=UUID.randomUUID();soul.assign(holder,"TEST");
        soul.fracture("TEST");assertEquals(DragonSoulState.FRACTURED,soul.state());assertNull(soul.holder());
        soul.assign(holder,"CLAIM");soul.limbo("TEST");assertEquals(DragonSoulState.MOTHER_SOUL_LIMBO,soul.state());assertNull(soul.holder());
    }
}
