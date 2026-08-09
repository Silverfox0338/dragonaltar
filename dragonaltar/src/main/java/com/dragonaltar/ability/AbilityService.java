package com.dragonaltar.ability;

import com.dragonaltar.compat.ServerAttributes;

import com.dragonaltar.ability.akuma.AbsoluteZero;
import com.dragonaltar.ability.akuma.AkumasHush;
import com.dragonaltar.ability.akuma.AkumasTrail;
import com.dragonaltar.ability.lamari.LamarisFault;
import com.dragonaltar.ability.lamari.LamarisReckoning;
import com.dragonaltar.ability.lamari.TitansBulwark;
import com.dragonaltar.ability.resonance.DragonTrinity;
import com.dragonaltar.ability.resonance.GlacialBastion;
import com.dragonaltar.ability.resonance.ThermalConvergence;
import com.dragonaltar.ability.resonance.VolcanicAegis;
import com.dragonaltar.ability.rev.InfernosWrath;
import com.dragonaltar.ability.rev.RevHeatBarManager;
import com.dragonaltar.ability.rev.RevsRend;
import com.dragonaltar.ability.rev.WrathOfRev;
import com.dragonaltar.ability.shared.Roar;
import com.dragonaltar.ability.shared.Wings;
import com.dragonaltar.dragonborn.DragonbornService;
import com.dragonaltar.api.event.DragonAbilityCastEvent;
import com.dragonaltar.api.event.DragonAbilitySelectEvent;
import com.dragonaltar.config.ConfigService;
import com.dragonaltar.player.PlayerDataService;
import com.dragonaltar.DragonAltarPlugin;
import com.dragonaltar.soul.SoulIdentity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

public final class AbilityService {
	private static final double MAX_TARGET_RADIUS = 64.0;
	private static final int MAX_TARGETS_PER_QUERY = 128;
	private final DragonAltarPlugin plugin;
	private final DragonbornService dragonborn;
	private final ConfigService config;
	private final PlayerDataService players;
	private final Map<String, DragonAbility> registry = new LinkedHashMap<>();
	private final AbilityEnergyManager energy;
	private final AbilitySelectionManager selections;
	private final AbilityCooldownTracker cooldowns;
	private final ResonanceState resonances = new ResonanceState();
	private final AbilityResonanceCoordinator resonanceCoordinator;
	private final AbilityDisplayTracker displays;
	private final TemporaryTerrainTracker terrain = new TemporaryTerrainTracker();
	private final TemporaryFlightManager temporaryFlight = new TemporaryFlightManager();
	private final BulwarkTracker bulwarks = new BulwarkTracker();
	private final AbilityCombatRules.BrittleTracker brittle = new AbilityCombatRules.BrittleTracker();
	private final AbilityCombatRules.RevHuntTracker revHunt = new AbilityCombatRules.RevHuntTracker();
	private final RevHeatBarManager revHeatBars;
	private final AbilityPresentation presentation;
	private final AbilityCombatRules.ReflectionGuard reflectionGuard = new AbilityCombatRules.ReflectionGuard();
	private boolean suppressCombatInteractions;
	private BukkitTask task;
	private final MiniMessage mini = MiniMessage.miniMessage();

