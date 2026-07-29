package com.dragonaltar.ritual;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class OfferingPlanTest {
    @Test void inventoryModeRequiresInventory(){OfferingPlan plan=OfferingPlan.create(OfferingMode.INVENTORY_CONSUME,Map.of("NETHER_STAR",2),Map.of("NETHER_STAR",2),Map.of());assertEquals(2,plan.inventory().get("NETHER_STAR"));assertEquals(2,plan.refund().get("NETHER_STAR"));assertThrows(IllegalStateException.class,()->OfferingPlan.create(OfferingMode.INVENTORY_CONSUME,Map.of("X",1),Map.of(),Map.of("X",1)));}
    @Test void pedestalModeNeverConsumesInventory(){OfferingPlan plan=OfferingPlan.create(OfferingMode.PEDESTAL_DEPOSIT,Map.of("X",2),Map.of("X",10),Map.of("X",2));assertTrue(plan.inventory().isEmpty());assertEquals(2,plan.pedestal().get("X"));}
    @Test void hybridUsesInventoryThenPedestalAndRefundsExactTotal(){OfferingPlan plan=OfferingPlan.create(OfferingMode.HYBRID,Map.of("X",5),Map.of("X",2),Map.of("X",4));assertEquals(2,plan.inventory().get("X"));assertEquals(3,plan.pedestal().get("X"));assertEquals(5,plan.refund().get("X"));}
}
