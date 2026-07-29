package com.dragonaltar.dragonevent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RecoveryDecisionTest {
    @Test void prioritizesPersistentOwnershipAndNeverInventsDragon(){assertEquals(RecoveryDecision.RESTORE_ALTAR,RecoveryDecision.decide(3,0,0));assertEquals(RecoveryDecision.RESTORE_ACTIVE_DRAGON,RecoveryDecision.decide(0,1,0));assertEquals(RecoveryDecision.REQUIRE_MANUAL_REPAIR,RecoveryDecision.decide(0,2,0));assertEquals(RecoveryDecision.RESUME_SUMMONING,RecoveryDecision.decide(0,0,4));assertEquals(RecoveryDecision.REQUIRE_MANUAL_REPAIR,RecoveryDecision.decide(0,0,2));assertEquals(RecoveryDecision.ABORT,RecoveryDecision.decide(0,0,0));}
}
