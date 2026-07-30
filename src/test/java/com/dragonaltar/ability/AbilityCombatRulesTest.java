package com.dragonaltar.ability;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AbilityCombatRulesTest {
    @Test
    void brittleShattersOnlyOnceOnPositiveDamage() {
        var tracker = new AbilityCombatRules.BrittleTracker();
        UUID target = UUID.randomUUID(), caster = UUID.randomUUID();
        tracker.apply(target, caster, 2_000);

        assertTrue(tracker.consume(target, 1_000, 0).isEmpty());
        assertEquals(caster, tracker.consume(target, 1_000, 1).orElseThrow().caster());
        assertTrue(tracker.consume(target, 1_001, 1).isEmpty());
    }

    @Test
    void expiredBrittleCannotShatter() {
        var tracker = new AbilityCombatRules.BrittleTracker();
        UUID target = UUID.randomUUID();
        tracker.apply(target, UUID.randomUUID(), 1_000);
        assertTrue(tracker.consume(target, 1_000, 5).isEmpty());
        assertFalse(tracker.active(target, 1_000));
    }

    @Test
    void brittleExtensionHonorsItsRemainingTimeCap() {
        var tracker = new AbilityCombatRules.BrittleTracker();
        UUID target = UUID.randomUUID();
        tracker.apply(target, UUID.randomUUID(), 2_000);
        tracker.extend(target, 1_000, 5_000, 3_000);
        assertTrue(tracker.active(target, 2_999));
        assertFalse(tracker.active(target, 3_000));
    }

    @Test
    void bulwarkStoresConfiguredPreventedFractionWithHardCap() {
        assertEquals(3.5, AbilityCombatRules.storedBulwarkDamage(0, 10, .35, 16), .0001);
        assertEquals(16, AbilityCombatRules.storedBulwarkDamage(15, 10, .35, 16), .0001);
        assertEquals(0, AbilityCombatRules.storedBulwarkDamage(0, -10, .35, 16), .0001);
        assertEquals(6, AbilityCombatRules.scaledBulwarkValue(16, 16, 1, 6), .0001);
        assertEquals(3.5, AbilityCombatRules.scaledBulwarkValue(8, 16, 1, 6), .0001);
    }

    @Test
    void reflectionGuardRejectsRecursiveEntry() {
        var guard = new AbilityCombatRules.ReflectionGuard();
        assertTrue(guard.enter());
        assertTrue(guard.active());
        assertFalse(guard.enter());
        guard.exit();
        assertTrue(guard.enter());
    }

    @Test
    void revHeatHonorsCapTargetCooldownAndPerTargetLimit() {
        var tracker = new AbilityCombatRules.RevHuntTracker();
        UUID rev = UUID.randomUUID(), target = UUID.randomUUID();
        assertEquals(4, tracker.gainHeat(rev,target,1_000,4,10,500,2).heat());
        assertFalse(tracker.gainHeat(rev,target,1_200,4,10,500,2).granted());
        assertEquals(10,tracker.gainHeat(rev,target,1_500,8,10,500,2).heat());
        assertTrue(tracker.finisherArmed(rev));
        assertFalse(tracker.gainHeat(rev,target,2_000,4,10,500,2).granted());
    }

    @Test
    void revHeatDecaysOnlyAfterDelayAndInConfiguredSteps() {
        var tracker = new AbilityCombatRules.RevHuntTracker();
        UUID rev=UUID.randomUUID();
        tracker.gainHeat(rev,UUID.randomUUID(),1_000,10,20,0,5);
        assertEquals(10,tracker.decayHeat(rev,3_999,2_000,1_000,2));
        assertEquals(8,tracker.decayHeat(rev,4_000,2_000,1_000,2));
        assertEquals(4,tracker.decayHeat(rev,6_000,2_000,1_000,2));
    }

    @Test
    void infernoMarksRefreshWithinCapAndRespectCountLimit() {
        var tracker=new AbilityCombatRules.RevHuntTracker();
        UUID rev=UUID.randomUUID(), first=UUID.randomUUID(), second=UUID.randomUUID();
        assertTrue(tracker.mark(first,rev,1_000,2_000,3_000,1));
        assertTrue(tracker.mark(first,rev,2_000,2_000,3_000,1));
        assertFalse(tracker.mark(second,rev,2_000,2_000,3_000,1));
        assertTrue(tracker.mark(first,rev,3_500,2_000,3_000,1));
        assertTrue(tracker.markedBy(first,rev,3_999));
        assertFalse(tracker.markedBy(first,rev,4_000));
    }

    @Test
    void rampageCountsDistinctTargetsUpToPerTargetLimit() {
        var tracker=new AbilityCombatRules.RevHuntTracker();
        UUID rev=UUID.randomUUID(), first=UUID.randomUUID(), second=UUID.randomUUID();
        tracker.beginHunt(rev,1_000,10_000,2_000,5_000);
        assertEquals(1,tracker.gainRampage(rev,first,2_000,1,3,1).progress());
        assertFalse(tracker.gainRampage(rev,first,2_100,1,3,1).granted());
        assertEquals(2,tracker.gainRampage(rev,second,2_200,1,3,1).progress());
    }

    @Test
    void rendRecastExpiresAndConsumesOnlyOnce() {
        var tracker=new AbilityCombatRules.RevHuntTracker();
        UUID rev=UUID.randomUUID();
        tracker.armRecast(rev,2_000);
        assertTrue(tracker.consumeRecast(rev,1_999));
        assertFalse(tracker.consumeRecast(rev,1_999));
        tracker.armRecast(rev,3_000);
        assertFalse(tracker.consumeRecast(rev,3_000));
    }

    @Test
    void finisherRequiresValidClaimAndConsumesOnlyOnce() {
        var tracker=new AbilityCombatRules.RevHuntTracker();
        UUID rev=UUID.randomUUID();
        tracker.gainHeat(rev,UUID.randomUUID(),1_000,10,10,0,1);
        assertFalse(tracker.consumeFinisher(rev,false,true));
        assertTrue(tracker.finisherArmed(rev));
        assertTrue(tracker.consumeFinisher(rev,true,true));
        assertEquals(0,tracker.heat(rev));
        assertFalse(tracker.consumeFinisher(rev,true,true));
    }

    @Test
    void armedFinisherExpiresWithoutFiring() {
        var tracker=new AbilityCombatRules.RevHuntTracker();
        UUID rev=UUID.randomUUID();
        tracker.armFinisher(rev,2_000);
        assertTrue(tracker.finisherArmed(rev,1_999));
        assertFalse(tracker.finisherArmed(rev,2_000));
        assertFalse(tracker.consumeFinisher(rev,2_000,true,true));
    }

    @Test
    void scriptedFireAndReflectionDamageCannotDriveHuntProgress() {
        assertTrue(AbilityCombatRules.RevHuntTracker.acceptsDamage(AbilityCombatRules.CombatDamageSource.DIRECT_PLAYER));
        assertTrue(AbilityCombatRules.RevHuntTracker.acceptsDamage(AbilityCombatRules.CombatDamageSource.PLAYER_PROJECTILE));
        assertFalse(AbilityCombatRules.RevHuntTracker.acceptsDamage(AbilityCombatRules.CombatDamageSource.FIRE_TICK));
        assertFalse(AbilityCombatRules.RevHuntTracker.acceptsDamage(AbilityCombatRules.CombatDamageSource.SCRIPTED_ABILITY));
        assertFalse(AbilityCombatRules.RevHuntTracker.acceptsDamage(AbilityCombatRules.CombatDamageSource.REFLECTION));
    }

    @Test
    void resetClearsHeatMarksRecastHuntAndFinisher() {
        var tracker=new AbilityCombatRules.RevHuntTracker();
        UUID rev=UUID.randomUUID(), target=UUID.randomUUID();
        tracker.gainHeat(rev,target,1_000,10,10,0,1);
        tracker.mark(target,rev,1_000,10_000,10_000,3);
        tracker.armRecast(rev,10_000);
        tracker.beginHunt(rev,1_000,10_000,1_000,3_000);
        tracker.reset(rev);
        assertEquals(0,tracker.heat(rev));
        assertEquals(0,tracker.activeMarks(rev,2_000));
        assertFalse(tracker.recastAvailable(rev,2_000));
        assertFalse(tracker.huntActive(rev,2_000));
        assertFalse(tracker.finisherArmed(rev));
    }

    @Test
    void runtimeTargetingHasHardRadiusAndEntityCaps() {
        assertEquals(0,AbilityService.boundedTargetRadius(-1));
        assertEquals(64,AbilityService.boundedTargetRadius(10_000));
        assertEquals(0,AbilityService.boundedTargetRadius(Double.NaN));
        assertEquals(128,AbilityService.maximumTargetsPerQuery());
    }
}
