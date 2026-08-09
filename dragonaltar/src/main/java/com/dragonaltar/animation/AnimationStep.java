package com.dragonaltar.animation;
import java.util.Map;
public record AnimationStep(long atTick, AnimationActionType type, Map<String, Object> options) {
	public AnimationStep {
		options = Map.copyOf(options);
	}
}
