package com.dragonaltar.altar;

import com.dragonaltar.DragonAltarPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.Particle;

import java.time.Duration;
import java.util.*;

public final class AltarSetupService {
	private final DragonAltarPlugin plugin;
	private final long timeoutMillis;
	private final Map<UUID, Session> sessions = new HashMap<>();
	public AltarSetupService(DragonAltarPlugin plugin, Duration timeout) {
		this.plugin = plugin;
		timeoutMillis = timeout.toMillis();
	}
	public synchronized void begin(Player player) {
		sessions.put(player.getUniqueId(),
				new Session(System.currentTimeMillis(), new LinkedHashMap<>(), new LinkedHashSet<>()));
	}
	public synchronized void set(Player player, String path, Location location) {
		Session session = require(player);
		session.locations.put(path, location.clone());
		session.removals.remove(path);
		session.lastActivity = System.currentTimeMillis();
	}
	public synchronized void remove(Player player, String path) {
		Session session = require(player);
		session.locations.remove(path);
		session.removals.add(path);
		session.lastActivity = System.currentTimeMillis();
	}
	public synchronized Set<String> staged(Player player) {
		Session session = require(player);
		Set<String> values = new LinkedHashSet<>(session.locations.keySet());
		values.addAll(session.removals.stream().map(x -> "-" + x).toList());
		return values;
	}
	public synchronized void preview(Player player) {
		Session session = require(player);
		for (var entry : session.locations.entrySet()) {
			Location location = entry.getValue();
			player.spawnParticle(Particle.END_ROD, location, 20, .3, .5, .3, .01);
			player.sendMessage(entry.getKey() + ": " + location.getWorld().getName() + " " + format(location.getX())
					+ "," + format(location.getY()) + "," + format(location.getZ()));
		}
	}
	public synchronized void save(Player player) {
		Session session = require(player);
		session.locations.forEach((path, location) -> plugin.saveLocation("altar.yml", path, location));
		session.removals.forEach(path -> plugin.setAltarValue(path, null));
		sessions.remove(player.getUniqueId());
	}
	public synchronized void cancel(Player player) {
		require(player);
		sessions.remove(player.getUniqueId());
	}
	public synchronized boolean active(Player player) {
		Session session = sessions.get(player.getUniqueId());
		if (session == null)
			return false;
		if (expired(session)) {
			sessions.remove(player.getUniqueId());
			return false;
		}
		return true;
	}
	private Session require(Player player) {
		Session session = sessions.get(player.getUniqueId());
		if (session == null || expired(session)) {
			sessions.remove(player.getUniqueId());
			throw new IllegalStateException("No active setup session; run /dragon setup begin");
		}
		return session;
	}
	private boolean expired(Session session) {
		return System.currentTimeMillis() - session.lastActivity > timeoutMillis;
	}
	private static String format(double value) {
		return String.format(Locale.ROOT, "%.3f", value);
	}
	private static final class Session {
		private long lastActivity;
		private final Map<String, Location> locations;
		private final Set<String> removals;
		private Session(long lastActivity, Map<String, Location> locations, Set<String> removals) {
			this.lastActivity = lastActivity;
			this.locations = locations;
			this.removals = removals;
		}
	}
}
