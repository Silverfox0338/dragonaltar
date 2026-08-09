package com.dragonaltar.ritual;
import org.bukkit.inventory.ItemStack;
import java.util.*;
public record RitualSession(UUID playerId, String soulId, RitualPhase phase, List<ItemStack> consumed, UUID sessionId) {
	public RitualSession {
		consumed = List.copyOf(consumed);
	}
	public RitualSession withPhase(RitualPhase next) {
		return new RitualSession(playerId, soulId, next, consumed, sessionId);
	}
}
