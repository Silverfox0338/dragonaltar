package com.dragonaltar.ability;
import net.kyori.adventure.text.Component;
import com.dragonaltar.soul.SoulIdentity;
import java.util.Set;
public interface DragonAbility {
	String id();
	Component displayName();
	AbilityCategory category();
	int energyCost();
	long cooldownMillis();
	default boolean ultimate() {
		return false;
	}
	default Set<SoulIdentity> souls() {
		return Set.of(SoulIdentity.values());
	}
	default boolean supports(SoulIdentity soul) {
		return souls().contains(soul);
	}
	default AbilityResult canUse(AbilityContext context) {
		return AbilityResult.ok();
	}
	AbilityResult activate(AbilityContext context);
}
