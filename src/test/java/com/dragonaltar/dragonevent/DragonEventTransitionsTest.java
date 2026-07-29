package com.dragonaltar.dragonevent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DragonEventTransitionsTest {
    @Test void officialHappyPathIsAllowed(){
        DragonEventState[] path={DragonEventState.NOT_STARTED,DragonEventState.PREPARING,DragonEventState.SUMMONING,DragonEventState.ACTIVE,DragonEventState.DEATH_SEQUENCE,DragonEventState.DEFEATED,DragonEventState.ALTAR_AWAKENING,DragonEventState.ALTAR_ACTIVE,DragonEventState.COMPLETED};
        for(int i=1;i<path.length;i++)assertTrue(DragonEventTransitions.allows(path[i-1],path[i]));
    }
    @Test void duplicateOrBackwardProgressionIsRejected(){
        assertFalse(DragonEventTransitions.allows(DragonEventState.ACTIVE,DragonEventState.SUMMONING));
        assertFalse(DragonEventTransitions.allows(DragonEventState.COMPLETED,DragonEventState.NOT_STARTED));
        assertThrows(IllegalStateException.class,()->DragonEventTransitions.require(DragonEventState.DEFEATED,DragonEventState.ACTIVE));
    }
}