	public AbilityService(DragonAltarPlugin plugin, DragonbornService dragonborn, ConfigService config,
			PlayerDataService players) {
		this.plugin = plugin;
		this.dragonborn = dragonborn;
		this.config = config;
		this.players = players;
		this.energy = new AbilityEnergyManager(config, players);
		this.selections = new AbilitySelectionManager(players);
		this.cooldowns = new AbilityCooldownTracker(players);
		this.displays = new AbilityDisplayTracker(plugin, players);
		this.revHeatBars = new RevHeatBarManager(config);
		this.presentation = new AbilityPresentation(config, players, plugin.getLogger());
		this.resonanceCoordinator = new AbilityResonanceCoordinator(plugin, dragonborn, config, resonances,
				player -> playAccessibleSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, .8f, 1.35f));
		registerDefaults();
	}
	public void start() {
		if (task != null && !task.isCancelled())
			return;
		long interval = config.file("abilities.yml").getLong("energy.regeneration-interval-ticks", 20);
		task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			resonanceCoordinator.refreshUnlocks();
			int max = maxEnergy(), baseRegen = config.file("abilities.yml").getInt("energy.regeneration", 2);
			for (Player p : Bukkit.getOnlinePlayers())
				if (dragonborn.isDragonborn(p.getUniqueId())) {
					dragonborn.refreshEnvironmentalPassives(p);
					int regen = baseRegen;
					if (dragonborn.hasSoul(p, SoulIdentity.REV)
							&& (p.getWorld().getEnvironment() == World.Environment.NETHER
									|| p.getWorld().getEnvironment() == World.Environment.THE_END))
						regen = (int) Math.ceil(regen * config.file("abilities.yml")
								.getDouble("named-souls.rev.dimension-energy-multiplier", 1.5));
					if (dragonborn.hasSoul(p, SoulIdentity.REV)) {
						revHunt.decayHeat(p.getUniqueId(), System.currentTimeMillis(),
								integerRoot("rev-hunt.heat.decay-delay-ticks", 100) * 50L,
								integerRoot("rev-hunt.heat.decay-interval-ticks", 20) * 50L,
								integerRoot("rev-hunt.heat.decay-amount", 2));
						showRevTracking(p, System.currentTimeMillis());
						updateRevHeatBar(p);
					} else {
						revHeatBars.hide(p);
					}
					if (current(p) < max && energy.regenerationAllowed(p.getUniqueId(), System.currentTimeMillis()))
						energy.set(p, Math.min(max, current(p) + regen), false);
					String selectedId = selected(p);
					DragonAbility selectedAbility = registry.get(selectedId);
					long cooldown = effectiveCooldownSeconds(p, selectedAbility);
					long ultimateCooldown = ultimateCooldownSeconds(p);
					Optional<DragonResonance> resonance = resonanceCoordinator.current(p);
					long resonanceCooldown = resonance
							.map(value -> effectiveCooldownSeconds(p, registry.get(value.id()))).orElse(0L);
					String abilityName = selectedAbility == null
							? selectedId
							: PlainTextComponentSerializer.plainText().serialize(selectedAbility.displayName());
					if (players.settings(p.getUniqueId()).hud())
						p.sendActionBar(plugin.messages().component("energy-hud", "energy",
								Integer.toString(current(p)), "maximum", Integer.toString(max), "ability", abilityName,
								"cooldown", formatCooldown(cooldown), "ultimate_cooldown",
								formatCooldown(ultimateCooldown), "resonance",
								resonance.map(DragonResonance::displayName).orElse("Locked"), "resonance_cooldown",
								resonance.isEmpty() ? "Locked" : formatCooldown(resonanceCooldown), "status",
								combatStatus(p)));
					if (config.file("abilities.yml").getBoolean("passives.particles", true)
							&& players.settings(p.getUniqueId()).passiveParticles()
							&& players.settings(p.getUniqueId()).effects() != com.dragonaltar.player.EffectMode.MINIMAL)
						passiveParticleAccessible(p.getLocation().add(0, 1, 0), Particle.DRAGON_BREATH,
								players.settings(p.getUniqueId()).effects() == com.dragonaltar.player.EffectMode.FULL
										? 3
										: 1,
								.35, .6, .35, 0);
				}
		}, interval, interval);
	}
	public void stop() {
		if (task != null)
			task.cancel();
		task = null;
		terrain.revertAll();
		displays.removeAll();
		temporaryFlight.restoreAll();
		brittle.clear();
		revHunt.clear();
		bulwarks.clear();
		resonances.clear();
		revHeatBars.hideAll();
		energy.persistAllAndClear();
		selections.clear();
		cooldowns.clear();
	}
	public Collection<DragonAbility> abilities() {
		return Collections.unmodifiableCollection(registry.values());
	}
	public int current(Player p) {
		return energy.current(p);
	}
	public int maxEnergy() {
		return energy.maximum();
	}
	public void setEnergy(Player p, int value) {
		energy.set(p, value, true);
	}
	public Collection<DragonAbility> abilities(Player player) {
		return available(player);
	}
	public String selected(Player p) {
		String value = selections.selected(p.getUniqueId(), "wings");
		DragonAbility chosen = registry.get(value);
		if (chosen == null || !supports(p, chosen)) {
			value = available(p).getFirst().id();
			selections.select(p.getUniqueId(), value);
		}
		return value;
	}
	public void select(Player p, String id) {
		if (available(p).stream().anyMatch(ability -> ability.id().equals(id))) {
			DragonAbilitySelectEvent event = new DragonAbilitySelectEvent(p, id);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled())
				selections.select(p.getUniqueId(), id);
		}
	}
	public void cycle(Player p, int direction) {
		List<String> ids = available(p).stream().map(DragonAbility::id).toList();
		int old = ids.indexOf(selected(p));
		select(p, ids.get(Math.floorMod(old + direction, ids.size())));
	}
	public void cycleCategory(Player p) {
		DragonAbility current = registry.get(selected(p));
		AbilityCategory[] categories = AbilityCategory.values();
		for (int offset = 1; offset <= categories.length; offset++) {
			AbilityCategory next = categories[(current.category().ordinal() + offset) % categories.length];
			Optional<DragonAbility> candidate = available(p).stream().filter(a -> a.category() == next).findFirst();
			if (candidate.isPresent()) {
				select(p, candidate.get().id());
				return;
			}
		}
	}
	public AbilityResult cast(Player p) {
		if (!p.hasPermission("dragonaltar.use") || !dragonborn.isDragonborn(p.getUniqueId()))
			return AbilityResult.fail("ability-not-dragonborn");
		if (!p.isOnline() || p.isDead() || p.getGameMode() == GameMode.SPECTATOR)
			return AbilityResult.fail("ability-unavailable");
		DragonAbility ability = registry.get(selected(p));
		if (ability == null || !supports(p, ability))
			return AbilityResult.fail("Unknown ability");
		DragonAbilityCastEvent apiEvent = new DragonAbilityCastEvent(p, ability.id());
		Bukkit.getPluginManager().callEvent(apiEvent);
		if (apiEvent.isCancelled())
			return AbilityResult.fail("ability-cancelled");
		if (ability.id().equals("revs-rend") && revHunt.recastAvailable(p.getUniqueId(), System.currentTimeMillis()))
			return revsRendRecast(p);
		Map<String, Long> playerCooldowns = cooldowns.mutable(p.getUniqueId());
		long ready = playerCooldowns.getOrDefault(ability.id(), 0L);
		if (ready > System.currentTimeMillis())
			return AbilityResult.fail("ability-cooldown");
		if (ability.ultimate()
				&& cooldowns.active(p.getUniqueId(), AbilityCooldownTracker.ULTIMATE_GROUP, System.currentTimeMillis()))
			return AbilityResult.fail("ability-ultimate-cooldown");
		if (resonances.isResonance(ability.id()) && cooldowns.active(p.getUniqueId(),
				AbilityCooldownTracker.RESONANCE_GROUP, System.currentTimeMillis()))
			return AbilityResult.fail("ability-resonance-cooldown");
		int requiredEnergy = ability.ultimate() ? maxEnergy() : ability.energyCost();
		if (current(p) < requiredEnergy) {
			if (players.settings(p.getUniqueId()).hud())
				p.sendActionBar(mini.deserialize(
						"<red>Not enough Dragon Energy! <gray>" + current(p) + "/" + requiredEnergy + "</gray>"));
			if (players.settings(p.getUniqueId()).sounds())
				p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, .8f, .6f);
			return AbilityResult.fail(ability.ultimate() ? "ability-full-energy" : "ability-energy");
		}
		AbilityResult can = ability.canUse(new AbilityContext(p, this));
		if (!can.success())
			return can;
		AbilityResult result = ability.activate(new AbilityContext(p, this));
		if (result.success()) {
			setEnergy(p, current(p) - requiredEnergy);
			long now = System.currentTimeMillis();
			cooldowns.start(p.getUniqueId(), ability.id(), now + ability.cooldownMillis());
			if (ability.ultimate())
				cooldowns.start(p.getUniqueId(), AbilityCooldownTracker.ULTIMATE_GROUP,
						now + integerRoot("ultimate.shared-cooldown-seconds", 120) * 1000L);
			if (resonances.isResonance(ability.id()))
				startResonanceCooldown(p, ability.id(), now + ability.cooldownMillis());
			cooldowns.persist(p.getUniqueId());
			energy.blockRegeneration(p.getUniqueId(), System.currentTimeMillis()
					+ config.file("abilities.yml").getLong("energy.delay-after-cast-ticks", 60) * 50L);
		}
		return result;
	}
	public Map<String, Long> cooldowns(Player p) {
		return cooldowns.view(p);
	}
	public long cooldownSeconds(Player p, String ability) {
		long own = rawCooldownSeconds(p, ability);
		DragonAbility registered = registry.get(ability);
		if (registered != null && registered.ultimate())
			return Math.max(own, ultimateCooldownSeconds(p));
		if (resonances.isResonance(ability))
			return Math.max(own, resonanceGroupCooldownSeconds(p, ability));
		return own;
	}
	private long rawCooldownSeconds(Player p, String ability) {
		return cooldowns.remainingSeconds(p.getUniqueId(), ability, System.currentTimeMillis());
	}
	public long ultimateCooldownSeconds(Player p) {
		return rawCooldownSeconds(p, AbilityCooldownTracker.ULTIMATE_GROUP);
	}
	private long effectiveCooldownSeconds(Player p, DragonAbility ability) {
		if (ability == null)
			return 0;
		long own = rawCooldownSeconds(p, ability.id());
		if (ability.ultimate())
			return Math.max(own, ultimateCooldownSeconds(p));
		if (resonances.isResonance(ability.id()))
			return Math.max(own, resonanceGroupCooldownSeconds(p, ability.id()));
		return own;
	}
	public void clearCooldowns(Player p, String ability) {
		DragonAbility selectedAbility = registry.get(ability);
		cooldowns.clear(p, ability, selectedAbility != null && selectedAbility.ultimate(),
				resonances.isResonance(ability));
	}
	public void clearCache(Player player) {
		UUID id = player.getUniqueId();
		energy.remove(id);
		selections.remove(id);
		cooldowns.remove(id);
		bulwarks.remove(id);
		resonances.remove(id);
		resetCombatState(player);
	}
	public void handleLogout(Player player) {
		UUID id = player.getUniqueId();
		energy.persistAndRemove(id);
		temporaryFlight.restoreOnLogout(player);
		selections.remove(id);
		cooldowns.remove(id);
		bulwarks.remove(id);
		resonances.remove(id);
		resetCombatState(player);
		revHeatBars.hide(player);
	}
	public void clearCaches() {
		energy.clear();
		selections.clear();
		cooldowns.clear();
		brittle.clear();
		revHunt.clear();
		bulwarks.clear();
		resonances.clear();
		revHeatBars.hideAll();
	}
	public void resetCombatState(Player player) {
		revHunt.reset(player.getUniqueId());
		updateRevHeatBar(player);
	}
	private boolean supports(Player player, DragonAbility ability) {
		if (!dragonborn.soul(player).map(ability::supports).orElse(false))
			return false;
		return !resonances.isResonance(ability.id())
				|| resonanceCoordinator.current(player).map(value -> value.id().equals(ability.id())).orElse(false);
	}
	private List<DragonAbility> available(Player player) {
		return registry.values().stream().filter(ability -> supports(player, ability)).toList();
	}
	private void registerDefaults() {
		register(new Wings(this));
		register(new Roar(this));
		register(new AkumasTrail(this));
		register(new AkumasHush(this));
		register(new AbsoluteZero(this));
		register(new RevsRend(this));
		register(new WrathOfRev(this));
		register(new InfernosWrath(this));
		register(new LamarisFault(this));
		register(new LamarisReckoning(this));
		register(new TitansBulwark(this));
		registerResonance(new ThermalConvergence(this));
		registerResonance(new VolcanicAegis(this));
		registerResonance(new GlacialBastion(this));
		registerResonance(new DragonTrinity(this));
	}
	private void register(DragonAbility ability) {
		registry.put(ability.id(), ability);
	}
	/**
	 * Public API bridge. Add-ons should call DragonAltarApi rather than this
	 * implementation method.
	 */
	public void registerExternal(DragonAbility ability) {
		Objects.requireNonNull(ability, "ability");
		if (registry.containsKey(ability.id()))
			throw new IllegalArgumentException("Ability id is already registered: " + ability.id());
		registry.put(ability.id(), ability);
	}

	/**
	 * Removes only the supplied external ids. Built-in ids are never supplied by
	 * the public API registry.
	 */
	public void unregisterExternal(Collection<String> abilityIds) {
		abilityIds.forEach(registry::remove);
	}
	private void registerResonance(DragonAbility ability) {
		resonances.register(ability.id());
		register(ability);
	}

	public void activateWings(Player player) {
		boolean previous = temporaryFlight.enable(player);
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			temporaryFlight.finish(player, previous);
			if (player.isOnline())
				player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0));
		}, integer("wings.duration-seconds", 8) * 20L);
	}
	public void activateRoar(Player player) {
		playAccessibleSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 1f);
		double radius = decimal("roar.radius", 8);
		for (LivingEntity living : nearbyLiving(player.getLocation(), radius, player)) {
			Vector push = living.getLocation().toVector().subtract(player.getLocation().toVector()).normalize()
					.multiply(1.2).setY(.35);
			living.setVelocity(push);
			living.addPotionEffect(
					new PotionEffect(PotionEffectType.WEAKNESS, integer("roar.weakness-seconds", 5) * 20, 0));
		}
	}
	public void activateAkumasTrail(Player player) {
		akumasTrail(player);
	}
	public void activateAkumasHush(Player player) {
		akumasHush(player);
	}
	public void activateAbsoluteZero(Player player) {
		absoluteZero(player);
	}
	public void activateRevsRend(Player player) {
		revsRend(player);
	}
	public void activateWrathOfRev(Player player) {
		wrathOfRev(player);
	}
	public void activateInfernosWrath(Player player) {
		infernosWrath(player);
	}
	public void activateLamarisFault(Player player) {
		lamarisFault(player);
	}
	public void activateLamarisReckoning(Player player) {
		lamarisReckoning(player);
	}
	public void activateTitansBulwark(Player player) {
		titansBulwark(player);
	}
	public boolean grounded(Player player) {
		return isGrounded(player);
	}
	public int abilityInteger(String path, int fallback) {
		return integer(path, fallback);
	}
	public Component abilityName(String id, String fallback) {
		return mini.deserialize(label(id, fallback));
	}
	public Component resonanceName(String id, String fallback) {
		return mini.deserialize(resonanceLabel(id, fallback));
	}
	public int resonanceEnergy(DragonResonance resonance) {
		return resonanceInteger(resonance.id() + ".energy", resonance == DragonResonance.DRAGON_TRINITY ? 100 : 70);
	}
	public int resonanceCooldownSeconds(DragonResonance resonance) {
		return resonanceInteger(resonance.id() + ".cooldown-seconds", 720);
	}
	public AbilityResult canUseResonance(Player player, DragonResonance resonance) {
		List<Player> participants = resonanceCoordinator.participants(player, resonance);
		if (participants.size() != resonance.souls().size())
			return AbilityResult.fail("ability-resonance-lost");
		return participants.stream()
				.anyMatch(member -> rawCooldownSeconds(member, AbilityCooldownTracker.RESONANCE_GROUP) > 0)
						? AbilityResult.fail("ability-resonance-cooldown")
						: AbilityResult.ok();
	}
	public void activateConfiguredResonance(Player player, DragonResonance resonance) {
		activateResonance(player, resonance);
	}
	private void akumasTrail(Player player) {
		if (hasNearbyWater(player.getLocation(), integer("akumas-trail.water-detection-radius", 5))) {
			armWaterRun(player);
			return;
		}
		Vector direction = player.getLocation().getDirection().setY(0).normalize();
		double distance = decimal("akumas-trail.distance", 10);
		player.setVelocity(direction.clone().multiply(decimal("akumas-trail.dash-strength", 2.35)).setY(.14));
		List<BlockDisplay> ice = new ArrayList<>();
		Location start = player.getLocation();
		for (double step = 0; step <= distance; step += .75) {
			Location point = start.clone().add(direction.clone().multiply(step));
			Location visual = point.getBlock().getRelative(0, -1, 0).getLocation().add(0, 1.01, 0);
			BlockDisplay display = point.getWorld().spawn(visual, BlockDisplay.class, entity -> {
				entity.setBlock(Material.PACKED_ICE.createBlockData());
				entity.setPersistent(false);
				entity.setViewRange(24);
				entity.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1, .08f, 1),
						new AxisAngle4f()));
			});
			displays.track(display);
			ice.add(display);
			particleAccessible(point.clone().add(0, .2, 0), Particle.SNOWFLAKE, 4, .25, .15, .25, .02);
			for (LivingEntity living : nearbyLiving(point, 1.7, player)) {
				int amplifier = brittle.active(living.getUniqueId(), System.currentTimeMillis())
						? integer("akumas-trail.brittle-slowness-amplifier", 2)
						: 1;
				living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, amplifier));
			}
		}
		long duration = integer("akumas-trail.ice-duration-seconds", 6) * 20L;
		Bukkit.getScheduler().runTaskLater(plugin, () -> displays.remove(ice), duration);
	}
	private boolean hasNearbyWater(Location center, int radius) {
		Block origin = center.getBlock();
		for (int x = -radius; x <= radius; x++)
			for (int y = -2; y <= 2; y++)
				for (int z = -radius; z <= radius; z++)
					if (x * x + z * z <= radius * radius && origin.getRelative(x, y, z).getType() == Material.WATER)
						return true;
		return false;
	}
	private boolean touchesWater(Player player) {
		Block feet = player.getLocation().getBlock();
		return feet.getType() == Material.WATER || feet.getRelative(0, -1, 0).getType() == Material.WATER;
	}
	private void armWaterRun(Player player) {
		int entryTicks = integer("akumas-trail.water-entry-seconds", 3) * 20;
		if (players.settings(player.getUniqueId()).hud())
			player.sendActionBar(mini.deserialize("<aqua>Touch the water within 3 seconds to begin Frost Run!</aqua>"));
		new BukkitRunnable() {
			int ticks;
			public void run() {
				if (!player.isOnline() || player.isDead() || ticks++ >= entryTicks) {
					cancel();
					return;
				}
				particleAccessible(player.getLocation().add(0, .2, 0), Particle.SNOWFLAKE, 3, .25, .1, .25, .01);
				if (touchesWater(player)) {
					startWaterRun(player);
					cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 1);
	}
	private void startWaterRun(Player player) {
		int durationTicks = integer("akumas-trail.water-run-seconds", 8) * 20;
		int radius = integer("akumas-trail.water-freeze-radius", 2);
		Map<Block, BlockData> frozen = new HashMap<>();
		player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks,
				integer("akumas-trail.water-run-speed-amplifier", 2), false, true, true));
		playAccessibleSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1.65f);
		new BukkitRunnable() {
			int ticks;
			public void run() {
				if (!player.isOnline() || player.isDead() || ticks++ >= durationTicks) {
					terrain.revert(frozen, Material.FROSTED_ICE);
					cancel();
					return;
				}
				Block feet = player.getLocation().getBlock();
				Block surface = feet.getType() == Material.WATER ? feet : feet.getRelative(0, -1, 0);
				for (int x = -radius; x <= radius; x++)
					for (int z = -radius; z <= radius; z++) {
						if (x * x + z * z > radius * radius)
							continue;
						Block water = surface.getRelative(x, 0, z);
						if (water.getType() == Material.WATER && water.getRelative(0, 1, 0).isEmpty()) {
							if (!frozen.containsKey(water)) {
								BlockData original = water.getBlockData().clone();
								frozen.put(water, original);
								terrain.track(water, original, Material.FROSTED_ICE);
							}
							water.setType(Material.FROSTED_ICE, false);
						}
					}
				particleAccessible(player.getLocation().add(0, .1, 0), Particle.SNOWFLAKE, 6, .45, .08, .45, .02);
			}
		}.runTaskTimer(plugin, 0, 1);
	}
	private void akumasHush(Player player) {
		Location center = player.getLocation().clone();
		int duration = integer("akumas-hush.duration-seconds", 10) * 20;
		double radius = decimal("akumas-hush.radius", 9);
		new BukkitRunnable() {
			int elapsed;
			public void run() {
				if (elapsed >= duration || center.getWorld() == null) {
					cancel();
					return;
				}
				particleAccessible(center.clone().add(0, .5, 0), Particle.SNOWFLAKE, 28, radius * .55, .5, radius * .55,
						.02);
				long now = System.currentTimeMillis();
				for (LivingEntity living : nearbyLiving(center, radius, player)) {
					living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 2));
					if (brittle.active(living.getUniqueId(), now))
						brittle.extend(living.getUniqueId(), now,
								integer("akumas-hush.brittle-extension-ticks", 10) * 50L,
								now + integer("akumas-hush.brittle-maximum-remaining-seconds", 4) * 1000L);
				}
				elapsed += 10;
			}
		}.runTaskTimer(plugin, 0, 10);
	}
	private void revsRend(Player player) {
		long now = System.currentTimeMillis();
		revHunt.armRecast(player.getUniqueId(), now);
		Vector direction = player.getEyeLocation().getDirection().normalize();
		player.setVelocity(direction.clone().multiply(decimal("revs-rend.dash-strength", 1.65))
				.add(new Vector(0, decimal("revs-rend.vertical-lift", .08), 0)));
		Location start = player.getLocation().clone();
		Set<UUID> crossed = new HashSet<>();
		int maximumTicks = Math.max(1, Math.min(40, integer("revs-rend.maximum-dash-ticks", 16)));
		double maximumDistance = Math.max(1, Math.min(16, decimal("revs-rend.maximum-distance", 9)));
		double crossRadius = Math.max(.25, Math.min(3, decimal("revs-rend.cross-detection-radius", 1.35)));
		new BukkitRunnable() {
			int ticks;
			Location previous = start;
			boolean recastAnnounced;
			public void run() {
				if (!player.isOnline() || player.isDead() || !dragonborn.hasSoul(player, SoulIdentity.REV)
						|| ticks++ >= maximumTicks || !player.getWorld().equals(start.getWorld())
						|| player.getLocation().distanceSquared(start) > maximumDistance * maximumDistance) {
					cancel();
					return;
				}
				Location current = player.getLocation().clone();
				particleAccessible(current.clone().add(0, .7, 0), Particle.SMALL_FLAME,
						integer("revs-rend.presentation.dash-particles", 5), .2, .35, .2, .015);
				for (LivingEntity living : nearbyLiving(current, crossRadius, player)) {
					if (crossed.contains(living.getUniqueId()) || !validRevPrey(player, living)
							|| distanceSquaredToSegment(living.getLocation().toVector(), previous.toVector(),
									current.toVector()) > crossRadius * crossRadius)
						continue;
					crossed.add(living.getUniqueId());
					applyInfernoMark(player, living, System.currentTimeMillis());
					grantHeat(player, living, integer("revs-rend.heat-per-cross", 8), System.currentTimeMillis());
					living.setFireTicks(Math.max(living.getFireTicks(), integer("revs-rend.pressure-fire-ticks", 30)));
					particleAccessible(living.getLocation().add(0, 1, 0), Particle.FLAME,
							integer("revs-rend.presentation.cross-particles", 12), .3, .6, .3, .03);
					if (!recastAnnounced) {
						long expiry = System.currentTimeMillis()
								+ Math.max(1, Math.min(100, integer("revs-rend.recast-window-ticks", 36))) * 50L;
						revHunt.armRecast(player.getUniqueId(), expiry);
						recastAnnounced = true;
						if (players.settings(player.getUniqueId()).hud())
							player.sendActionBar(mini.deserialize(
									"<gold>Rend Recast Ready</gold> <gray>Aim at Inferno Marked prey</gray>"));
						playConfiguredSound(player.getLocation(), "revs-rend.sounds.recast-ready",
								Sound.ITEM_FIRECHARGE_USE, .75f, 1.3f);
					}
				}
				previous = current;
			}
		}.runTaskTimer(plugin, 1, 1);
	}

	private AbilityResult revsRendRecast(Player player) {
		long now = System.currentTimeMillis();
		LivingEntity target = aimedMarkedTarget(player,
				Math.max(1, Math.min(24, decimal("revs-rend.recast-range", 14))),
				decimal("revs-rend.recast-aim-cone-degrees", 24), now);
		if (target == null) {
			if (players.settings(player.getUniqueId()).hud())
				player.sendActionBar(mini.deserialize(
						"<red>Rend recast failed:</red> <gray>Aim at visible Inferno Marked prey in range</gray>"));
			playConfiguredSound(player.getLocation(), "revs-rend.sounds.recast-failed", Sound.BLOCK_NOTE_BLOCK_BASS,
					.65f, .7f);
			return AbilityResult.fail("Rend recast needs a visible Inferno Marked enemy in range.");
		}
		if (!revHunt.consumeRecast(player.getUniqueId(), now))
			return AbilityResult.fail("Rend recast expired.");
		Vector toward = target.getEyeLocation().toVector().subtract(player.getEyeLocation().toVector());
		double distance = Math.max(.01, toward.length());
		player.setVelocity(
				toward.normalize().multiply(Math.min(Math.min(2, decimal("revs-rend.recast-surge-strength", 1.45)),
						distance / Math.max(1, integer("revs-rend.recast-travel-ticks", 8)))));
		particleFlameArc(player.getLocation().add(0, 1, 0), target.getLocation().add(0, 1, 0),
				integer("revs-rend.presentation.recast-arc-particles", 14));
		playConfiguredSound(player.getLocation(), "revs-rend.sounds.recast-surge", Sound.ENTITY_BLAZE_SHOOT, .9f,
				1.15f);
		boolean claim = revHunt.consumeFinisher(player.getUniqueId(), now, true,
				config.file("abilities.yml").getBoolean("rev-hunt.finisher.reset-heat-on-consume", true));
		updateRevHeatBar(player);
		if (claim) {
			dealAbilityDamage(target, Math.min(8, decimal("revs-rend.predators-claim-damage", 6)), player);
			target.setVelocity(
					target.getVelocity().add(new Vector(0, decimal("revs-rend.predators-claim-lift", .22), 0)));
			particleFlameArc(player.getEyeLocation(), target.getEyeLocation(),
					integer("revs-rend.presentation.finisher-arc-particles", 24));
			playConfiguredSound(target.getLocation(), "revs-rend.sounds.predators-claim",
					Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.45f);
			showTitle(player, "<red><bold>PREDATOR'S CLAIM</bold>", "<gold>The mark is consumed</gold>");
		}
		return AbilityResult.ok();
	}

	private void wrathOfRev(Player player) {
		int pulses = Math.max(1, Math.min(12, integer("wrath-of-rev.pulses", 6)));
		int interval = Math.max(1, integer("wrath-of-rev.pulse-interval-ticks", 16));
		Set<UUID> encountered = new HashSet<>();
		new BukkitRunnable() {
			int pulse;
			public void run() {
				if (!player.isOnline() || player.isDead() || !dragonborn.hasSoul(player, SoulIdentity.REV)
						|| pulse >= pulses) {
					cancel();
					return;
				}
				pulse++;
				Location center = player.getLocation().clone();
				double radius = Math.max(1, Math.min(16, decimal("wrath-of-rev.radius", 7)));
				particleAccessible(center.add(0, 1, 0), Particle.FLAME,
						integer("wrath-of-rev.presentation.pulse-particles", 34), radius * .5, .65, radius * .5, .025);
				playConfiguredSound(player.getLocation(), "wrath-of-rev.sounds.pulse", Sound.ENTITY_BLAZE_SHOOT, .7f,
						.8f + (pulse * .04f));
				long now = System.currentTimeMillis();
				for (LivingEntity living : nearbyLiving(player.getLocation(), radius, player)) {
					if (!validRevPrey(player, living) || !player.hasLineOfSight(living))
						continue;
					boolean firstContact = encountered.add(living.getUniqueId());
					if (firstContact)
						applyInfernoMark(player, living, now);
					if (firstContact && revHunt.markedBy(living.getUniqueId(), player.getUniqueId(), now))
						grantHeat(player, living, integer("wrath-of-rev.heat-per-distinct-target", 6), now);
					dealAbilityDamage(living, Math.min(3, decimal("wrath-of-rev.pressure-damage", 1.5)), player);
					living.setFireTicks(
							Math.max(living.getFireTicks(), integer("wrath-of-rev.pressure-fire-ticks", 30)));
					showMarkedTrailToRev(player, living);
				}
				if (pulse == pulses && revHunt
						.heat(player.getUniqueId()) >= integer("wrath-of-rev.pursuit-surge-heat-threshold", 60))
					wrathPursuitSurge(player, now);
			}
		}.runTaskTimer(plugin, 0, interval);
	}

	private void wrathPursuitSurge(Player player, long now) {
		LivingEntity nearest = nearbyLiving(player.getLocation(),
				Math.max(1, Math.min(24, decimal("wrath-of-rev.pursuit-surge-range", 11))), player)
				.stream()
				.filter(target -> validRevPrey(player, target)
						&& revHunt.markedBy(target.getUniqueId(), player.getUniqueId(), now)
						&& player.hasLineOfSight(target) && clearPath(player, target))
				.min(Comparator.comparingDouble(target -> target.getLocation().distanceSquared(player.getLocation())))
				.orElse(null);
		if (nearest != null) {
			Vector toward = nearest.getLocation().toVector().subtract(player.getLocation().toVector());
			if (toward.lengthSquared() > .01)
				player.setVelocity(
						toward.normalize().multiply(Math.min(1, decimal("wrath-of-rev.pursuit-surge-strength", .65)))
								.setY(Math.min(.4, decimal("wrath-of-rev.pursuit-surge-lift", .12))));
		} else
			player.addPotionEffect(
					new PotionEffect(PotionEffectType.SPEED, integer("wrath-of-rev.fallback-speed-ticks", 30),
							integer("wrath-of-rev.fallback-speed-amplifier", 1), false, true, true));
		playConfiguredSound(player.getLocation(), "wrath-of-rev.sounds.pursuit-surge", Sound.ENTITY_BLAZE_SHOOT, .9f,
				1.4f);
	}
	private void lamarisFault(Player player) {
		player.setFlying(false);
		player.setVelocity(new Vector(0, -decimal("lamaris-fault.slam-speed", 2.4), 0));
		new BukkitRunnable() {
			int ticks;
			public void run() {
				if (!player.isOnline() || ticks++ > 80) {
					cancel();
					return;
				}
				particleAccessible(player.getLocation(), Particle.BLOCK, 5, .3, .3, .3, .02,
						Material.DEEPSLATE.createBlockData());
				if (ticks > 2 && isGrounded(player)) {
					earthPulse(player, decimal("lamaris-fault.radius", 7), false);
					cancel();
				}
			}
		}.runTaskTimer(plugin, 1, 1);
	}
	private void lamarisReckoning(Player player) {
		earthPulse(player, decimal("lamaris-reckoning.radius", 10), true);
	}
	private void earthPulse(Player player, double radius, boolean knockup) {
		Location center = player.getLocation();
		particleAccessible(center, Particle.BLOCK, 90, radius * .5, .35, radius * .5, .08,
				Material.DEEPSLATE.createBlockData());
		playAccessibleSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, .55f);
		for (LivingEntity living : nearbyLiving(center, radius, player)) {
			Vector away = living.getLocation().toVector().subtract(center.toVector());
			if (away.lengthSquared() < .01)
				away = new Vector(1, 0, 0);
			away.normalize().multiply(knockup ? .45 : 1.25).setY(knockup ? 1.15 : .45);
			living.setVelocity(away);
			if (!knockup)
				living.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE,
						integer("lamaris-fault.fatigue-seconds", 8) * 20, 1));
		}
	}
	private void absoluteZero(Player player) {
		Location center = player.getLocation().clone();
		showTitle(player, "<aqua><bold>ABSOLUTE ZERO</bold>", "<gray>The air turns brittle</gray>");
		playAccessibleSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.2f, .45f);
		particleAccessible(center.clone().add(0, 1, 0), Particle.SNOWFLAKE,
				integer("absolute-zero.presentation.anticipation-particles", 32), 1.2, .8, 1.2, .02);
		int impactDelay = integer("absolute-zero.presentation.impact-delay-ticks", 12);
		Bukkit.getScheduler().runTaskLater(plugin, () -> absoluteZeroImpact(player, center), impactDelay);
	}

	private void absoluteZeroImpact(Player player, Location center) {
		if (!player.isOnline() || player.isDead() || !dragonborn.hasSoul(player, SoulIdentity.AKUMA)
				|| center.getWorld() == null)
			return;
		double radius = decimal("absolute-zero.radius", 9);
		int trapTicks = integer("absolute-zero.trap-duration-seconds", 4) * 20;
		int brittleTicks = integer("absolute-zero.brittle-duration-seconds", 6) * 20;
		List<LivingEntity> targets = nearbyLiving(center, radius, player).stream().filter(player::hasLineOfSight)
				.toList();
		playAccessibleSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, .55f);
		playAccessibleSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.1f, .7f);
		particleAccessible(center.clone().add(0, 1, 0), Particle.SNOWFLAKE,
				integer("absolute-zero.presentation.impact-particles", 96), radius * .55, 1.2, radius * .55, .1);
		long brittleExpiry = System.currentTimeMillis() + brittleTicks * 50L;
		for (LivingEntity target : targets) {
			suppressCombatInteractions = true;
			try {
				target.damage(decimal("absolute-zero.burst-damage", 14), player);
			} finally {
				suppressCombatInteractions = false;
			}
			if (!target.isValid() || target.isDead())
				continue;
			brittle.apply(target.getUniqueId(), player.getUniqueId(), brittleExpiry);
			target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, trapTicks,
					integer("absolute-zero.slowness-amplifier", 4), false, true, true));
			target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, trapTicks,
					integer("absolute-zero.mining-fatigue-amplifier", 3), false, true, true));
			if (target instanceof Player affected) {
				if (players.settings(affected.getUniqueId()).screenEffects())
					affected.setFreezeTicks(
							Math.max(affected.getFreezeTicks(), integer("absolute-zero.screen-freeze-ticks", 60)));
				if (players.settings(affected.getUniqueId()).hud())
					affected.sendActionBar(
							mini.deserialize("<aqua>Brittle</aqua> <gray>Your next hit will shatter the frost</gray>"));
			}
			encaseWithIceDisplays(target, trapTicks);
		}
		int dotSeconds = integer("absolute-zero.frost-duration-seconds", 5);
		new BukkitRunnable() {
			int pulse;
			public void run() {
				if (pulse++ >= dotSeconds) {
					playAccessibleSound(center, Sound.BLOCK_GLASS_HIT, .8f, 1.7f);
					particleAccessible(center.clone().add(0, 1, 0), Particle.CLOUD,
							integer("absolute-zero.presentation.ending-particles", 24), radius * .35, .5, radius * .35,
							.01);
					applyUltimateDownside(player, "absolute-zero");
					cancel();
					return;
				}
				for (LivingEntity target : targets)
					if (target.isValid() && !target.isDead()) {
						suppressCombatInteractions = true;
						try {
							target.damage(decimal("absolute-zero.frost-damage", 2), player);
						} finally {
							suppressCombatInteractions = false;
						}
						particleAccessible(target.getLocation().add(0, 1, 0), Particle.SNOWFLAKE,
								integer("absolute-zero.presentation.active-particles-per-target", 6), .3, .5, .3, .02);
					}
			}
		}.runTaskTimer(plugin, 20, 20);
	}
	private void encaseWithIceDisplays(LivingEntity target, int durationTicks) {
		int count = Math.max(4, Math.min(12, integer("absolute-zero.presentation.shell-display-count", 8)));
		double scale = decimal("absolute-zero.presentation.shell-display-scale", .42);
		double radius = decimal("absolute-zero.presentation.shell-orbit-radius", .38);
		List<BlockDisplay> shell = new ArrayList<>();
		for (int index = 0; index < count; index++)
			shell.add(spawnAbilityBlock(iceShellLocation(target, index, count, 0, radius, scale),
					index % 3 == 0 ? Material.BLUE_ICE : Material.PACKED_ICE, scale));
		new BukkitRunnable() {
			int ticks;
			public void run() {
				if (!target.isValid() || target.isDead() || ticks >= durationTicks) {
					if (target.isValid()) {
						particleAccessible(target.getLocation().add(0, 1, 0), Particle.ITEM_SNOWBALL,
								integer("absolute-zero.presentation.shell-break-particles", 20), .4, .7, .4, .12);
						playAccessibleSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, .75f, 1.55f);
					}
					displays.remove(shell);
					cancel();
					return;
				}
				double angle = ticks * decimal("absolute-zero.presentation.shell-rotation-radians-per-tick", .09);
				for (int index = 0; index < shell.size(); index++) {
					BlockDisplay display = shell.get(index);
					if (display.isValid())
						display.teleport(iceShellLocation(target, index, count, angle, radius, scale));
				}
				if (ticks % 10 == 0)
					shell.forEach(displays::refreshVisibility);
				ticks++;
			}
		}.runTaskTimer(plugin, 0, 1);
	}

	private Location iceShellLocation(LivingEntity target, int index, int count, double rotation, double radius,
			double scale) {
		double angle = Math.PI * 2 * index / count + rotation;
		double layer = index % 3;
		double y = .25 + layer * .55 + Math.sin(angle * 2) * .08;
		return target.getLocation().add(Math.cos(angle) * radius - scale / 2, y, Math.sin(angle) * radius - scale / 2);
	}
	private void infernosWrath(Player player) {
		Location center = player.getLocation().clone();
		showTitle(player, "<red><bold>INFERNO'S WRATH</bold>", "<gold>The hunt begins</gold>");
		playConfiguredSound(center, "infernos-wrath.sounds.anticipation", Sound.ENTITY_BLAZE_AMBIENT, 1.2f, .55f);
		particleAccessible(center.clone().add(0, 1, 0), Particle.SMALL_FLAME,
				integer("infernos-wrath.presentation.anticipation-particles", 28), 1, .8, 1, .03);
		Bukkit.getScheduler().runTaskLater(plugin, () -> infernosWrathImpact(player, center),
				integer("infernos-wrath.presentation.impact-delay-ticks", 10));
	}

	private void infernosWrathImpact(Player player, Location center) {
		if (!player.isOnline() || player.isDead() || !dragonborn.hasSoul(player, SoulIdentity.REV)
				|| center.getWorld() == null)
			return;
		double radius = Math.max(1, Math.min(16, decimal("infernos-wrath.radius", 9)));
		int huntTicks = Math.max(20, Math.min(400, integer("infernos-wrath.hunt-duration-ticks", 200)));
		int speedTicks = Math.min(huntTicks, Math.max(1, integer("infernos-wrath.initial-mobility-ticks", 60)));
		int maximumSpeedTicks = Math.min(huntTicks,
				Math.max(speedTicks, integer("infernos-wrath.maximum-mobility-ticks", 140)));
		long now = System.currentTimeMillis();
		revHunt.beginHunt(player.getUniqueId(), now, huntTicks * 50L, speedTicks * 50L, maximumSpeedTicks * 50L);
		List<LivingEntity> targets = nearbyLiving(center, radius, player).stream().filter(player::hasLineOfSight)
				.toList();
		playConfiguredSound(center, "infernos-wrath.sounds.impact", Sound.ENTITY_GENERIC_EXPLODE, 1.25f, .9f);
		particleAccessible(center.clone().add(0, 1, 0), Particle.FLAME,
				integer("infernos-wrath.presentation.impact-particles", 84), radius * .5, 1, radius * .5, .06);
		for (LivingEntity target : targets) {
			if (!validRevPrey(player, target))
				continue;
			dealAbilityDamage(target, Math.min(8, decimal("infernos-wrath.impact-damage", 6)), player);
			if (!target.isValid() || target.isDead())
				continue;
			target.setFireTicks(Math.max(target.getFireTicks(), integer("infernos-wrath.pressure-fire-ticks", 40)));
			applyInfernoMark(player, target, now);
			if (target instanceof Player affected && players.settings(affected.getUniqueId()).hud())
				affected.sendActionBar(mini.deserialize("<red>Inferno Marked</red> <gray>Rev is hunting you</gray>"));
		}
		applyPursuitEffects(player, speedTicks);
		new BukkitRunnable() {
			int ticks;
			public void run() {
				if (!player.isOnline() || player.isDead() || !dragonborn.hasSoul(player, SoulIdentity.REV)) {
					revHunt.reset(player.getUniqueId());
					cancel();
					return;
				}
				if (ticks >= huntTicks) {
					playConfiguredSound(player.getLocation(), "infernos-wrath.sounds.exhaustion",
							Sound.ENTITY_BLAZE_DEATH, 1f, .8f);
					particleAccessible(player.getLocation().add(0, 1, 0), Particle.SMOKE,
							integer("infernos-wrath.presentation.ending-particles", 24), .8, .7, .8, .02);
					if (players.settings(player.getUniqueId()).hud())
						player.sendActionBar(mini.deserialize(
								"<dark_red>Hunt exhausted</dark_red> <gray>Unused Predator's Claim expired</gray>"));
					revHunt.endHunt(player.getUniqueId());
					applyUltimateDownside(player, "infernos-wrath");
					cancel();
					return;
				}
				double ringRadius = radius * Math.min(1,
						ticks / (double) Math.max(1, integer("infernos-wrath.presentation.ring-expand-ticks", 30)));
				int points = Math.max(1, Math.min(integerRoot("presentation.maximum-ring-points", 48),
						integer("infernos-wrath.presentation.ring-points", 32)));
				for (int point = 0; point < points; point++) {
					double angle = Math.PI * 2 * point / points;
					particleAccessible(
							center.clone().add(Math.cos(angle) * ringRadius, .2, Math.sin(angle) * ringRadius),
							point % 8 == 0 ? Particle.LAVA : Particle.FLAME, 1, 0, 0, 0, .01);
				}
				ticks += integer("infernos-wrath.presentation.ring-interval-ticks", 5);
			}
		}.runTaskTimer(plugin, 0, integer("infernos-wrath.presentation.ring-interval-ticks", 5));
	}
	private void titansBulwark(Player player) {
		int durationTicks = integer("titans-bulwark.duration-seconds", 7) * 20;
		bulwarks.activate(player.getUniqueId());
		player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, durationTicks,
				integer("titans-bulwark.resistance-amplifier", 3), false, true, true));
		int activeSlow = integer("titans-bulwark.downside.active-slowness-amplifier", 1);
		if (activeSlow >= 0)
			player.addPotionEffect(
					new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, activeSlow, false, true, true));
		showTitle(player, "<gray><bold>TITAN'S BULWARK</bold>", "<gold>Stand and endure</gold>");
		playAccessibleSound(player.getLocation(), Sound.BLOCK_DEEPSLATE_BRICKS_PLACE, 1.2f, .45f);
		List<BlockDisplay> shell = new ArrayList<>();
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			if (!bulwarks.active(player.getUniqueId()) || !player.isOnline())
				return;
			for (int i = 0; i < 6; i++)
				shell.add(spawnShellBlock(player, i));
			playAccessibleSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, .8f, .75f);
		}, integer("titans-bulwark.presentation.impact-delay-ticks", 8));
		new BukkitRunnable() {
			int ticks;
			public void run() {
				if (!player.isOnline() || player.isDead() || ticks++ >= durationTicks) {
					displays.remove(shell);
					double stored = bulwarks.deactivate(player.getUniqueId());
					if (player.isOnline() && !player.isDead())
						bulwarkExpiry(player, stored);
					cancel();
					return;
				}
				for (int i = 0; i < shell.size(); i++)
					shell.get(i).teleport(shellLocation(player, i));
				if (ticks % 20 == 0)
					shell.forEach(AbilityService.this::updateShellVisibility);
				if (ticks % 5 == 0) {
					double ratio = bulwarks.charge(player.getUniqueId())
							/ Math.max(1, decimal("titans-bulwark.stored-damage-cap", 16));
					particleAccessible(player.getLocation().add(0, 1, 0),
							ratio > .65 ? Particle.ENCHANTED_HIT : Particle.CRIT,
							integer("titans-bulwark.presentation.active-particles", 4), .7, 1, .7, .01);
					if (ticks % 20 == 0)
						playAccessibleSound(player.getLocation(), Sound.BLOCK_STONE_HIT, .35f,
								(float) (.65 + ratio * .7));
				}
			}
		}.runTaskTimer(plugin, 0, 1);
	}
	private BlockDisplay spawnShellBlock(Player player, int index) {
		BlockDisplay shell = player.getWorld().spawn(shellLocation(player, index), BlockDisplay.class, display -> {
			display.setBlock((index % 2 == 0 ? Material.DEEPSLATE_TILES : Material.STONE_BRICKS).createBlockData());
			display.setPersistent(false);
			display.setViewRange(32);
			display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
					new Vector3f(.72f, .72f, .72f), new AxisAngle4f()));
		});
		return displays.track(shell);
	}
	private void updateShellVisibility(BlockDisplay shell) {
		displays.refreshVisibility(shell);
	}
	private Location shellLocation(Player player, int index) {
		double[][] offsets = {{.8, .7, 0}, {-.8, .7, 0}, {0, .7, .8}, {0, .7, -.8}, {0, 1.55, 0}, {0, -.05, 0}};
		double[] offset = offsets[index];
		return player.getLocation().add(offset[0] - .36, offset[1], offset[2] - .36);
	}
	private void bulwarkExpiry(Player player, double stored) {
		Location center = player.getLocation();
		double radius = decimal("titans-bulwark.expiry-radius", 4);
		double cap = decimal("titans-bulwark.stored-damage-cap", 16);
		double damage = AbilityCombatRules.scaledBulwarkValue(stored, cap,
				decimal("titans-bulwark.expiry-min-damage", 1), decimal("titans-bulwark.expiry-max-damage", 6));
		double knockback = AbilityCombatRules.scaledBulwarkValue(stored, cap,
				decimal("titans-bulwark.expiry-min-knockback", .8),
				decimal("titans-bulwark.expiry-max-knockback", 1.5));
		playAccessibleSound(center, Sound.ENTITY_IRON_GOLEM_DAMAGE, 1.25f,
				(float) (.65 + Math.min(1, stored / Math.max(1, cap)) * .35));
		particleAccessible(center.clone().add(0, 1, 0), Particle.GUST,
				integer("titans-bulwark.presentation.ending-particles", 48), radius * .4, .7, radius * .4, .08);
		for (LivingEntity target : nearbyLiving(center, radius, player).stream().filter(player::hasLineOfSight)
				.toList()) {
			Vector away = target.getLocation().toVector().subtract(center.toVector()).setY(0);
			if (away.lengthSquared() < .01)
				away = new Vector(1, 0, 0);
			target.setVelocity(away.normalize().multiply(knockback).setY(.3));
			suppressCombatInteractions = true;
			try {
				target.damage(damage, player);
			} finally {
				suppressCombatInteractions = false;
			}
		}
		applyUltimateDownside(player, "titans-bulwark");
	}

	private void startResonanceCooldown(Player caster, String abilityId, long readyAt) {
		DragonResonance resonance = Arrays.stream(DragonResonance.values())
				.filter(value -> value.id().equals(abilityId)).findFirst().orElse(null);
		if (resonance == null)
			return;
		for (Player participant : resonanceCoordinator.participants(caster, resonance)) {
			cooldowns.start(participant.getUniqueId(), abilityId, readyAt);
			cooldowns.startAndPersist(participant.getUniqueId(), AbilityCooldownTracker.RESONANCE_GROUP, readyAt);
		}
	}

	private long resonanceGroupCooldownSeconds(Player caster, String abilityId) {
		DragonResonance resonance = Arrays.stream(DragonResonance.values())
				.filter(value -> value.id().equals(abilityId)).findFirst().orElse(null);
		if (resonance == null)
			return rawCooldownSeconds(caster, AbilityCooldownTracker.RESONANCE_GROUP);
		List<Player> participants = resonanceCoordinator.participants(caster, resonance);
		if (participants.isEmpty())
			return rawCooldownSeconds(caster, AbilityCooldownTracker.RESONANCE_GROUP);
		return participants.stream()
				.mapToLong(player -> rawCooldownSeconds(player, AbilityCooldownTracker.RESONANCE_GROUP)).max()
				.orElse(0);
	}

	private void activateResonance(Player caster, DragonResonance resonance) {
		List<Player> team = resonanceCoordinator.participants(caster, resonance);
		if (team.size() != resonance.souls().size())
			return;
		Location center = teamCenter(team);
		for (Player member : team) {
			showTitle(member, resonanceTitle(resonance), "<gray>Dragon Souls resonate as one</gray>");
			plugin.messages().send(member, "resonance-cast", "ability", resonance.displayName(), "caster",
					caster.getName());
		}
		switch (resonance) {
			case THERMAL_CONVERGENCE -> thermalConvergence(caster, team, center);
			case VOLCANIC_AEGIS -> volcanicAegis(caster, team, center);
			case GLACIAL_BASTION -> glacialBastion(caster, team, center);
			case DRAGON_TRINITY -> dragonTrinity(caster, team, center);
		}
	}

	private void thermalConvergence(Player caster, List<Player> team, Location center) {
		playAccessibleSound(center, Sound.BLOCK_GLASS_HIT, 1f, .55f);
		List<AnchoredDisplay> armor = spawnTeamOrbit(team, new Material[]{Material.BLUE_ICE, Material.MAGMA_BLOCK},
				resonanceInteger("thermal-convergence.displays-per-player", 3),
				resonanceDecimal("thermal-convergence.display-scale", .34));
		int impactDelay = resonanceInteger("thermal-convergence.impact-delay-ticks", 20);
		int activeTicks = resonanceInteger("thermal-convergence.active-seconds", 6) * 20;
		int pulseInterval = Math.max(10, resonanceInteger("thermal-convergence.pulse-interval-ticks", 20));
		new BukkitRunnable() {
			int ticks;
			public void run() {
				if (!validTeam(team) || ticks > impactDelay + activeTicks) {
					removeAnchoredDisplays(armor);
					cancel();
					return;
				}
				Location midpoint = teamCenter(team);
				updateTeamOrbit(armor, ticks, resonanceDecimal("thermal-convergence.display-orbit-radius", .75), .13);
				if (ticks % 5 == 0) {
					particleLink(team.get(0).getLocation().add(0, 1, 0), team.get(1).getLocation().add(0, 1, 0),
							ticks % 10 == 0 ? Particle.SNOWFLAKE : Particle.FLAME,
							resonanceInteger("thermal-convergence.link-points", 14));
					particleAccessible(midpoint.clone().add(0, 1, 0),
							ticks % 10 == 0 ? Particle.SNOWFLAKE : Particle.FLAME,
							resonanceInteger("thermal-convergence.active-particles", 12), 1.4, .8, 1.4, .03);
				}
				if (ticks == impactDelay)
					thermalImpact(caster, team, midpoint);
				int active = ticks - impactDelay;
				if (active > 0 && active < activeTicks && active % pulseInterval == 0)
					thermalPulse(caster, team, midpoint, active / pulseInterval);
				if (active == activeTicks) {
					thermalFinisher(caster, team, midpoint);
					removeAnchoredDisplays(armor);
					cancel();
					return;
				}
				ticks++;
			}
		}.runTaskTimer(plugin, 0, 1);
	}

	private void volcanicAegis(Player caster, List<Player> team, Location center) {
		playAccessibleSound(center, Sound.BLOCK_ANVIL_PLACE, 1f, .65f);
		List<AnchoredDisplay> armor = spawnTeamOrbit(team,
				new Material[]{Material.MAGMA_BLOCK, Material.POLISHED_BLACKSTONE},
				resonanceInteger("volcanic-aegis.displays-per-player", 5),
				resonanceDecimal("volcanic-aegis.display-scale", .42));
		int impactDelay = resonanceInteger("volcanic-aegis.impact-delay-ticks", 18);
		int activeTicks = resonanceInteger("volcanic-aegis.active-seconds", 10) * 20;
		int pulseInterval = Math.max(20, resonanceInteger("volcanic-aegis.pulse-interval-ticks", 40));
		new BukkitRunnable() {
			int ticks;
			public void run() {
				if (!validTeam(team) || ticks > impactDelay + activeTicks) {
					clearVolcanicAegis(team);
					removeAnchoredDisplays(armor);
					cancel();
					return;
				}
				updateTeamOrbit(armor, ticks, resonanceDecimal("volcanic-aegis.display-orbit-radius", .72), .1);
				if (ticks % 5 == 0)
					for (Player member : team)
						particleAccessible(member.getLocation().add(0, 1, 0), Particle.FLAME,
								resonanceInteger("volcanic-aegis.active-particles-per-player", 8), .65, .9, .65, .03);
				if (ticks == impactDelay)
					volcanicImpact(caster, team);
				int active = ticks - impactDelay;
				if (active > 0 && active < activeTicks && active % pulseInterval == 0)
					volcanicPulse(caster, team);
				if (active == activeTicks) {
					volcanicFinisher(caster, team, teamCenter(team));
					clearVolcanicAegis(team);
					removeAnchoredDisplays(armor);
					cancel();
					return;
				}
				ticks++;
			}
		}.runTaskTimer(plugin, 0, 1);
	}

	private void glacialBastion(Player caster, List<Player> team, Location center) {
		playAccessibleSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, .5f);
		List<AnchoredDisplay> armor = spawnTeamOrbit(team, new Material[]{Material.BLUE_ICE, Material.AMETHYST_BLOCK},
				resonanceInteger("glacial-bastion.displays-per-player", 5),
				resonanceDecimal("glacial-bastion.display-scale", .4));
		int impactDelay = resonanceInteger("glacial-bastion.impact-delay-ticks", 18);
		int activeTicks = resonanceInteger("glacial-bastion.active-seconds", 10) * 20;
		int pulseInterval = Math.max(10, resonanceInteger("glacial-bastion.pulse-interval-ticks", 20));
		new BukkitRunnable() {
			int ticks;
			public void run() {
				if (!validTeam(team) || ticks > impactDelay + activeTicks) {
					clearGlacialWards(team);
					removeAnchoredDisplays(armor);
					cancel();
					return;
				}
				updateTeamOrbit(armor, ticks, resonanceDecimal("glacial-bastion.display-orbit-radius", .78), -.085);
				if (ticks % 5 == 0)
					for (Player member : team)
						particleAccessible(member.getLocation().add(0, 1, 0), Particle.SNOWFLAKE,
								resonanceInteger("glacial-bastion.active-particles-per-player", 8), .8, 1, .8, .025);
				if (ticks == impactDelay)
					glacialImpact(team);
				int active = ticks - impactDelay;
				if (active > 0 && active < activeTicks && active % pulseInterval == 0)
					glacialPulse(caster, team);
				if (active == activeTicks) {
					glacialFinisher(caster, team, teamCenter(team));
					clearGlacialWards(team);
					removeAnchoredDisplays(armor);
					cancel();
					return;
				}
				ticks++;
			}
		}.runTaskTimer(plugin, 0, 1);
	}

	private void dragonTrinity(Player caster, List<Player> team, Location center) {
		playAccessibleSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, .75f);
		List<BlockDisplay> sigil = spawnCenterOrbit(center,
				new Material[]{Material.BLUE_ICE, Material.MAGMA_BLOCK, Material.DEEPSLATE_TILES},
				resonanceInteger("dragon-trinity.sigil-displays", 9),
				resonanceDecimal("dragon-trinity.display-scale", .5));
		int impactDelay = resonanceInteger("dragon-trinity.impact-delay-ticks", 30);
		int activeTicks = resonanceInteger("dragon-trinity.active-seconds", 12) * 20;
		int pulseInterval = Math.max(20, resonanceInteger("dragon-trinity.pulse-interval-ticks", 60));
		new BukkitRunnable() {
			int ticks;
			public void run() {
				if (!validTeam(team) || ticks > impactDelay + activeTicks) {
					displays.remove(sigil);
					cancel();
					return;
				}
				Location midpoint = teamCenter(team);
				updateCenterOrbit(sigil, midpoint, ticks, resonanceDecimal("dragon-trinity.sigil-radius", 2.3), .075);
				if (ticks % 4 == 0) {
					for (Player member : team)
						particleLink(member.getLocation().add(0, 1.2, 0), midpoint.clone().add(0, 1.3, 0),
								Particle.DRAGON_BREATH, resonanceInteger("dragon-trinity.link-points", 16));
					particleAccessible(midpoint.clone().add(0, 1.2, 0), Particle.END_ROD,
							resonanceInteger("dragon-trinity.active-particles", 18), 2, 1.2, 2, .035);
				}
				if (ticks == impactDelay)
					trinityImpact(caster, team, midpoint);
				int active = ticks - impactDelay;
				if (active > 0 && active < activeTicks && active % pulseInterval == 0)
					trinityPulse(caster, team, midpoint, active / pulseInterval);
				if (active == activeTicks) {
					trinityFinisher(caster, team, midpoint);
					displays.remove(sigil);
					cancel();
					return;
				}
				ticks++;
			}
		}.runTaskTimer(plugin, 0, 1);
	}

	private void thermalImpact(Player caster, List<Player> team, Location center) {
		int duration = resonanceInteger("thermal-convergence.active-seconds", 6) * 20;
		for (Player member : team) {
			member.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration,
					resonanceInteger("thermal-convergence.speed-amplifier", 1)));
			member.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0));
		}
		double radius = resonanceDecimal("thermal-convergence.radius", 11);
		resonancePhase(team, "<gradient:aqua:red>Thermal collision</gradient>");
		playAccessibleSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.35f, 1.15f);
		particleAccessible(center.clone().add(0, 1, 0), Particle.FLAME,
				resonanceInteger("thermal-convergence.impact-particles", 88), radius * .45, 1.2, radius * .45, .09);
		particleAccessible(center.clone().add(0, 1, 0), Particle.SNOWFLAKE,
				resonanceInteger("thermal-convergence.impact-particles", 88) / 2, radius * .4, 1, radius * .4, .06);
		for (LivingEntity target : resonanceTargets(center, radius, team)) {
			dealAbilityDamage(target, resonanceDecimal("thermal-convergence.initial-damage", 12), caster);
			applyThermalDebuffs(target);
			pullToward(target, center, resonanceDecimal("thermal-convergence.pull-strength", .45));
		}
	}

	private void thermalPulse(Player caster, List<Player> team, Location center, int pulse) {
		double radius = resonanceDecimal("thermal-convergence.radius", 11);
		Particle particle = pulse % 2 == 0 ? Particle.SNOWFLAKE : Particle.FLAME;
		playAccessibleSound(center, pulse % 2 == 0 ? Sound.BLOCK_GLASS_HIT : Sound.ENTITY_BLAZE_SHOOT, .7f,
				pulse % 2 == 0 ? 1.45f : .95f);
		particleAccessible(center.clone().add(0, 1, 0), particle,
				resonanceInteger("thermal-convergence.pulse-particles", 36), radius * .35, .8, radius * .35, .05);
		for (LivingEntity target : resonanceTargets(center, radius, team)) {
			dealAbilityDamage(target, resonanceDecimal("thermal-convergence.pulse-damage", 2), caster);
			applyThermalDebuffs(target);
			pullToward(target, center, resonanceDecimal("thermal-convergence.pull-strength", .45));
		}
	}

	private void thermalFinisher(Player caster, List<Player> team, Location center) {
		double radius = resonanceDecimal("thermal-convergence.radius", 11);
		resonancePhase(team, "<red><bold>THERMAL RUPTURE</bold></red>");
		playAccessibleSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 1.35f);
		particleAccessible(center.clone().add(0, 1, 0), Particle.FLASH,
				resonanceInteger("thermal-convergence.flash-particles", 2), 0, 0, 0, 0);
		particleAccessible(center.clone().add(0, 1, 0), Particle.ITEM_SNOWBALL,
				resonanceInteger("thermal-convergence.finisher-particles", 64), radius * .45, 1.1, radius * .45, .15);
		for (LivingEntity target : resonanceTargets(center, radius, team)) {
			dealAbilityDamage(target, resonanceDecimal("thermal-convergence.finisher-damage", 6), caster);
			pushFrom(target, center, resonanceDecimal("thermal-convergence.finisher-knockback", 1.1), .45);
		}
	}

	private void applyThermalDebuffs(LivingEntity target) {
		int seconds = resonanceInteger("thermal-convergence.debuff-seconds", 5);
		target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, seconds * 20,
				resonanceInteger("thermal-convergence.slowness-amplifier", 2)));
		target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, seconds * 20,
				resonanceInteger("thermal-convergence.weakness-amplifier", 1)));
		target.setFireTicks(
				Math.max(target.getFireTicks(), resonanceInteger("thermal-convergence.fire-seconds", 3) * 20));
		if (!(target instanceof Player affected) || players.settings(affected.getUniqueId()).screenEffects())
			target.setFreezeTicks(
					Math.max(target.getFreezeTicks(), resonanceInteger("thermal-convergence.freeze-ticks", 50)));
	}

	private void volcanicImpact(Player caster, List<Player> team) {
		int duration = resonanceInteger("volcanic-aegis.active-seconds", 10) * 20;
		long expires = System.currentTimeMillis() + duration * 50L;
		for (Player member : team) {
			member.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration,
					resonanceInteger("volcanic-aegis.resistance-amplifier", 1)));
			member.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration,
					resonanceInteger("volcanic-aegis.absorption-amplifier", 2)));
			member.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0));
			resonances.grantAegis(member.getUniqueId(), expires);
		}
		resonancePhase(team, "<red><bold>MAGMA ARMOR FORGED</bold></red>");
		for (Player member : team) {
			playAccessibleSound(member.getLocation(), Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1f, .75f);
			particleAccessible(member.getLocation().add(0, 1, 0), Particle.LAVA,
					resonanceInteger("volcanic-aegis.impact-particles", 64), 1, .9, 1, .04);
		}
		for (LivingEntity target : resonanceTargetsAroundTeam(team,
				resonanceDecimal("volcanic-aegis.pulse-radius", 6))) {
			dealAbilityDamage(target, resonanceDecimal("volcanic-aegis.initial-damage", 10), caster);
			target.setFireTicks(
					Math.max(target.getFireTicks(), resonanceInteger("volcanic-aegis.fire-seconds", 6) * 20));
		}
	}

	private void volcanicPulse(Player caster, List<Player> team) {
		for (Player member : team) {
			playAccessibleSound(member.getLocation(), Sound.ENTITY_BLAZE_SHOOT, .55f, .7f);
			particleAccessible(member.getLocation().add(0, .8, 0), Particle.FLAME,
					resonanceInteger("volcanic-aegis.pulse-particles", 30),
					resonanceDecimal("volcanic-aegis.pulse-radius", 6) * .35, .6,
					resonanceDecimal("volcanic-aegis.pulse-radius", 6) * .35, .04);
		}
		for (LivingEntity target : resonanceTargetsAroundTeam(team,
				resonanceDecimal("volcanic-aegis.pulse-radius", 6))) {
			dealAbilityDamage(target, resonanceDecimal("volcanic-aegis.pulse-damage", 2), caster);
			target.setFireTicks(
					Math.max(target.getFireTicks(), resonanceInteger("volcanic-aegis.fire-seconds", 6) * 20));
		}
	}

	private void volcanicFinisher(Player caster, List<Player> team, Location center) {
		resonancePhase(team, "<gold><bold>AEGIS ERUPTION</bold></gold>");
		double radius = resonanceDecimal("volcanic-aegis.finisher-radius", 10);
		playAccessibleSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, .6f);
		particleAccessible(center.clone().add(0, 1, 0), Particle.LAVA,
				resonanceInteger("volcanic-aegis.finisher-particles", 72), radius * .45, 1, radius * .45, .1);
		for (LivingEntity target : resonanceTargets(center, radius, team)) {
			dealAbilityDamage(target, resonanceDecimal("volcanic-aegis.finisher-damage", 8), caster);
			pushFrom(target, center, resonanceDecimal("volcanic-aegis.finisher-knockback", 1.6), .65);
			target.setFireTicks(
					Math.max(target.getFireTicks(), resonanceInteger("volcanic-aegis.fire-seconds", 6) * 20));
		}
	}

	private void clearVolcanicAegis(List<Player> team) {
		for (Player member : team)
			resonances.clearAegis(member.getUniqueId());
	}

	private void glacialImpact(List<Player> team) {
		int duration = resonanceInteger("glacial-bastion.active-seconds", 10) * 20;
		int charges = resonanceInteger("glacial-bastion.ward-charges", 2);
		for (Player member : team) {
			member.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration,
					resonanceInteger("glacial-bastion.resistance-amplifier", 1)));
			member.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration,
					resonanceInteger("glacial-bastion.absorption-amplifier", 2)));
			resonances.grantWard(member.getUniqueId(), charges);
		}
		resonancePhase(team, "<aqua><bold>CRYSTAL WARDS: " + charges + "</bold></aqua>");
		for (Player member : team) {
			playAccessibleSound(member.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1f, .8f);
			particleAccessible(member.getLocation().add(0, 1, 0), Particle.SNOWFLAKE,
					resonanceInteger("glacial-bastion.impact-particles", 72), 1, .9, 1, .05);
		}
	}

	private void glacialPulse(Player caster, List<Player> team) {
		double radius = resonanceDecimal("glacial-bastion.domain-radius", 7);
		for (Player member : team)
			particleAccessible(member.getLocation().add(0, .5, 0), Particle.SNOWFLAKE,
					resonanceInteger("glacial-bastion.pulse-particles", 32), radius * .4, .45, radius * .4, .03);
		for (LivingEntity target : resonanceTargetsAroundTeam(team, radius)) {
			dealAbilityDamage(target, resonanceDecimal("glacial-bastion.pulse-damage", 1), caster);
			int seconds = resonanceInteger("glacial-bastion.debuff-seconds", 4);
			target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, seconds * 20,
					resonanceInteger("glacial-bastion.slowness-amplifier", 3)));
			target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, seconds * 20,
					resonanceInteger("glacial-bastion.mining-fatigue-amplifier", 2)));
			if (!(target instanceof Player affected) || players.settings(affected.getUniqueId()).screenEffects())
				target.setFreezeTicks(
						Math.max(target.getFreezeTicks(), resonanceInteger("glacial-bastion.freeze-ticks", 80)));
		}
	}

	private void glacialFinisher(Player caster, List<Player> team, Location center) {
		resonancePhase(team, "<aqua><bold>BASTION SHATTER</bold></aqua>");
		double radius = resonanceDecimal("glacial-bastion.finisher-radius", 11);
		playAccessibleSound(center, Sound.BLOCK_GLASS_BREAK, 1.4f, .65f);
		particleAccessible(center.clone().add(0, 1, 0), Particle.ITEM_SNOWBALL,
				resonanceInteger("glacial-bastion.finisher-particles", 80), radius * .45, 1, radius * .45, .14);
		for (LivingEntity target : resonanceTargets(center, radius, team)) {
			dealAbilityDamage(target, resonanceDecimal("glacial-bastion.finisher-damage", 8), caster);
			pushFrom(target, center, resonanceDecimal("glacial-bastion.finisher-knockback", .9), .45);
		}
	}

	private void clearGlacialWards(List<Player> team) {
		for (Player member : team)
			resonances.clearWard(member.getUniqueId());
	}

	private void trinityImpact(Player caster, List<Player> team, Location center) {
		int duration = resonanceInteger("dragon-trinity.active-seconds", 12) * 20;
		for (Player member : team) {
			member.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration,
					resonanceInteger("dragon-trinity.strength-amplifier", 1)));
			member.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration,
					resonanceInteger("dragon-trinity.speed-amplifier", 1)));
			member.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration,
					resonanceInteger("dragon-trinity.resistance-amplifier", 1)));
			member.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration,
					resonanceInteger("dragon-trinity.absorption-amplifier", 3)));
		}
		resonancePhase(team, "<light_purple><bold>PHASE I: SOUL ASCENSION</bold></light_purple>");
		double radius = resonanceDecimal("dragon-trinity.radius", 16);
		playAccessibleSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.6f, .55f);
		particleAccessible(center.clone().add(0, 1, 0), Particle.DRAGON_BREATH,
				resonanceInteger("dragon-trinity.impact-particles", 128), radius * .5, 1.5, radius * .5, .12);
		for (LivingEntity target : resonanceTargets(center, radius, team)) {
			dealAbilityDamage(target, resonanceDecimal("dragon-trinity.initial-damage", 18), caster);
			pullToward(target, center, resonanceDecimal("dragon-trinity.pull-strength", .65));
			target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,
					resonanceInteger("dragon-trinity.reveal-seconds", 12) * 20, 0));
		}
	}

	private void trinityPulse(Player caster, List<Player> team, Location center, int pulse) {
		resonancePhase(team, "<gold>PHASE II: TRINITY PULSE " + pulse + "</gold>");
		playAccessibleSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, (float) (.7 + pulse * .12));
		particleAccessible(center.clone().add(0, 1.2, 0), pulse % 2 == 0 ? Particle.END_ROD : Particle.DRAGON_BREATH,
				resonanceInteger("dragon-trinity.pulse-particles", 72), 3, 1.2, 3, .07);
		for (Player member : team) {
			var health = member.getAttribute(ServerAttributes.MAX_HEALTH);
			if (health != null)
				member.setHealth(Math.min(health.getValue(),
						member.getHealth() + resonanceDecimal("dragon-trinity.healing-per-pulse", 2)));
			setEnergy(member,
					Math.min(maxEnergy(), current(member) + resonanceInteger("dragon-trinity.energy-per-pulse", 5)));
		}
		for (LivingEntity target : resonanceTargets(center, resonanceDecimal("dragon-trinity.radius", 16), team)) {
			dealAbilityDamage(target, resonanceDecimal("dragon-trinity.pulse-damage", 4), caster);
			pullToward(target, center, resonanceDecimal("dragon-trinity.pull-strength", .65));
		}
	}

	private void trinityFinisher(Player caster, List<Player> team, Location center) {
		resonancePhase(team, "<gradient:aqua:red:gold><bold>PHASE III: DRAGONFALL</bold></gradient>");
		double radius = resonanceDecimal("dragon-trinity.finisher-radius", 18);
		playAccessibleSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.6f, 1.15f);
		particleAccessible(center.clone().add(0, 1.5, 0), Particle.FLASH,
				resonanceInteger("dragon-trinity.flash-particles", 3), 0, 0, 0, 0);
		particleAccessible(center.clone().add(0, 1, 0), Particle.END_ROD,
				resonanceInteger("dragon-trinity.finisher-particles", 128), radius * .5, 1.8, radius * .5, .16);
		for (LivingEntity target : resonanceTargets(center, radius, team)) {
			dealAbilityDamage(target, resonanceDecimal("dragon-trinity.finisher-damage", 14), caster);
			pushFrom(target, center, resonanceDecimal("dragon-trinity.finisher-knockback", 2), 1);
		}
	}

	private List<LivingEntity> resonanceTargetsAroundTeam(List<Player> team, double radius) {
		Set<UUID> friendly = team.stream().map(Player::getUniqueId).collect(java.util.stream.Collectors.toSet());
		Map<UUID, LivingEntity> targets = new LinkedHashMap<>();
		double boundedRadius = boundedTargetRadius(radius);
		for (Player member : team)
			if (member.isOnline())
				for (LivingEntity target : member.getWorld().getNearbyLivingEntities(member.getLocation(),
						boundedRadius, living -> validTarget(living, null) && !friendly.contains(living.getUniqueId())
								&& member.hasLineOfSight(living)))
					targets.put(target.getUniqueId(), target);
		return targets.values().stream().limit(MAX_TARGETS_PER_QUERY).toList();
	}

	private void pullToward(LivingEntity target, Location center, double strength) {
		Vector toward = center.toVector().subtract(target.getLocation().toVector());
		if (toward.lengthSquared() < .01)
			return;
		target.setVelocity(toward.normalize().multiply(strength).setY(.18));
	}

	private void resonancePhase(List<Player> team, String message) {
		Component component = mini.deserialize(message);
		for (Player member : team)
			if (member.isOnline() && players.settings(member.getUniqueId()).hud())
				member.sendActionBar(component);
	}

	private List<AnchoredDisplay> spawnTeamOrbit(List<Player> team, Material[] materials, int requestedPerPlayer,
			double scale) {
		int count = Math.max(2, Math.min(8, requestedPerPlayer));
		List<AnchoredDisplay> displays = new ArrayList<>();
		for (Player member : team)
			for (int index = 0; index < count; index++) {
				BlockDisplay display = spawnAbilityBlock(member.getLocation().add(0, 1, 0),
						materials[index % materials.length], scale);
				displays.add(new AnchoredDisplay(display, member, index, count));
			}
		return displays;
	}

	private void updateTeamOrbit(List<AnchoredDisplay> displays, int ticks, double radius, double speed) {
		for (AnchoredDisplay anchored : displays) {
			if (!anchored.anchor().isOnline() || !anchored.display().isValid())
				continue;
			double angle = Math.PI * 2 * anchored.index() / anchored.count() + ticks * speed;
			double y = .35 + (anchored.index() % 3) * .55 + Math.sin(angle * 2) * .1;
			anchored.display().teleport(anchored.anchor().getLocation().add(Math.cos(angle) * radius - .2, y,
					Math.sin(angle) * radius - .2));
			if (ticks % 10 == 0)
				this.displays.refreshVisibility(anchored.display());
		}
	}

	private List<BlockDisplay> spawnCenterOrbit(Location center, Material[] materials, int requested, double scale) {
		int count = Math.max(3, Math.min(12, requested));
		List<BlockDisplay> displays = new ArrayList<>();
		for (int index = 0; index < count; index++)
			displays.add(spawnAbilityBlock(center.clone().add(0, 1, 0), materials[index % materials.length], scale));
		return displays;
	}

	private void updateCenterOrbit(List<BlockDisplay> displays, Location center, int ticks, double radius,
			double speed) {
		for (int index = 0; index < displays.size(); index++) {
			BlockDisplay display = displays.get(index);
			if (!display.isValid())
				continue;
			double angle = Math.PI * 2 * index / displays.size() + ticks * speed;
			double y = .5 + (index % 3) * .75 + Math.sin(angle * 3) * .18;
			display.teleport(center.clone().add(Math.cos(angle) * radius - .25, y, Math.sin(angle) * radius - .25));
			if (ticks % 10 == 0)
				this.displays.refreshVisibility(display);
		}
	}

	private BlockDisplay spawnAbilityBlock(Location location, Material material, double scale) {
		float bounded = (float) Math.max(.1, Math.min(1.5, scale));
		BlockDisplay display = location.getWorld().spawn(location, BlockDisplay.class, entity -> {
			entity.setBlock(material.createBlockData());
			entity.setPersistent(false);
			entity.setViewRange(40);
			entity.setInterpolationDuration(2);
			entity.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
					new Vector3f(bounded, bounded, bounded), new AxisAngle4f()));
		});
		return displays.track(display);
	}

	private void particleLink(Location from, Location to, Particle particle, int requestedPoints) {
		if (from.getWorld() == null || !from.getWorld().equals(to.getWorld()))
			return;
		int points = Math.max(2, Math.min(integerRoot("presentation.maximum-ring-points", 48), requestedPoints));
		Vector path = to.toVector().subtract(from.toVector());
		for (int point = 0; point <= points; point++)
			particleAccessible(from.clone().add(path.clone().multiply(point / (double) points)), particle, 1, 0, 0, 0,
					.01);
	}

	private boolean validTeam(List<Player> team) {
		if (team.isEmpty())
			return false;
		World world = team.getFirst().getWorld();
		return team.stream()
				.allMatch(member -> member.isOnline() && !member.isDead() && member.getWorld().equals(world));
	}

	private void removeAnchoredDisplays(Collection<AnchoredDisplay> displays) {
		this.displays.remove(displays.stream().map(AnchoredDisplay::display).toList());
	}

	private record AnchoredDisplay(BlockDisplay display, Player anchor, int index, int count) {
	}

	private List<LivingEntity> resonanceTargets(Location center, double radius, List<Player> team) {
		Set<UUID> friendly = team.stream().map(Player::getUniqueId).collect(java.util.stream.Collectors.toSet());
		double boundedRadius = boundedTargetRadius(radius);
		return center.getWorld()
				.getNearbyLivingEntities(center, boundedRadius,
						target -> validTarget(target, null) && !friendly.contains(target.getUniqueId()) && team.stream()
								.anyMatch(member -> member.isOnline() && member.getWorld().equals(target.getWorld())
										&& member.hasLineOfSight(target)))
				.stream().limit(MAX_TARGETS_PER_QUERY).toList();
	}

	private void dealAbilityDamage(LivingEntity target, double damage, Player source) {
		suppressCombatInteractions = true;
		try {
			target.damage(Math.max(0, damage), source);
		} finally {
			suppressCombatInteractions = false;
		}
	}

	private void pushFrom(LivingEntity target, Location center, double strength, double y) {
		Vector away = target.getLocation().toVector().subtract(center.toVector()).setY(0);
		if (away.lengthSquared() < .01)
			away = new Vector(1, 0, 0);
		target.setVelocity(away.normalize().multiply(strength).setY(y));
	}

	private Location teamCenter(List<Player> team) {
		World world = team.getFirst().getWorld();
		double x = 0, y = 0, z = 0;
		for (Player member : team) {
			x += member.getX();
			y += member.getY();
			z += member.getZ();
		}
		return new Location(world, x / team.size(), y / team.size(), z / team.size());
	}

	private String resonanceTitle(DragonResonance resonance) {
		return switch (resonance) {
			case THERMAL_CONVERGENCE -> "<gradient:aqua:red><bold>THERMAL CONVERGENCE</bold></gradient>";
			case VOLCANIC_AEGIS -> "<gradient:red:gray><bold>VOLCANIC AEGIS</bold></gradient>";
			case GLACIAL_BASTION -> "<gradient:aqua:gray><bold>GLACIAL BASTION</bold></gradient>";
			case DRAGON_TRINITY -> "<gradient:aqua:red:gold><bold>DRAGON TRINITY</bold></gradient>";
		};
	}

	private String formatCooldown(long seconds) {
		if (seconds <= 0)
			return "Ready";
		if (seconds < 60)
			return seconds + "s";
		long minutes = seconds / 60, remaining = seconds % 60;
		return remaining == 0 ? minutes + "m" : minutes + "m " + remaining + "s";
	}

	private void applyUltimateDownside(Player player, String ability) {
		if (!player.isOnline() || player.isDead())
			return;
		int duration = integer(ability + ".downside.duration-seconds", 4);
		int first = integer(ability + ".downside.primary-amplifier", 0);
		int second = integer(ability + ".downside.secondary-amplifier", 0);
		PotionEffectType primary = ability.equals("titans-bulwark")
				? PotionEffectType.MINING_FATIGUE
				: PotionEffectType.SLOWNESS;
		PotionEffectType secondary = PotionEffectType.WEAKNESS;
		if (duration > 0) {
			if (first >= 0)
				player.addPotionEffect(new PotionEffect(primary, duration * 20, first, false, true, true));
			if (second >= 0)
				player.addPotionEffect(new PotionEffect(secondary, duration * 20, second, false, true, true));
		}
		int lock = integer(ability + ".downside.energy-regeneration-lock-seconds", 5);
		energy.blockRegeneration(player.getUniqueId(), System.currentTimeMillis() + lock * 1000L);
		if (players.settings(player.getUniqueId()).hud())
			player.sendActionBar(plugin.messages().component("ultimate-downside", "downside",
					config.file("abilities.yml").getString("abilities." + ability + ".downside.name", "Recovery"),
					"seconds", Integer.toString(Math.max(duration, lock))));
		playAccessibleSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, .65f, .7f);
	}
	public void handleResonanceDefense(EntityDamageEvent event) {
		if (suppressCombatInteractions || event.isCancelled() || event.getFinalDamage() <= 0
				|| !(event.getEntity() instanceof Player defender))
			return;
		int charges = resonances.wardCharges(defender.getUniqueId());
		if (charges <= 0)
			return;
		double reduction = Math.max(0,
				Math.min(.9, resonanceDecimal("glacial-bastion.ward-damage-reduction-fraction", .5)));
		event.setDamage(event.getDamage() * (1 - reduction));
		int remaining = resonances.consumeWard(defender.getUniqueId());
		playAccessibleSound(defender.getLocation(), Sound.BLOCK_GLASS_BREAK, .9f, 1.4f);
		particleAccessible(defender.getLocation().add(0, 1, 0), Particle.ITEM_SNOWBALL,
				resonanceInteger("glacial-bastion.ward-break-particles", 28), .55, .8, .55, .14);
		if (players.settings(defender.getUniqueId()).hud())
			defender.sendActionBar(mini.deserialize(
					"<aqua>Crystal Ward absorbed damage</aqua> <gray>| Charges: " + remaining + "</gray>"));
	}

	public void handleResonanceRetaliation(EntityDamageByEntityEvent event) {
		if (suppressCombatInteractions || event.isCancelled() || event.getFinalDamage() <= 0
				|| !(event.getEntity() instanceof Player defender)
				|| !resonances.hasAegis(defender.getUniqueId(), System.currentTimeMillis()))
			return;
		LivingEntity attacker = null;
		if (event.getDamager() instanceof LivingEntity living)
			attacker = living;
		else if (event.getDamager() instanceof Projectile projectile
				&& projectile.getShooter() instanceof LivingEntity living)
			attacker = living;
		if (attacker == null || attacker == defender)
			return;
		UUID key = new UUID(
				defender.getUniqueId().getMostSignificantBits() ^ attacker.getUniqueId().getMostSignificantBits(),
				defender.getUniqueId().getLeastSignificantBits() ^ attacker.getUniqueId().getLeastSignificantBits());
		long now = System.currentTimeMillis();
		if (!resonances.claimRetaliation(key, now,
				now + resonanceInteger("volcanic-aegis.retaliation-cooldown-ticks", 30) * 50L))
			return;
		dealAbilityDamage(attacker, resonanceDecimal("volcanic-aegis.retaliation-damage", 3), defender);
		attacker.setFireTicks(
				Math.max(attacker.getFireTicks(), resonanceInteger("volcanic-aegis.retaliation-fire-seconds", 3) * 20));
		playAccessibleSound(attacker.getLocation(), Sound.ITEM_FIRECHARGE_USE, .65f, 1.05f);
		particleAccessible(attacker.getLocation().add(0, 1, 0), Particle.FLAME,
				resonanceInteger("volcanic-aegis.retaliation-particles", 18), .35, .6, .35, .06);
	}
	public void handleCombatEffects(EntityDamageByEntityEvent event) {
		if (suppressCombatInteractions || event.isCancelled() || event.getFinalDamage() <= 0
				|| !(event.getEntity() instanceof LivingEntity target))
			return;
		Player attacker = attackingPlayer(event.getDamager());
		long now = System.currentTimeMillis();
		Optional<AbilityCombatRules.Brittle> shattered = brittle.consume(target.getUniqueId(), now,
				event.getFinalDamage());
		if (shattered.isPresent()) {
			double bonus = Math.max(0, decimal("absolute-zero.shatter-bonus-damage", 4));
			if (bonus > 0)
				event.setDamage(event.getDamage() + bonus);
			playAccessibleSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.1f, 1.35f);
			particleAccessible(target.getLocation().add(0, 1, 0), Particle.ITEM_SNOWBALL,
					integer("absolute-zero.shatter-particles", 20), .45, .65, .45, .16);
		}
		if (attacker != null && dragonborn.hasSoul(attacker, SoulIdentity.REV) && validRevPrey(attacker, target)
				&& revHunt.markedBy(target.getUniqueId(), attacker.getUniqueId(), now)) {
			AbilityCombatRules.CombatDamageSource source = event.getDamager() instanceof Player
					? AbilityCombatRules.CombatDamageSource.DIRECT_PLAYER
					: AbilityCombatRules.CombatDamageSource.PLAYER_PROJECTILE;
			if (AbilityCombatRules.RevHuntTracker.acceptsDamage(source)) {
				grantHeat(attacker, target, integerRoot("rev-hunt.heat.damage-marked-gain", 4), now);
				AbilityCombatRules.RampageGain rampage = revHunt.gainRampage(attacker.getUniqueId(),
						target.getUniqueId(), now, integer("infernos-wrath.rampage-progress-per-hit", 1),
						integer("infernos-wrath.maximum-rampage", 4),
						integer("infernos-wrath.rampage-grants-per-target", 1));
				if (rampage.granted()) {
					long mobilityExpiry = revHunt.extendMobility(attacker.getUniqueId(), now,
							integer("infernos-wrath.mobility-extension-ticks", 20) * 50L);
					applyPursuitEffects(attacker, (int) Math.max(1, Math.ceil((mobilityExpiry - now) / 50d)));
					playConfiguredSound(attacker.getLocation(), "infernos-wrath.sounds.rampage-gain",
							Sound.ENTITY_BLAZE_SHOOT, .65f, 1.35f);
					particleFlameArc(target.getLocation().add(0, 1, 0), attacker.getLocation().add(0, 1, 0),
							integer("infernos-wrath.presentation.rampage-arc-particles", 10));
					if (rampage.maximumReached()) {
						revHunt.armFinisher(attacker.getUniqueId(), Math.min(revHunt.huntEndsAt(attacker.getUniqueId()),
								now + integerRoot("rev-hunt.finisher.armed-duration-ticks", 120) * 50L));
						showTitle(attacker, "<red><bold>PREDATOR'S CLAIM READY</bold>",
								"<gold>Consume it with Rend's marked recast</gold>");
						playConfiguredSound(attacker.getLocation(), "infernos-wrath.sounds.rampage-maximum",
								Sound.ENTITY_WITHER_SPAWN, .7f, 1.35f);
					}
				}
			}
		}
	}

	public void handleBulwarkDamage(EntityDamageEvent event) {
		if (event.isCancelled() || !(event.getEntity() instanceof Player defender)
				|| !bulwarks.active(defender.getUniqueId()))
			return;
		double prevented = Math.max(0, event.getDamage() - event.getFinalDamage());
		double stored = AbilityCombatRules.storedBulwarkDamage(bulwarks.charge(defender.getUniqueId()), prevented,
				decimal("titans-bulwark.stored-damage-fraction", .35), decimal("titans-bulwark.stored-damage-cap", 16));
		bulwarks.charge(defender.getUniqueId(), stored);
		if (players.settings(defender.getUniqueId()).hud())
			defender.sendActionBar(mini.deserialize(
					"<gold>Bulwark Charge:</gold> <white>" + String.format(Locale.ROOT, "%.1f", stored) + "</white>"));
	}

	public void handleBulwarkMelee(EntityDamageByEntityEvent event) {
		if (suppressCombatInteractions || reflectionGuard.active() || !(event.getEntity() instanceof Player defender)
				|| !bulwarks.active(defender.getUniqueId()))
			return;
		if (!(event.getDamager() instanceof LivingEntity attacker) || attacker == defender)
			return;
		double reflected = Math.max(0,
				event.getFinalDamage() * decimal("titans-bulwark.reflected-damage-fraction", .4));
		if (reflected > 0 && reflectionGuard.enter()) {
			suppressCombatInteractions = true;
			try {
				attacker.damage(reflected, defender);
			} finally {
				suppressCombatInteractions = false;
				reflectionGuard.exit();
			}
		}
		attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
				integer("titans-bulwark.reflected-slowness-seconds", 2) * 20, 1));
	}

	private void applyPursuitEffects(Player player, int ticks) {
		player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ticks,
				integer("infernos-wrath.mobility-speed-amplifier", 1), false, true, true));
		player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, ticks, 0, false, true, true));
	}

	private Player attackingPlayer(Entity damager) {
		if (damager instanceof Player player)
			return player;
		if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player)
			return player;
		return null;
	}

	private void applyInfernoMark(Player rev, LivingEntity target, long now) {
		revHunt.mark(target.getUniqueId(), rev.getUniqueId(), now,
				integerRoot("rev-hunt.mark.duration-ticks", 120) * 50L,
				integerRoot("rev-hunt.mark.maximum-remaining-ticks", 180) * 50L,
				Math.min(16, integerRoot("rev-hunt.mark.maximum-targets", 8)));
	}

	private void grantHeat(Player rev, LivingEntity target, int amount, long now) {
		int previous = revHunt.heat(rev.getUniqueId());
		AbilityCombatRules.HeatGain gain = revHunt.gainHeat(rev.getUniqueId(), target.getUniqueId(), now, amount,
				integerRoot("rev-hunt.heat.maximum", 100),
				integerRoot("rev-hunt.heat.per-target-gain-cooldown-ticks", 20) * 50L,
				target instanceof Player
						? integerRoot("rev-hunt.heat.maximum-gains-per-player", 20)
						: integerRoot("rev-hunt.heat.maximum-gains-per-mob", 3));
		if (!gain.granted())
			return;
		updateRevHeatBar(rev);
		int mobilityThreshold = integerRoot("rev-hunt.heat.mobility-threshold", 35);
		int trackingThreshold = integerRoot("rev-hunt.heat.tracking-threshold", 65);
		if (previous < mobilityThreshold && gain.heat() >= mobilityThreshold) {
			rev.addPotionEffect(
					new PotionEffect(PotionEffectType.SPEED, integerRoot("rev-hunt.heat.threshold-speed-ticks", 30),
							integerRoot("rev-hunt.heat.threshold-speed-amplifier", 0), false, true, true));
			playConfiguredSound(rev.getLocation(), "rev-hunt.sounds.tier-gain", Sound.ITEM_FIRECHARGE_USE, .7f, 1.15f);
		} else if (previous < trackingThreshold && gain.heat() >= trackingThreshold)
			playConfiguredSound(rev.getLocation(), "rev-hunt.sounds.tier-gain", Sound.ITEM_FIRECHARGE_USE, .75f, 1.35f);
		if (gain.finisherArmed() && previous < integerRoot("rev-hunt.heat.maximum", 100)) {
			revHunt.armFinisher(rev.getUniqueId(),
					now + integerRoot("rev-hunt.finisher.armed-duration-ticks", 120) * 50L);
			showTitle(rev, "<red><bold>PREDATOR'S CLAIM READY</bold>",
					"<gold>Cross prey with Rend, then recast</gold>");
			playConfiguredSound(rev.getLocation(), "rev-hunt.sounds.finisher-ready", Sound.ENTITY_WITHER_SPAWN, .65f,
					1.45f);
		}
	}

	private boolean validRevPrey(Player rev, LivingEntity target) {
		if (target == rev || !target.isValid() || target.isDead() || target instanceof ArmorStand
				|| target instanceof Player player && player.getGameMode() == GameMode.SPECTATOR)
			return false;
		if (dragonborn.isDragonborn(target.getUniqueId()))
			return false;
		if (target instanceof Player)
			return true;
		double maximumHealth = Optional.ofNullable(target.getAttribute(ServerAttributes.MAX_HEALTH))
				.map(attribute -> attribute.getValue()).orElse(0d);
		return target instanceof Monster && maximumHealth >= decimalRoot("rev-hunt.heat.minimum-mob-max-health", 12);
	}

	private LivingEntity aimedMarkedTarget(Player player, double range, double coneDegrees, long now) {
		Vector direction = player.getEyeLocation().getDirection().normalize();
		double minimumDot = Math.cos(Math.toRadians(Math.max(0, Math.min(89, coneDegrees))));
		return nearbyLiving(player.getLocation(), range, player).stream()
				.filter(target -> validRevPrey(player, target)
						&& revHunt.markedBy(target.getUniqueId(), player.getUniqueId(), now)
						&& player.hasLineOfSight(target) && clearPath(player, target))
				.filter(target -> {
					Vector toward = target.getEyeLocation().toVector().subtract(player.getEyeLocation().toVector());
					return toward.lengthSquared() > .01 && toward.normalize().dot(direction) >= minimumDot;
				}).max(Comparator.comparingDouble(target -> {
					Vector toward = target.getEyeLocation().toVector().subtract(player.getEyeLocation().toVector())
							.normalize();
					return toward.dot(direction);
				})).orElse(null);
	}

	private boolean clearPath(Player player, LivingEntity target) {
		Vector path = target.getEyeLocation().toVector().subtract(player.getEyeLocation().toVector());
		double distance = path.length();
		return distance > .01 && player.getWorld().rayTraceBlocks(player.getEyeLocation(), path.normalize(), distance,
				FluidCollisionMode.NEVER, true) == null;
	}

	private void showRevTracking(Player rev, long now) {
		if (revHunt.heat(rev.getUniqueId()) < integerRoot("rev-hunt.heat.tracking-threshold", 65))
			return;
		var settings = players.settings(rev.getUniqueId());
		if (!settings.animationParticles() || settings.effects() == com.dragonaltar.player.EffectMode.MINIMAL)
			return;
		int maximum = Math.max(1, Math.min(8, integerRoot("rev-hunt.tracking.maximum-target-cues", 4))), shown = 0;
		double range = Math.max(1, Math.min(48, decimalRoot("rev-hunt.tracking.range", 24)));
		for (LivingEntity target : nearbyLiving(rev.getLocation(), range, rev)) {
			if (shown >= maximum)
				break;
			if (validRevPrey(rev, target) && revHunt.markedBy(target.getUniqueId(), rev.getUniqueId(), now)
					&& rev.hasLineOfSight(target)) {
				showMarkedTrailToRev(rev, target);
				shown++;
			}
		}
	}

	private void showMarkedTrailToRev(Player rev, LivingEntity target) {
		var settings = players.settings(rev.getUniqueId());
		if (!settings.animationParticles() || settings.effects() == com.dragonaltar.player.EffectMode.MINIMAL)
			return;
		int requested = integerRoot("rev-hunt.tracking.particles-per-cue", 4);
		int count = settings.effects() == com.dragonaltar.player.EffectMode.REDUCED
				? Math.max(1, requested / 2)
				: requested;
		Vector direction = target.getLocation().toVector().subtract(rev.getLocation().toVector());
		if (direction.lengthSquared() < .01)
			return;
		Location cue = rev.getEyeLocation()
				.add(direction.normalize().multiply(decimalRoot("rev-hunt.tracking.directional-cue-distance", 1.8)));
		rev.spawnParticle(Particle.SMALL_FLAME, cue, count, .08, .08, .08, .005);
	}

	private void particleFlameArc(Location start, Location end, int requested) {
		int points = Math.max(1, Math.min(integerRoot("presentation.maximum-flame-arc-particles", 16), requested));
		Vector path = end.toVector().subtract(start.toVector());
		for (int i = 0; i < points; i++) {
			double progress = (i + 1d) / points;
			Location point = start.clone().add(path.clone().multiply(progress)).add(0,
					Math.sin(Math.PI * progress) * decimalRoot("rev-hunt.tracking.flame-arc-height", .7), 0);
			particleAccessible(point, Particle.FLAME, 1, 0, 0, 0, .01);
		}
	}

	private void playConfiguredSound(Location location, String path, Sound fallback, float fallbackVolume,
			float fallbackPitch) {
		presentation.playConfiguredSound(location, path, fallback, fallbackVolume, fallbackPitch);
	}

	private String combatStatus(Player player) {
		long now = System.currentTimeMillis();
		List<String> parts = new ArrayList<>();
		if (brittle.active(player.getUniqueId(), now))
			parts.add("Brittle");
		if (revHunt.marked(player.getUniqueId(), now))
			parts.add("Inferno Marked");
		int brittleTargets = brittle.countFor(player.getUniqueId(), now);
		if (brittleTargets > 0)
			parts.add("Brittle targets: " + brittleTargets);
		if (dragonborn.hasSoul(player, SoulIdentity.REV)) {
			int heat = revHunt.heat(player.getUniqueId()), maximum = integerRoot("rev-hunt.heat.maximum", 100);
			parts.add("Heat: " + heat + "/" + maximum + " " + heatTier(heat));
			int marks = revHunt.activeMarks(player.getUniqueId(), now);
			if (marks > 0)
				parts.add("Marked: " + marks);
			if (revHunt.recastAvailable(player.getUniqueId(), now))
				parts.add("Rend Recast: "
						+ formatCooldown((long) Math.ceil((revHunt.recastEndsAt(player.getUniqueId()) - now) / 1000d)));
			if (revHunt.huntActive(player.getUniqueId(), now))
				parts.add("Hunt: "
						+ formatCooldown((long) Math.ceil((revHunt.huntEndsAt(player.getUniqueId()) - now) / 1000d))
						+ " Rampage: " + revHunt.rampage(player.getUniqueId()) + "/"
						+ integer("infernos-wrath.maximum-rampage", 4));
			if (revHunt.finisherArmed(player.getUniqueId(), now))
				parts.add("Predator's Claim Ready");
		}
		if (bulwarks.active(player.getUniqueId()))
			parts.add("Bulwark: " + String.format(Locale.ROOT, "%.1f", bulwarks.charge(player.getUniqueId())));
		return parts.isEmpty() ? "" : " | " + String.join(" | ", parts);
	}

	private String heatTier(int heat) {
		if (heat >= integerRoot("rev-hunt.heat.tracking-threshold", 65))
			return "Predator";
		if (heat >= integerRoot("rev-hunt.heat.mobility-threshold", 35))
			return "Pursuing";
		return "Stalking";
	}

	private void updateRevHeatBar(Player player) {
		boolean visible = player.isOnline() && dragonborn.hasSoul(player, SoulIdentity.REV)
				&& config.file("abilities.yml").getBoolean("rev-hunt.heat-bar.enabled", true)
				&& players.settings(player.getUniqueId()).hud();
		revHeatBars.update(player, revHunt.heat(player.getUniqueId()), integerRoot("rev-hunt.heat.maximum", 100),
				integerRoot("rev-hunt.heat.mobility-threshold", 35),
				integerRoot("rev-hunt.heat.tracking-threshold", 65), visible);
	}

	private void showTitle(Player player, String title, String subtitle) {
		presentation.showTitle(player, title, subtitle);
	}

	private void playAccessibleSound(Location location, Sound sound, float volume, float pitch) {
		presentation.playSound(location, sound, volume, pitch);
	}

	private void particleAccessible(Location location, Particle particle, int requested, double offsetX, double offsetY,
			double offsetZ, double extra) {
		particleAccessible(location, particle, requested, offsetX, offsetY, offsetZ, extra, null);
	}

	private void particleAccessible(Location location, Particle particle, int requested, double offsetX, double offsetY,
			double offsetZ, double extra, Object data) {
		presentation.particle(location, particle, requested, offsetX, offsetY, offsetZ, extra, data);
	}

	private void passiveParticleAccessible(Location location, Particle particle, int requested, double offsetX,
			double offsetY, double offsetZ, double extra) {
		presentation.passiveParticle(location, particle, requested, offsetX, offsetY, offsetZ, extra);
	}

	private static List<LivingEntity> nearbyLiving(Location center, double radius, Player owner) {
		if (center.getWorld() == null)
			return List.of();
		double boundedRadius = boundedTargetRadius(radius);
		return center.getWorld().getNearbyLivingEntities(center, boundedRadius, living -> validTarget(living, owner))
				.stream().limit(MAX_TARGETS_PER_QUERY).toList();
	}
	private static boolean validTarget(LivingEntity living, Player owner) {
		return living != owner && living.isValid() && !living.isDead() && !(living instanceof ArmorStand)
				&& (!(living instanceof Player player) || player.getGameMode() != GameMode.SPECTATOR);
	}
	static double boundedTargetRadius(double requested) {
		if (!Double.isFinite(requested))
			return 0;
		return Math.max(0, Math.min(MAX_TARGET_RADIUS, requested));
	}
	static int maximumTargetsPerQuery() {
		return MAX_TARGETS_PER_QUERY;
	}
	private static boolean isGrounded(Player player) {
		return player.getWorld().rayTraceBlocks(player.getLocation(), new Vector(0, -1, 0), .2,
				FluidCollisionMode.NEVER, true) != null;
	}
	private static double distanceSquaredToSegment(Vector point, Vector start, Vector end) {
		Vector segment = end.clone().subtract(start);
		double lengthSquared = segment.lengthSquared();
		if (lengthSquared <= .000001)
			return point.distanceSquared(start);
		double progress = Math.max(0, Math.min(1, point.clone().subtract(start).dot(segment) / lengthSquared));
		return point.distanceSquared(start.clone().add(segment.multiply(progress)));
	}
	private int integer(String path, int fallback) {
		return config.file("abilities.yml").getInt("abilities." + path, fallback);
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
	private String label(String ability, String fallback) {
		return config.file("abilities.yml").getString("abilities." + ability + ".name", fallback);
	}
	private int resonanceInteger(String path, int fallback) {
		return config.file("abilities.yml").getInt("resonances." + path, fallback);
	}
	private double resonanceDecimal(String path, double fallback) {
		return config.file("abilities.yml").getDouble("resonances." + path, fallback);
	}
	private String resonanceLabel(String ability, String fallback) {
		return config.file("abilities.yml").getString("resonances." + ability + ".name", fallback);
	}
}
