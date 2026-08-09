package com.dragonaltar.ability;

import com.dragonaltar.config.ConfigService;
import com.dragonaltar.player.EffectMode;
import com.dragonaltar.player.PlayerDataService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Owns ability presentation limits and per-viewer accessibility preferences.
 */
final class AbilityPresentation {
	private final ConfigService config;
	private final PlayerDataService players;
	private final Logger logger;
	private final MiniMessage miniMessage = MiniMessage.miniMessage();
	private final Set<String> warnedSounds = new HashSet<>();

	AbilityPresentation(ConfigService config, PlayerDataService players, Logger logger) {
		this.config = config;
		this.players = players;
		this.logger = logger;
	}

	void playConfiguredSound(Location location, String path, Sound fallback, float fallbackVolume,
			float fallbackPitch) {
		String configured = config.file("abilities.yml").getString("abilities." + path);
		if (path.startsWith("rev-hunt.")) {
			configured = config.file("abilities.yml").getString(path);
		}
		Sound sound = configured == null ? fallback : resolveSound(configured, fallback, path);
		double volume = path.startsWith("rev-hunt.")
				? decimalRoot(path + "-volume", fallbackVolume)
				: decimal(path + "-volume", fallbackVolume);
		double pitch = path.startsWith("rev-hunt.")
				? decimalRoot(path + "-pitch", fallbackPitch)
				: decimal(path + "-pitch", fallbackPitch);
		playSound(location, sound, (float) volume, (float) pitch);
	}

	void showTitle(Player player, String title, String subtitle) {
		if (players.settings(player.getUniqueId()).titles()) {
			player.showTitle(Title.title(miniMessage.deserialize(title), miniMessage.deserialize(subtitle)));
		}
	}

	void playSound(Location location, Sound sound, float volume, float pitch) {
		if (location.getWorld() == null) {
			return;
		}
		int viewDistance = integerRoot("presentation.view-distance-blocks", 48);
		for (Player viewer : location.getWorld().getPlayers()) {
			if (viewer.getLocation().distanceSquared(location) <= viewDistance * viewDistance
					&& players.settings(viewer.getUniqueId()).sounds()) {
				viewer.playSound(location, sound, volume, pitch);
			}
		}
	}

	void particle(Location location, Particle particle, int requested, double offsetX, double offsetY, double offsetZ,
			double extra, Object data) {
		emitParticles(location, particle, requested, offsetX, offsetY, offsetZ, extra, data, false);
	}

	void passiveParticle(Location location, Particle particle, int requested, double offsetX, double offsetY,
			double offsetZ, double extra) {
		emitParticles(location, particle, requested, offsetX, offsetY, offsetZ, extra, null, true);
	}

	private void emitParticles(Location location, Particle particle, int requested, double offsetX, double offsetY,
			double offsetZ, double extra, Object data, boolean passive) {
		if (location.getWorld() == null) {
			return;
		}
		int bounded = Math.max(0, Math.min(integerRoot("presentation.maximum-particles-per-emission", 128), requested));
		int viewDistance = integerRoot("presentation.view-distance-blocks", 48);
		for (Player viewer : location.getWorld().getPlayers()) {
			if (viewer.getLocation().distanceSquared(location) > viewDistance * viewDistance) {
				continue;
			}
			var settings = players.settings(viewer.getUniqueId());
			boolean particlesEnabled = passive ? settings.passiveParticles() : settings.animationParticles();
			if (!particlesEnabled || settings.effects() == EffectMode.MINIMAL) {
				continue;
			}
			int count = settings.effects() == EffectMode.REDUCED ? Math.max(1, bounded / 2) : bounded;
			if (count <= 0) {
				continue;
			}
			if (data == null) {
				viewer.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
			} else {
				viewer.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, data);
			}
		}
	}

	private Sound resolveSound(String configured, Sound fallback, String path) {
		NamespacedKey key = NamespacedKey.fromString(configured.toLowerCase(Locale.ROOT));
		Sound matched = key == null ? null : Registry.SOUNDS.get(key);
		if (matched == null) {
			try {
				Object legacy = Sound.class.getField(configured.toUpperCase(Locale.ROOT)).get(null);
				if (legacy instanceof Sound found) {
					matched = found;
				}
			} catch (ReflectiveOperationException exception) {
				if (warnedSounds.add(path + '=' + configured)) {
					logger.warning("Invalid abilities.yml sound '" + configured + "' at " + path
							+ "; using the built-in fallback");
				}
			}
		}
		return matched == null ? fallback : matched;
	}

	private int integerRoot(String path, int fallback) {
		return config.file("abilities.yml").getInt(path, fallback);
	}

	private double decimal(String path, double fallback) {
		return config.file("abilities.yml").getDouble("abilities." + path, fallback);
	}

	private double decimalRoot(String path, double fallback) {
		return config.file("abilities.yml").getDouble(path, fallback);
	}
}
