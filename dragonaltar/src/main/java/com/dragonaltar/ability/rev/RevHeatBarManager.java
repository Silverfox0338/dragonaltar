package com.dragonaltar.ability.rev;

import com.dragonaltar.config.ConfigService;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class RevHeatBarManager {
	enum HeatTier {
		STALKING, PURSUING, PREDATOR
	}

	private final ConfigService config;
	private final Map<UUID, BossBar> bars = new HashMap<>();
	private final MiniMessage mini = MiniMessage.miniMessage();

	public RevHeatBarManager(ConfigService config) {
		this.config = config;
	}

	public void update(Player player, int heat, int maximum, int mobilityThreshold, int trackingThreshold,
			boolean visible) {
		if (!visible) {
			hide(player);
			return;
		}
		int boundedMaximum = Math.max(1, maximum);
		int boundedHeat = Math.max(0, Math.min(boundedMaximum, heat));
		HeatTier tier = tier(boundedHeat, mobilityThreshold, trackingThreshold);
		BossBar bar = bars.computeIfAbsent(player.getUniqueId(), ignored -> {
			BossBar created = BossBar.bossBar(net.kyori.adventure.text.Component.empty(), 0, BossBar.Color.YELLOW,
					overlay());
			player.showBossBar(created);
			return created;
		});
		String title = config.file("abilities.yml").getString("rev-hunt.heat-bar.title",
				"<red><bold>REV HEAT</bold></red> <gray><heat>/<maximum> | <tier></gray>");
		bar.name(mini.deserialize(title.replace("<heat>", Integer.toString(boundedHeat))
				.replace("<maximum>", Integer.toString(boundedMaximum)).replace("<tier>", displayName(tier))));
		bar.progress(progress(boundedHeat, boundedMaximum));
		bar.color(color(tier));
	}

	public void hide(Player player) {
		BossBar bar = bars.remove(player.getUniqueId());
		if (bar != null)
			player.hideBossBar(bar);
	}

	public void hideAll() {
		for (var entry : bars.entrySet()) {
			Player player = Bukkit.getPlayer(entry.getKey());
			if (player != null)
				player.hideBossBar(entry.getValue());
		}
		bars.clear();
	}

	static float progress(int heat, int maximum) {
		if (maximum <= 0)
			return 0;
		return (float) Math.max(0, Math.min(1, heat / (double) maximum));
	}

	static HeatTier tier(int heat, int mobilityThreshold, int trackingThreshold) {
		if (heat >= trackingThreshold)
			return HeatTier.PREDATOR;
		if (heat >= mobilityThreshold)
			return HeatTier.PURSUING;
		return HeatTier.STALKING;
	}

	private String displayName(HeatTier tier) {
		return switch (tier) {
			case STALKING -> "Stalking";
			case PURSUING -> "Pursuing";
			case PREDATOR -> "Predator";
		};
	}

	private BossBar.Color color(HeatTier tier) {
		String path = switch (tier) {
			case STALKING -> "stalking-color";
			case PURSUING -> "pursuing-color";
			case PREDATOR -> "predator-color";
		};
		BossBar.Color fallback = switch (tier) {
			case STALKING -> BossBar.Color.YELLOW;
			case PURSUING -> BossBar.Color.RED;
			case PREDATOR -> BossBar.Color.PURPLE;
		};
		try {
			return BossBar.Color.valueOf(config.file("abilities.yml")
					.getString("rev-hunt.heat-bar." + path, fallback.name()).toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			return fallback;
		}
	}

	private BossBar.Overlay overlay() {
		try {
			return BossBar.Overlay.valueOf(config.file("abilities.yml")
					.getString("rev-hunt.heat-bar.overlay", "PROGRESS").toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			return BossBar.Overlay.PROGRESS;
		}
	}
}
