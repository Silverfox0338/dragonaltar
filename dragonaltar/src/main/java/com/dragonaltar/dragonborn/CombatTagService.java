package com.dragonaltar.dragonborn;

import org.bukkit.entity.Player;
import java.time.*;
import java.util.*;

public final class CombatTagService {
	private final long durationMillis;
	private final Map<UUID, Tag> tags = new HashMap<>();
	public CombatTagService(long seconds) {
		durationMillis = Math.max(0, seconds) * 1000L;
	}
	public synchronized void tag(Player first, Player second) {
		long until = System.currentTimeMillis() + durationMillis;
		tags.put(first.getUniqueId(), new Tag(second.getUniqueId(), until));
		tags.put(second.getUniqueId(), new Tag(first.getUniqueId(), until));
	}
	public synchronized boolean tagged(UUID player) {
		prune(player);
		return tags.containsKey(player);
	}
	public synchronized int seconds(UUID player) {
		prune(player);
		Tag tag = tags.get(player);
		return tag == null ? 0 : (int) Math.ceil((tag.until - System.currentTimeMillis()) / 1000d);
	}
	public synchronized Optional<UUID> opponent(UUID player) {
		prune(player);
		Tag tag = tags.get(player);
		return tag == null ? Optional.empty() : Optional.of(tag.opponent);
	}
	public synchronized void clear(UUID player) {
		tags.remove(player);
	}
	private void prune(UUID player) {
		Tag tag = tags.get(player);
		if (tag != null && tag.until <= System.currentTimeMillis())
			tags.remove(player);
	}
	private record Tag(UUID opponent, long until) {
	}
}
