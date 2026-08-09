package com.dragonaltar.player;
public record PlayerSettings(EffectMode effects, boolean hud, SelectorMode selector, boolean slowFalling,
		boolean passiveParticles, boolean animationParticles, boolean sounds, boolean titles, boolean screenEffects) {
	public static PlayerSettings defaults() {
		return new PlayerSettings(EffectMode.FULL, true, SelectorMode.LOCKED, true, true, true, true, true, true);
	}
	public PlayerSettings withEffects(EffectMode value) {
		return new PlayerSettings(value, hud, selector, slowFalling, passiveParticles, animationParticles, sounds,
				titles, screenEffects);
	}
	public PlayerSettings withHud(boolean value) {
		return new PlayerSettings(effects, value, selector, slowFalling, passiveParticles, animationParticles, sounds,
				titles, screenEffects);
	}
	public PlayerSettings withSelector(SelectorMode value) {
		return new PlayerSettings(effects, hud, value, slowFalling, passiveParticles, animationParticles, sounds,
				titles, screenEffects);
	}
	public PlayerSettings withSlowFalling(boolean value) {
		return new PlayerSettings(effects, hud, selector, value, passiveParticles, animationParticles, sounds, titles,
				screenEffects);
	}
	public PlayerSettings withPassiveParticles(boolean value) {
		return new PlayerSettings(effects, hud, selector, slowFalling, value, animationParticles, sounds, titles,
				screenEffects);
	}
	public PlayerSettings withAnimationParticles(boolean value) {
		return new PlayerSettings(effects, hud, selector, slowFalling, passiveParticles, value, sounds, titles,
				screenEffects);
	}
	public PlayerSettings withSounds(boolean value) {
		return new PlayerSettings(effects, hud, selector, slowFalling, passiveParticles, animationParticles, value,
				titles, screenEffects);
	}
	public PlayerSettings withTitles(boolean value) {
		return new PlayerSettings(effects, hud, selector, slowFalling, passiveParticles, animationParticles, sounds,
				value, screenEffects);
	}
	public PlayerSettings withScreenEffects(boolean value) {
		return new PlayerSettings(effects, hud, selector, slowFalling, passiveParticles, animationParticles, sounds,
				titles, value);
	}
}
