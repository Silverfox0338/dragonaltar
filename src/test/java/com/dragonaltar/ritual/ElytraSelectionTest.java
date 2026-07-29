package com.dragonaltar.ritual;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class ElytraSelectionTest {
    private final List<ElytraSelection.Candidate> candidates=List.of(
            new ElytraSelection.Candidate(7,100,3),
            new ElytraSelection.Candidate(2,300,5),
            new ElytraSelection.Candidate(9,10,0));
    @Test void mostDamagedUsesLeastDurabilityRemaining(){assertEquals(2,ElytraSelection.select(candidates,ElytraSelection.Policy.MOST_DAMAGED).slot());}
    @Test void leastDamagedUsesLowestDamage(){assertEquals(9,ElytraSelection.select(candidates,ElytraSelection.Policy.LEAST_DAMAGED).slot());}
    @Test void firstMatchUsesLowestInventorySlot(){assertEquals(2,ElytraSelection.select(candidates,ElytraSelection.Policy.FIRST_MATCH).slot());}
    @Test void lowestEnchantmentValueWins(){assertEquals(9,ElytraSelection.select(candidates,ElytraSelection.Policy.LOWEST_ENCHANTMENT_VALUE).slot());}
}
