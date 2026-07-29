package com.dragonaltar.animation;
import java.util.List;
public record AnimationDefinition(String id, List<AnimationStep> steps) {
    public AnimationDefinition { steps=steps.stream().sorted(java.util.Comparator.comparingLong(AnimationStep::atTick)).toList(); }
}
