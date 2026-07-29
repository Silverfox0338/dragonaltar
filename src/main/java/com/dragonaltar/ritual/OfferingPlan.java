package com.dragonaltar.ritual;

import java.util.*;

public record OfferingPlan(Map<String,Integer> inventory,Map<String,Integer> pedestal,Map<String,Integer> refund) {
    public OfferingPlan {inventory=Map.copyOf(inventory);pedestal=Map.copyOf(pedestal);refund=Map.copyOf(refund);}
    public static OfferingPlan create(OfferingMode mode,Map<String,Integer> requirements,Map<String,Integer> inventoryAvailable,Map<String,Integer> pedestalAvailable){
        Map<String,Integer> inventory=new LinkedHashMap<>(),pedestal=new LinkedHashMap<>(),refund=new LinkedHashMap<>();
        for(var entry:requirements.entrySet()){String material=entry.getKey();int required=entry.getValue();if(required<=0)continue;int fromInventory=0,fromPedestal=0;
            if(mode!=OfferingMode.PEDESTAL_DEPOSIT)fromInventory=Math.min(required,Math.max(0,inventoryAvailable.getOrDefault(material,0)));
            int remaining=required-fromInventory;if(mode!=OfferingMode.INVENTORY_CONSUME)fromPedestal=Math.min(remaining,Math.max(0,pedestalAvailable.getOrDefault(material,0)));
            if(fromInventory+fromPedestal<required)throw new IllegalStateException("Missing offering: "+material+" x"+required);
            if(fromInventory>0)inventory.put(material,fromInventory);if(fromPedestal>0)pedestal.put(material,fromPedestal);refund.put(material,required);
        }return new OfferingPlan(inventory,pedestal,refund);
    }
}
