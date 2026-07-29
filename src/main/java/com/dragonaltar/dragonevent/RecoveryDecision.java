package com.dragonaltar.dragonevent;
public enum RecoveryDecision {
    RESTORE_ALTAR, RESTORE_ACTIVE_DRAGON, RESUME_SUMMONING, REQUIRE_MANUAL_REPAIR, ABORT;
    public static RecoveryDecision decide(int souls,int matchingDragons,int sessionCrystals){
        if(souls>0)return RESTORE_ALTAR;
        if(matchingDragons==1)return RESTORE_ACTIVE_DRAGON;
        if(matchingDragons>1)return REQUIRE_MANUAL_REPAIR;
        if(sessionCrystals==4)return RESUME_SUMMONING;
        if(sessionCrystals>0)return REQUIRE_MANUAL_REPAIR;
        return ABORT;
    }
}
