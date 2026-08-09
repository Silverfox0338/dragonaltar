package com.dragonaltar.animation;

import com.dragonaltar.config.ConfigService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Display;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.persistence.PersistentDataType;
import com.dragonaltar.DragonAltarPlugin;
import com.dragonaltar.player.EffectMode;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import org.joml.AxisAngle4f;

public final class AnimationService {
	private final DragonAltarPlugin plugin;
	private final ConfigService config;
	private final Map<String, AnimationDefinition> definitions = new LinkedHashMap<>();
	private final Map<UUID, List<BukkitTask>> sessions = new HashMap<>();
	private final MiniMessage mini = MiniMessage.miniMessage();
	private final Set<BossBar> activeBossBars = new HashSet<>();
	private final Set<UUID> glowingPlayers = new HashSet<>();
	public AnimationService(DragonAltarPlugin plugin, ConfigService config) {
		this.plugin = plugin;
		this.config = config;
	}
	public void load() {
		definitions.clear();
		YamlConfiguration y = config.file("animations.yml");
		for (String id : y.getKeys(false)) {
			if (id.equals("config-version"))
				continue;
			List<AnimationStep> steps = new ArrayList<>();
			for (Map<?, ?> raw : y.getMapList(id)) {
				try {
					Object tickValue = raw.containsKey("at-tick") ? raw.get("at-tick") : 0;
					long tick = Long.parseLong(String.valueOf(tickValue));
					AnimationActionType type = AnimationActionType
							.valueOf(String.valueOf(raw.get("type")).toUpperCase());
					Map<String, Object> options = new LinkedHashMap<>();
					raw.forEach((k, v) -> options.put(String.valueOf(k), v));
					steps.add(new AnimationStep(tick, type, options));
				} catch (RuntimeException ex) {
					plugin.getLogger().warning("Skipping invalid animation step in " + id + ": " + ex.getMessage());
				}
			}
			definitions.put(id, new AnimationDefinition(id, steps));
		}
	}
	public Set<String> ids() {
		return Collections.unmodifiableSet(definitions.keySet());
	}
	public UUID play(String id, Location origin, Player subject) {
		AnimationDefinition def = Optional.ofNullable(definitions.get(id))
				.orElseThrow(() -> new IllegalArgumentException("Unknown animation " + id));
		UUID session = UUID.randomUUID();
		List<BukkitTask> tasks = new ArrayList<>();
		for (AnimationStep step : def.steps())
			tasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> execute(session, step, origin, subject),
					step.atTick()));
		long end = def.steps().stream().mapToLong(step -> step.atTick() + actionDuration(step)).max().orElse(0) + 1;
		tasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> sessions.remove(session), end));
		sessions.put(session, tasks);
		return session;
	}
	public void stop(UUID session) {
		List<BukkitTask> tasks = sessions.remove(session);
		if (tasks != null)
			tasks.forEach(BukkitTask::cancel);
	}
	public void stopAll() {
		for (UUID session : new ArrayList<>(sessions.keySet()))
			stop(session);
		for (BossBar bar : new ArrayList<>(activeBossBars))
			Bukkit.getOnlinePlayers().forEach(player -> player.hideBossBar(bar));
		activeBossBars.clear();
		for (UUID playerId : new ArrayList<>(glowingPlayers)) {
			Player player = Bukkit.getPlayer(playerId);
			if (player != null)
				player.setGlowing(false);
		}
		glowingPlayers.clear();
	}
	private void execute(UUID session, AnimationStep s, Location at, Player p) {
		try {
			switch (s.type()) {
				case SOUND -> {
					if (p != null && !plugin.players().settings(p.getUniqueId()).sounds())
						break;
					NamespacedKey key = Objects.requireNonNull(
							NamespacedKey.fromString(str(s, "sound", "minecraft:entity.ender_dragon.growl")),
							"Invalid sound key");
					Sound sound = Objects.requireNonNull(Registry.SOUNDS.get(key), "Unknown sound");
					if (p == null)
						at.getWorld().playSound(at, sound, num(s, "volume", 1), num(s, "pitch", 1));
					else
						p.playSound(at, sound, num(s, "volume", 1), num(s, "pitch", 1));
				}
				case PARTICLE -> {
					if (!animationParticles(p))
						break;
					Particle particle = Particle.valueOf(str(s, "particle", "DRAGON_BREATH"));
					int count = scaledCount(p, (int) num(s, "count", 30));
					spawnParticle(at.getWorld(), particle, at, count, 1, .5, 1, .02);
				}
				case PARTICLE_RING -> {
					if (!animationParticles(p))
						break;
					Particle particle = Particle.valueOf(str(s, "particle", "DRAGON_BREATH"));
					int count = scaledCount(p, (int) num(s, "count", 30));
					double radius = num(s, "radius", 2);
					for (int i = 0; i < count; i++) {
						double angle = Math.PI * 2 * i / count;
						spawnParticle(at.getWorld(), particle,
								at.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius), 1, 0, 0, 0, 0);
					}
				}
				case PARTICLE_SPIRAL -> {
					if (!animationParticles(p))
						break;
					Particle particle = Particle.valueOf(str(s, "particle", "DRAGON_BREATH"));
					int count = scaledCount(p, (int) num(s, "count", 30));
					double radius = num(s, "radius", 1.5), height = num(s, "height", 3);
					for (int i = 0; i < count; i++) {
						double progress = i / (double) count, angle = progress * Math.PI * 6;
						spawnParticle(at.getWorld(), particle,
								at.clone().add(Math.cos(angle) * radius, progress * height, Math.sin(angle) * radius),
								1, 0, 0, 0, 0);
					}
				}
				case LIGHTNING_EFFECT -> {
					if (screenEffects(p))
						at.getWorld().strikeLightningEffect(at);
				}
				case TITLE -> {
					if (p != null && plugin.players().settings(p.getUniqueId()).titles())
						p.showTitle(net.kyori.adventure.title.Title.title(mini.deserialize(str(s, "title", "")),
								Component.empty()));
				}
				case SUBTITLE -> {
					if (p != null && plugin.players().settings(p.getUniqueId()).titles())
						p.showTitle(net.kyori.adventure.title.Title.title(Component.empty(),
								mini.deserialize(str(s, "subtitle", ""))));
				}
				case ACTION_BAR -> {
					if (p != null)
						p.sendActionBar(mini.deserialize(str(s, "message", "")));
				}
				case BROADCAST -> Bukkit.broadcast(mini.deserialize(str(s, "message", "")));
				case BOSS_BAR -> {
					if (!screenEffects(p))
						break;
					BossBar bar = BossBar.bossBar(mini.deserialize(str(s, "title", "DragonAltar")),
							Math.max(0, Math.min(1, num(s, "progress", 1))), BossBar.Color.PURPLE,
							BossBar.Overlay.PROGRESS);
					Collection<? extends Player> viewers = p == null
							? List.copyOf(Bukkit.getOnlinePlayers())
							: List.of(p);
					activeBossBars.add(bar);
					viewers.forEach(v -> v.showBossBar(bar));
					BukkitTask cleanup = Bukkit.getScheduler().runTaskLater(plugin, () -> {
						viewers.forEach(v -> v.hideBossBar(bar));
						activeBossBars.remove(bar);
					}, (long) num(s, "duration", 60));
					sessions.computeIfPresent(session, (id, list) -> {
						list.add(cleanup);
						return list;
					});
				}
				case PLAYER_GLOW -> {
					if (p != null && screenEffects(p)) {
						p.setGlowing(true);
						glowingPlayers.add(p.getUniqueId());
						BukkitTask cleanup = Bukkit.getScheduler().runTaskLater(plugin, () -> {
							p.setGlowing(false);
							glowingPlayers.remove(p.getUniqueId());
						}, (long) num(s, "duration", 40));
						sessions.computeIfPresent(session, (id, list) -> {
							list.add(cleanup);
							return list;
						});
					}
				}
				case PLAYER_LEVITATE -> {
					if (p != null && screenEffects(p))
						p.addPotionEffect(
								new PotionEffect(PotionEffectType.LEVITATION, (int) num(s, "duration", 40), 0));
				}
				case TEMPORARY_WINGS -> {
					if (p != null && animationParticles(p)) {
						long duration = Math.max(1, (long) num(s, "duration", 80));
						BukkitTask wings = new org.bukkit.scheduler.BukkitRunnable() {
							long elapsed;
							@Override
							public void run() {
								if (!p.isOnline() || elapsed >= duration) {
									cancel();
									return;
								}
								drawWings(p);
								elapsed += 4;
							}
						}.runTaskTimer(plugin, 0, 4);
						sessions.computeIfPresent(session, (id, list) -> {
							list.add(wings);
							return list;
						});
					}
				}
				case DISPLAY_MOVE -> {
					for (Display display : displays(at)) {
						display.setInterpolationDuration((int) num(s, "duration", 20));
						display.teleport(at.clone().add(num(s, "x", 0), num(s, "y", 0), num(s, "z", 0)));
					}
				}
				case DISPLAY_SCALE -> {
					float scale = num(s, "scale", 1);
					for (Display display : displays(at)) {
						var transform = display.getTransformation();
						transform.getScale().set(scale);
						display.setInterpolationDuration((int) num(s, "duration", 20));
						display.setTransformation(transform);
					}
				}
				case DISPLAY_ROTATE -> {
					float radians = (float) Math.toRadians(num(s, "degrees", 360));
					for (Display display : displays(at)) {
						var transform = display.getTransformation();
						transform.getLeftRotation().set(new AxisAngle4f(radians, 0, 1, 0));
						display.setInterpolationDuration((int) num(s, "duration", 20));
						display.setTransformation(transform);
					}
				}
				case DISPLAY_FADE -> {
					for (Display display : displays(at)) {
						display.setViewRange(Math.max(.01f, num(s, "view-range", .01)));
					}
				}
				case WAIT -> {
				}
			}
		} catch (RuntimeException ex) {
			plugin.getLogger().warning("Animation action " + s.type() + " failed: " + ex.getMessage());
		}
	}
	private List<Display> displays(Location origin) {
		NamespacedKey eggKey = new NamespacedKey(plugin, "altar_egg");
		return origin.getWorld().getNearbyEntitiesByType(BlockDisplay.class, origin, 4).stream()
				.filter(display -> display.getPersistentDataContainer().has(eggKey, PersistentDataType.BYTE))
				.map(Display.class::cast).toList();
	}
	private static String str(AnimationStep s, String key, String fallback) {
		return String.valueOf(s.options().getOrDefault(key, fallback));
	}
	private static float num(AnimationStep s, String key, double fallback) {
		try {
			return Float.parseFloat(String.valueOf(s.options().getOrDefault(key, fallback)));
		} catch (NumberFormatException e) {
			return (float) fallback;
		}
	}
	private static long actionDuration(AnimationStep step) {
		return switch (step.type()) {
			case TEMPORARY_WINGS -> (long) num(step, "duration", 80);
			case BOSS_BAR -> (long) num(step, "duration", 60);
			case PLAYER_GLOW -> (long) num(step, "duration", 40);
			default -> 0;
		};
	}
	private int scaledCount(Player player, int configured) {
		int bounded = Math.max(1, Math.min(512, configured));
		if (player == null)
			return bounded;
		EffectMode mode = plugin.players().settings(player.getUniqueId()).effects();
		double factor = switch (mode) {
			case FULL -> 1;
			case REDUCED -> .5;
			case MINIMAL -> .15;
		};
		return Math.max(1, (int) Math.round(bounded * factor));
	}
	private boolean animationParticles(Player player) {
		return player == null || plugin.players().settings(player.getUniqueId()).animationParticles();
	}
	private boolean screenEffects(Player player) {
		return player == null || plugin.players().settings(player.getUniqueId()).screenEffects();
	}
	private void drawWings(Player player) {
		Location center = player.getLocation().add(0, 1.15, 0);
		org.bukkit.util.Vector forward = center.getDirection().setY(0);
		if (forward.lengthSquared() < .01)
			forward.setZ(1);
		forward.normalize();
		org.bukkit.util.Vector right = new org.bukkit.util.Vector(-forward.getZ(), 0, forward.getX());
		int points = scaledCount(player, 18);
		for (int side : new int[]{-1, 1})
			for (int i = 0; i < points; i++) {
				double progress = i / (double) Math.max(1, points - 1), span = Math.sin(progress * Math.PI) * 1.7,
						height = .7 - progress * .9;
				Location point = center.clone().add(right.clone().multiply(side * span))
						.subtract(forward.clone().multiply(.35 + progress * .45)).add(0, height, 0);
				spawnParticle(player, Particle.DRAGON_BREATH, point, 1, 0, 0, 0, 0);
			}
	}
	private static Object particleData(Particle particle) {
		return particle.getDataType() == Float.class ? 1.0f : null;
	}
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void spawnParticle(World world, Particle particle, Location at, int count, double x, double y,
			double z, double extra) {
		world.spawnParticle(particle, at, count, x, y, z, extra, particleData(particle));
	}
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void spawnParticle(Player player, Particle particle, Location at, int count, double x, double y,
			double z, double extra) {
		player.spawnParticle(particle, at, count, x, y, z, extra, particleData(particle));
	}
}
