package com.dragonaltar.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public final class ConfigService {
	private final JavaPlugin plugin;
	private final Map<String, YamlConfiguration> files = new java.util.HashMap<>();

	public ConfigService(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	public void load() {
		plugin.saveDefaultConfig();
		mergeGeneralDefaults();
		for (String name : new String[]{"messages.yml", "altar.yml", "ritual.yml", "abilities.yml", "animations.yml"}) {
			File file = new File(plugin.getDataFolder(), name);
			if (!file.exists())
				plugin.saveResource(name, false);
			YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
			try (var stream = plugin.getResource(name)) {
				if (stream != null) {
					YamlConfiguration defaults = YamlConfiguration
							.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
					boolean changed = false;
					int previousVersion = loaded.getInt("config-version", 0);
					if (name.equals("altar.yml") && previousVersion < 3 && migrateProtectionV3(loaded))
						changed = true;
					if (name.equals("ritual.yml") && previousVersion < 2 && isOriginalBetaRecipe(loaded)) {
						loaded.set("offerings", defaults.getList("offerings"));
						changed = true;
					}
					if (name.equals("messages.yml") && previousVersion < 5 && loaded
							.getString("removal-ritual-backfire", "").contains("intended target remains untouched")) {
						loaded.set("removal-ritual-backfire", defaults.getString("removal-ritual-backfire"));
						loaded.set("all-dragonborn-silenced", defaults.getString("all-dragonborn-silenced"));
						changed = true;
					}
					if (name.equals("messages.yml") && migrateAbilityHudMessage(loaded, defaults, previousVersion))
						changed = true;
					if (name.equals("messages.yml") && migrateResonanceHudMessage(loaded, defaults, previousVersion))
						changed = true;
					if (name.equals("abilities.yml") && previousVersion < 5 && migrateResonanceV5(loaded, defaults))
						changed = true;
					if (name.equals("abilities.yml") && previousVersion < 6 && migrateRevV6(loaded, defaults))
						changed = true;
					for (String key : defaults.getKeys(true))
						if (!defaults.isConfigurationSection(key) && !loaded.contains(key)) {
							loaded.set(key, defaults.get(key));
							changed = true;
						}
					int currentVersion = defaults.getInt("config-version", previousVersion);
					if (previousVersion < currentVersion) {
						loaded.set("config-version", currentVersion);
						changed = true;
					}
					if (changed)
						loaded.save(file);
				}
			} catch (Exception ex) {
				throw new IllegalStateException("Could not merge defaults into " + name, ex);
			}
			files.put(name, loaded);
		}
	}

	public void reload() {
		plugin.reloadConfig();
		load();
	}
	public YamlConfiguration file(String name) {
		return files.get(name);
	}
	public org.bukkit.configuration.file.FileConfiguration general() {
		return plugin.getConfig();
	}
	public ServerMode serverMode() {
		try {
			return ServerMode.valueOf(plugin.getConfig().getString("server-mode", "BETA").toUpperCase());
		} catch (IllegalArgumentException ex) {
			return ServerMode.BETA;
		}
	}
	public boolean destructiveAllowed() {
		return SafetyPolicy.destructiveAllowed(serverMode(),
				plugin.getConfig().getBoolean("safety.allow-destructive-commands-in-production", false));
	}
	private static boolean isOriginalBetaRecipe(YamlConfiguration loaded) {
		var offerings = loaded.getMapList("offerings");
		if (offerings.size() != 1)
			return false;
		Map<?, ?> only = offerings.getFirst();
		return "NETHER_STAR".equalsIgnoreCase(String.valueOf(only.get("material")))
				&& "1".equals(String.valueOf(only.get("amount")));
	}
	private void mergeGeneralDefaults() {
		File file = new File(plugin.getDataFolder(), "config.yml");
		YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
		try (var stream = plugin.getResource("config.yml")) {
			if (stream == null)
				return;
			YamlConfiguration defaults = YamlConfiguration
					.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
			boolean changed = mergeMissing(loaded, defaults);
			if (changed)
				loaded.save(file);
			plugin.reloadConfig();
		} catch (Exception ex) {
			throw new IllegalStateException("Could not merge defaults into config.yml", ex);
		}
	}
	static boolean mergeMissing(YamlConfiguration loaded, YamlConfiguration defaults) {
		boolean changed = false;
		for (String key : defaults.getKeys(true))
			if (!defaults.isConfigurationSection(key) && !loaded.contains(key)) {
				loaded.set(key, defaults.get(key));
				changed = true;
			}
		int currentVersion = defaults.getInt("config-version", loaded.getInt("config-version", 0));
		if (loaded.getInt("config-version", 0) < currentVersion) {
			loaded.set("config-version", currentVersion);
			changed = true;
		}
		return changed;
	}
	static boolean migrateAbilityHudMessage(YamlConfiguration loaded, YamlConfiguration defaults, int previousVersion) {
		if (previousVersion >= 6)
			return false;
		String oldDefault = "<light_purple>Dragon Energy:</light_purple> <energy>/<maximum> <gray>|</gray> <ability> <gray>|</gray> <yellow>Ability CD: <cooldown></yellow> <gray>|</gray> <gold>Ultimate CD: <ultimate_cooldown></gold>";
		if (!oldDefault.equals(loaded.getString("energy-hud")))
			return false;
		loaded.set("energy-hud", defaults.getString("energy-hud"));
		return true;
	}
	static boolean migrateResonanceHudMessage(YamlConfiguration loaded, YamlConfiguration defaults,
			int previousVersion) {
		if (previousVersion >= 7)
			return false;
		String oldDefault = "<light_purple>Dragon Energy:</light_purple> <energy>/<maximum> <gray>|</gray> <ability> <gray>|</gray> <yellow>Ability CD: <cooldown></yellow> <gray>|</gray> <gold>Ultimate CD: <ultimate_cooldown></gold><status>";
		if (!oldDefault.equals(loaded.getString("energy-hud")))
			return false;
		loaded.set("energy-hud", defaults.getString("energy-hud"));
		return true;
	}
	static boolean migrateResonanceV5(YamlConfiguration loaded, YamlConfiguration defaults) {
		boolean changed = false;
		changed |= renameTunable(loaded, defaults, "resonances.thermal-convergence.damage",
				"resonances.thermal-convergence.initial-damage", 10d);
		changed |= renameTunable(loaded, defaults, "resonances.thermal-convergence.radius",
				"resonances.thermal-convergence.radius", 10d);
		changed |= renameTunable(loaded, defaults, "resonances.volcanic-aegis.damage",
				"resonances.volcanic-aegis.initial-damage", 8d);
		changed |= renameTunable(loaded, defaults, "resonances.volcanic-aegis.radius",
				"resonances.volcanic-aegis.finisher-radius", 8d);
		changed |= renameTunable(loaded, defaults, "resonances.volcanic-aegis.knockback",
				"resonances.volcanic-aegis.finisher-knockback", 1.1d);
		changed |= renameTunable(loaded, defaults, "resonances.volcanic-aegis.buff-seconds",
				"resonances.volcanic-aegis.active-seconds", 8);
		changed |= renameTunable(loaded, defaults, "resonances.glacial-bastion.damage",
				"resonances.glacial-bastion.finisher-damage", 6d);
		changed |= renameTunable(loaded, defaults, "resonances.glacial-bastion.radius",
				"resonances.glacial-bastion.domain-radius", 9d);
		changed |= renameTunable(loaded, defaults, "resonances.glacial-bastion.buff-seconds",
				"resonances.glacial-bastion.active-seconds", 8);
		changed |= renameTunable(loaded, defaults, "resonances.dragon-trinity.damage",
				"resonances.dragon-trinity.initial-damage", 20d);
		changed |= renameTunable(loaded, defaults, "resonances.dragon-trinity.radius",
				"resonances.dragon-trinity.radius", 15d);
		changed |= renameTunable(loaded, defaults, "resonances.dragon-trinity.knockback",
				"resonances.dragon-trinity.finisher-knockback", 1.6d);
		changed |= renameTunable(loaded, defaults, "resonances.dragon-trinity.buff-seconds",
				"resonances.dragon-trinity.active-seconds", 12);
		return changed;
	}
	static boolean migrateRevV6(YamlConfiguration loaded, YamlConfiguration defaults) {
		boolean changed = false;
		changed |= renameTunable(loaded, defaults, "abilities.revs-rend.dash-strength",
				"abilities.revs-rend.dash-strength", 2.2d);
		changed |= renameScaledTunable(loaded, defaults, "abilities.revs-rend.fire-seconds",
				"abilities.revs-rend.pressure-fire-ticks", 6d, 20d);
		changed |= renameTunable(loaded, defaults, "abilities.infernos-wrath.burst-damage",
				"abilities.infernos-wrath.impact-damage", 14d);
		changed |= renameScaledTunable(loaded, defaults, "abilities.infernos-wrath.speed-seconds",
				"abilities.infernos-wrath.initial-mobility-ticks", 6d, 20d);
		changed |= renameScaledTunable(loaded, defaults, "abilities.infernos-wrath.maximum-pursuit-seconds",
				"abilities.infernos-wrath.maximum-mobility-ticks", 10d, 20d);
		changed |= renameTunable(loaded, defaults, "abilities.infernos-wrath.reward-extension-ticks",
				"abilities.infernos-wrath.mobility-extension-ticks", 20);
		changed |= renameScaledTunable(loaded, defaults, "abilities.infernos-wrath.mark-duration-seconds",
				"rev-hunt.mark.duration-ticks", 8d, 20d);
		return changed;
	}
	static boolean migrateProtectionV3(YamlConfiguration loaded) {
		if (!loaded.contains("protection.enabled"))
			return false;
		boolean legacy = loaded.getBoolean("protection.enabled", false);
		if (!loaded.contains("internal-protection.enabled")
				|| (legacy && !loaded.getBoolean("internal-protection.enabled", false)))
			loaded.set("internal-protection.enabled", legacy);
		loaded.set("protection.enabled", null);
		return true;
	}
	private static boolean renameScaledTunable(YamlConfiguration loaded, YamlConfiguration defaults, String oldPath,
			String newPath, double oldDefault, double scale) {
		if (!loaded.contains(oldPath) || loaded.contains(newPath))
			return false;
		double value = loaded.getDouble(oldPath);
		Object replacement = Double.compare(value, oldDefault) == 0
				? defaults.get(newPath)
				: (int) Math.round(value * scale);
		loaded.set(newPath, replacement);
		loaded.set(oldPath, null);
		return true;
	}
	private static boolean renameTunable(YamlConfiguration loaded, YamlConfiguration defaults, String oldPath,
			String newPath, Object oldDefault) {
		if (!loaded.contains(oldPath) || (!oldPath.equals(newPath) && loaded.contains(newPath)))
			return false;
		Object oldValue = loaded.get(oldPath);
		Object replacement = String.valueOf(oldValue).equals(String.valueOf(oldDefault))
				? defaults.get(newPath)
				: oldValue;
		loaded.set(newPath, replacement);
		if (!oldPath.equals(newPath))
			loaded.set(oldPath, null);
		return !Objects.equals(oldValue, replacement) || !oldPath.equals(newPath);
	}
}
