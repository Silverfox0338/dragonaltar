package com.dragonaltar.player;

import com.dragonaltar.persistence.YamlDataStore;
import com.dragonaltar.persistence.CooldownCodec;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;
import java.util.logging.Logger;

public final class PlayerDataService {
	private final YamlDataStore store;
	private final Logger logger;
	private final Map<UUID, PlayerSettings> settings = new HashMap<>();
	private final Map<UUID, Integer> energy = new HashMap<>();
	private final Map<UUID, String> selection = new HashMap<>();
	private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
	private final Map<UUID, List<String>> history = new HashMap<>();
	public PlayerDataService(YamlDataStore store, Logger logger) {
		this.store = store;
		this.logger = Objects.requireNonNull(logger, "logger");
	}
	public synchronized void load() {
		settings.clear();
		energy.clear();
		selection.clear();
		cooldowns.clear();
		history.clear();
		YamlConfiguration y = store.load("players.yml");
		ConfigurationSection root = y.getConfigurationSection("players");
		if (root != null)
			for (String key : root.getKeys(false)) {
				try {
					UUID id = UUID.fromString(key);
					String p = "players." + key + ".";
					settings.put(id, new PlayerSettings(EffectMode.valueOf(y.getString(p + "settings.effects", "FULL")),
							y.getBoolean(p + "settings.hud", true),
							SelectorMode.valueOf(y.getString(p + "settings.selector", "LOCKED")),
							y.getBoolean(p + "settings.slow-falling", true),
							y.getBoolean(p + "settings.passive-particles", true),
							y.getBoolean(p + "settings.animation-particles", true),
							y.getBoolean(p + "settings.sounds", true), y.getBoolean(p + "settings.titles", true),
							y.getBoolean(p + "settings.screen-effects", true)));
					if (y.contains(p + "energy"))
						energy.put(id, y.getInt(p + "energy"));
					String selected = y.getString(p + "selected-ability");
					if (selected != null)
						selection.put(id, selected);
					history.put(id, new ArrayList<>(y.getStringList(p + "dragonborn-history")));
				} catch (IllegalArgumentException | NullPointerException exception) {
					logger.warning("Skipping malformed players.yml entry '" + key + "': "
							+ exception.getClass().getSimpleName() + " (check UUID, effects, and selector values)");
				}
			}
		YamlConfiguration c = store.load("cooldowns.yml");
		ConfigurationSection cooldownRoot = c.getConfigurationSection("players");
		if (cooldownRoot != null)
			for (String key : cooldownRoot.getKeys(false)) {
				try {
					UUID id = UUID.fromString(key);
					ConfigurationSection section = cooldownRoot.getConfigurationSection(key);
					if (section != null)
						cooldowns.put(id, CooldownCodec.decode(section.getValues(false)));
				} catch (IllegalArgumentException exception) {
					logger.warning("Skipping malformed cooldowns.yml entry '" + key
							+ "': expected a UUID key and numeric expiry values");
				}
			}
	}
	public synchronized PlayerSettings settings(UUID id) {
		return settings.getOrDefault(id, PlayerSettings.defaults());
	}
	public synchronized void settings(UUID id, PlayerSettings value) {
		settings.put(id, value);
		save();
	}
	public synchronized OptionalInt energy(UUID id) {
		Integer value = energy.get(id);
		return value == null ? OptionalInt.empty() : OptionalInt.of(value);
	}
	public synchronized void energy(UUID id, int value) {
		energy.put(id, value);
		save();
	}
	public synchronized String selection(UUID id, String fallback) {
		return selection.getOrDefault(id, fallback);
	}
	public synchronized void setSelection(UUID id, String value) {
		selection.put(id, value);
		save();
	}
	public synchronized Map<String, Long> cooldowns(UUID id) {
		return new HashMap<>(cooldowns.getOrDefault(id, Map.of()));
	}
	public synchronized void cooldowns(UUID id, Map<String, Long> values) {
		cooldowns.put(id, new HashMap<>(values));
		save();
	}
	public synchronized void recordHistory(UUID id, String event, String soul) {
		history.computeIfAbsent(id, k -> new ArrayList<>()).add(java.time.Instant.now() + "|" + event + "|" + soul);
		save();
	}
	public synchronized List<String> history(UUID id) {
		return List.copyOf(history.getOrDefault(id, List.of()));
	}
	public synchronized void clearHistory() {
		history.clear();
		save();
	}
	public synchronized void reset() {
		settings.clear();
		energy.clear();
		selection.clear();
		cooldowns.clear();
		history.clear();
		save();
	}
	private void save() {
		YamlConfiguration y = new YamlConfiguration();
		y.set("data-version", 1);
		Set<UUID> ids = new TreeSet<>();
		ids.addAll(settings.keySet());
		ids.addAll(energy.keySet());
		ids.addAll(selection.keySet());
		ids.addAll(cooldowns.keySet());
		ids.addAll(history.keySet());
		for (UUID id : ids) {
			String p = "players." + id + ".";
			PlayerSettings s = settings(id);
			y.set(p + "settings.effects", s.effects().name());
			y.set(p + "settings.hud", s.hud());
			y.set(p + "settings.selector", s.selector().name());
			y.set(p + "settings.slow-falling", s.slowFalling());
			y.set(p + "settings.passive-particles", s.passiveParticles());
			y.set(p + "settings.animation-particles", s.animationParticles());
			y.set(p + "settings.sounds", s.sounds());
			y.set(p + "settings.titles", s.titles());
			y.set(p + "settings.screen-effects", s.screenEffects());
			y.set(p + "energy", energy.get(id));
			y.set(p + "selected-ability", selection.get(id));
			y.set(p + "dragonborn-history", history.getOrDefault(id, List.of()));
		}
		store.saveCoalesced("players.yml", y);
		YamlConfiguration c = new YamlConfiguration();
		c.set("data-version", 1);
		for (var entry : cooldowns.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList())
			c.set("players." + entry.getKey(), CooldownCodec.encode(entry.getValue()));
		store.saveCoalesced("cooldowns.yml", c);
	}
}
