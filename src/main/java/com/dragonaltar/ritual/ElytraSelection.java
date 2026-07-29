package com.dragonaltar.ritual;

import java.util.*;

public final class ElytraSelection {
    private ElytraSelection(){}
    public enum Policy { MOST_DAMAGED, LEAST_DAMAGED, FIRST_MATCH, LOWEST_ENCHANTMENT_VALUE }
    public record Candidate(int slot,int damage,int enchantmentValue){}
    public static Candidate select(List<Candidate> candidates,Policy policy){
        if(candidates.isEmpty())throw new IllegalArgumentException("No Elytra candidates");
        Comparator<Candidate> comparator=switch(policy){
            case MOST_DAMAGED->Comparator.comparingInt((Candidate value)->-value.damage());
            case LEAST_DAMAGED->Comparator.comparingInt(Candidate::damage);
            case FIRST_MATCH->Comparator.comparingInt(Candidate::slot);
            case LOWEST_ENCHANTMENT_VALUE->Comparator.comparingInt(Candidate::enchantmentValue);
        };return candidates.stream().min(comparator.thenComparingInt(Candidate::slot)).orElseThrow();
    }
}
