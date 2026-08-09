package com.dragonaltar.dragonevent;

import com.dragonaltar.altar.AltarState;
import com.dragonaltar.DragonAltarPlugin;
import com.dragonaltar.audit.AuditService;
import com.dragonaltar.api.event.*;
import com.dragonaltar.persistence.YamlDataStore;
import com.dragonaltar.soul.DragonSoulService;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

import java.time.Instant;
import java.util.*;

public final class DragonEventManager {
	private final DragonAltarPlugin plugin;
	private final YamlDataStore store;
	private final DragonSoulService souls;
	private final AuditService audit;
	private final NamespacedKey canonicalKey, sessionKey, crystalKey, testKey, testCrystalKey;
	private DragonEventState state = DragonEventState.NOT_STARTED;
	private AltarState altarState = AltarState.UNCONFIGURED;
	private UUID sessionId, dragonId;
	private UUID startedBy;
	private Instant startedAt, completedAt;
	private String completionMethod;
	private boolean forceKilled;
	private long testVanillaUntil;

	public DragonEventManager(DragonAltarPlugin plugin, YamlDataStore store, DragonSoulService souls,
			AuditService audit) {
		this.plugin = plugin;
		this.store = store;
		this.souls = souls;
		this.audit = audit;
		canonicalKey = new NamespacedKey(plugin, "canonical_dragon");
		sessionKey = new NamespacedKey(plugin, "event_session");
		crystalKey = new NamespacedKey(plugin, "summoning_crystal");
		testKey = new NamespacedKey(plugin, "test_dragon");
		testCrystalKey = new NamespacedKey(plugin, "test_crystal");
	}
	public void load() {
		YamlConfiguration y = store.load("event.yml");
		try {
			altarState = AltarState.valueOf(store.load("altar-state.yml").getString("altar.state", "UNCONFIGURED"));
		} catch (IllegalArgumentException ignored) {
			altarState = AltarState.UNCONFIGURED;
		}
		try {
			state = DragonEventState.valueOf(y.getString("dragon-event.state", "NOT_STARTED"));
		} catch (IllegalArgumentException e) {
			state = DragonEventState.RECOVERY_REQUIRED;
		}
		sessionId = uuid(y.getString("dragon-event.session-id"));
		dragonId = uuid(y.getString("dragon-event.canonical-dragon-uuid"));
		startedBy = uuid(y.getString("dragon-event.started-by"));
		startedAt = instant(y.getString("dragon-event.started-at"));
		completedAt = instant(y.getString("dragon-event.completed-at"));
		completionMethod = y.getString("dragon-event.completion-method");
		forceKilled = y.getBoolean("dragon-event.force-killed", false);
		boolean recoveryChanged = false;
		if (EnumSet.of(DragonEventState.PREPARING, DragonEventState.SUMMONING, DragonEventState.DEATH_SEQUENCE)
				.contains(state)) {
			state = DragonEventState.RECOVERY_REQUIRED;
			recoveryChanged = true;
		}
		if (state == DragonEventState.ACTIVE && canonicalDragon().isEmpty()) {
			state = DragonEventState.RECOVERY_REQUIRED;
			recoveryChanged = true;
		}
		if (state == DragonEventState.NOT_STARTED && store.load("altar-state.yml").getString("altar.state") == null)
			setAltarState(AltarState.UNCONFIGURED);
		if (!y.contains("dragon-event.state") || recoveryChanged)
			save(null, null);
		if (state == DragonEventState.RECOVERY_REQUIRED)
			notifyRecoveryRequired();
		if (state == DragonEventState.DEFEATED || state == DragonEventState.ALTAR_AWAKENING)
			Bukkit.getScheduler().runTask(plugin, this::finishAltarAwakening);
	}
	public DragonEventState state() {
		return state;
	}
	public AltarState altarState() {
		return altarState;
	}
	public UUID dragonId() {
		return dragonId;
	}
	public UUID sessionId() {
		return sessionId;
	}
	public boolean isCanonical(EnderDragon dragon) {
		String session = dragon.getPersistentDataContainer().get(sessionKey, PersistentDataType.STRING);
		return dragonId != null && dragon.getUniqueId().equals(dragonId) && sessionId != null
				&& sessionId.toString().equals(session);
	}
	public boolean isEventCrystal(EnderCrystal crystal) {
		String session = crystal.getPersistentDataContainer().get(crystalKey, PersistentDataType.STRING);
		return sessionId != null && sessionId.toString().equals(session);
	}
	public List<String> validateStart(Location fountain, Map<String, Location> configuredCrystals) {
		List<String> errors = new ArrayList<>();
		if (state != DragonEventState.NOT_STARTED)
			errors.add("Event state is " + state);
		if (!souls.all().isEmpty())
			errors.add("Dragon Soul records already exist");
		String setup = plugin.validateSetup();
		if (!setup.equals("Valid"))
			errors.add(setup);
		if (fountain == null || fountain.getWorld() == null
				|| fountain.getWorld().getEnvironment() != World.Environment.THE_END)
			errors.add("A valid End fountain is required");
		if (configuredCrystals.size() != 4)
			errors.add("Four crystal locations are required");
		if (fountain != null && fountain.getWorld() != null) {
			DragonBattle battle = fountain.getWorld().getEnderDragonBattle();
			if (battle == null)
				errors.add("The configured world has no End dragon battle");
			else if (battle.getRespawnPhase() != DragonBattle.RespawnPhase.NONE)
				errors.add("A vanilla dragon respawn sequence is already active");
			if (!fountain.getWorld().getEntitiesByClass(EnderDragon.class).isEmpty())
				errors.add("An Ender Dragon is already active in the configured world");
			for (Location location : configuredCrystals.values())
				if (location.getWorld() == null || !location.getWorld().equals(fountain.getWorld()))
					errors.add("A crystal is outside the configured End world");
				else if (location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)
						&& !location.getNearbyEntitiesByType(EnderCrystal.class, 1.25).isEmpty())
					errors.add("A crystal location is occupied");
		}
		return List.copyOf(errors);
	}
	public void start(Player actor, Location fountain, Map<String, Location> configuredCrystals) {
		List<String> preflight = validateStart(fountain, configuredCrystals);
		if (!preflight.isEmpty())
			throw new IllegalStateException(String.join("; ", preflight));
		if (fountain == null || fountain.getWorld() == null
				|| fountain.getWorld().getEnvironment() != World.Environment.THE_END)
			throw new IllegalStateException("A valid End fountain is required");
		DragonBattle battle = Objects.requireNonNull(fountain.getWorld().getEnderDragonBattle(),
				"The configured world has no End dragon battle");
		if (battle.getRespawnPhase() != DragonBattle.RespawnPhase.NONE)
			throw new IllegalStateException("A vanilla dragon respawn sequence is already active");
		if (!fountain.getWorld().getEntitiesByClass(EnderDragon.class).isEmpty())
			throw new IllegalStateException("An Ender Dragon is already active in the configured world");
		transition(DragonEventState.PREPARING);
		sessionId = UUID.randomUUID();
		startedBy = actor.getUniqueId();
		startedAt = Instant.now();
		save(actor.getUniqueId(), null);
		AncientDragonEventPrepareEvent prepare = new AncientDragonEventPrepareEvent(actor, sessionId);
		Bukkit.getPluginManager().callEvent(prepare);
		if (prepare.isCancelled()) {
			transition(DragonEventState.NOT_STARTED);
			sessionId = null;
			save(actor.getUniqueId(), "CANCELLED");
			throw new IllegalStateException("Event start was cancelled by another plugin");
		}
		List<Location> locations = new ArrayList<>(configuredCrystals.values());
		if (locations.size() != 4) {
			transition(DragonEventState.NOT_STARTED);
			sessionId = null;
			save(actor.getUniqueId(), null);
			throw new IllegalStateException("Four crystal locations are required");
		}
		for (Location location : locations) {
			location.getChunk().load();
			if (!location.getNearbyEntitiesByType(EnderCrystal.class, 1.25).isEmpty()) {
				transition(DragonEventState.NOT_STARTED);
				sessionId = null;
				save(actor.getUniqueId(), null);
				throw new IllegalStateException("A crystal already occupies " + location.getBlockX() + ","
						+ location.getBlockY() + "," + location.getBlockZ());
			}
		}
		transition(DragonEventState.SUMMONING);
		save(actor.getUniqueId(), null);
		Bukkit.getPluginManager().callEvent(new AncientDragonEventStartEvent(sessionId));
		List<EnderCrystal> spawned = new ArrayList<>();
		try {
			for (Location location : locations)
				spawned.add(location.getWorld().spawn(location, EnderCrystal.class, c -> {
					c.setShowingBottom(true);
					c.getPersistentDataContainer().set(crystalKey, PersistentDataType.STRING, sessionId.toString());
				}));
			if (!battle.initiateRespawn(spawned))
				throw new IllegalStateException(
						"Paper rejected the configured crystals for the vanilla respawn ritual");
		} catch (RuntimeException ex) {
			transition(DragonEventState.RECOVERY_REQUIRED);
			save(actor.getUniqueId(), "CRYSTAL_SPAWN_FAILURE");
			notifyRecoveryRequired();
			throw new IllegalStateException("Crystal spawning failed; event requires recovery", ex);
		}
		save(actor.getUniqueId(), null);
		audit.record("EVENT_START", actor.getUniqueId().toString(), sessionId.toString());
		Bukkit.broadcast(plugin.messages().component("event-started"));
	}
	public void trackVanillaSpawn(EnderDragon dragon, CreatureSpawnEvent.SpawnReason reason) {
		if (reason != CreatureSpawnEvent.SpawnReason.DEFAULT)
			return;
		if (state != DragonEventState.SUMMONING && System.currentTimeMillis() <= testVanillaUntil) {
			dragon.getPersistentDataContainer().set(testKey, PersistentDataType.BYTE, (byte) 1);
			dragon.customName(net.kyori.adventure.text.Component.text("Test Vanilla Dragon"));
			audit.record("TEST_VANILLA_DRAGON", "DEVELOPER", dragon.getUniqueId().toString());
			return;
		}
		Location fountain = plugin.configuredLocation("altar.yml", "fountain");
		if (fountain == null || !fountain.getWorld().equals(dragon.getWorld())
				|| Math.abs(dragon.getLocation().getX() - fountain.getX()) > 128
				|| Math.abs(dragon.getLocation().getZ() - fountain.getZ()) > 128)
			return;
		trackSpawn(dragon);
	}
	private void trackSpawn(EnderDragon dragon) {
		if (state != DragonEventState.SUMMONING || dragonId != null)
			return;
		dragonId = dragon.getUniqueId();
		transition(DragonEventState.ACTIVE);
		dragon.getPersistentDataContainer().set(canonicalKey, PersistentDataType.BYTE, (byte) 1);
		dragon.getPersistentDataContainer().set(sessionKey, PersistentDataType.STRING, sessionId.toString());
		save(null, null);
		audit.record("DRAGON_SPAWN", "SYSTEM", dragonId.toString());
		Bukkit.getPluginManager().callEvent(new AncientDragonSpawnEvent(dragon));
	}
	public void defeated(EnderDragon dragon, String method) {
		if (!isCanonical(dragon) || state != DragonEventState.ACTIVE)
			return;
		transition(DragonEventState.DEATH_SEQUENCE);
		save(null, method);
		Bukkit.getPluginManager().callEvent(new AncientDragonDeathEvent(dragon, method));
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			transition(DragonEventState.DEFEATED);
			save(null, method);
			transition(DragonEventState.ALTAR_AWAKENING);
			save(null, method);
			setAltarState(AltarState.AWAKENING);
			Location altar = plugin.configuredLocation("altar.yml", "altar-center");
			if (altar != null)
				plugin.animations().play("altar-awaken", altar, null);
			Bukkit.getScheduler().runTaskLater(plugin, this::finishAltarAwakening,
					plugin.getConfig().getLong("event.altar-awakening-delay-ticks", 80));
		}, plugin.getConfig().getLong("event.scaled-dragon-reward-delay-ticks", 40));
	}
	private void finishAltarAwakening() {
		if (state != DragonEventState.DEFEATED && state != DragonEventState.ALTAR_AWAKENING)
			return;
		if (state == DragonEventState.DEFEATED) {
			transition(DragonEventState.ALTAR_AWAKENING);
			save(null, null);
			setAltarState(AltarState.AWAKENING);
		}
		if (souls.all().isEmpty())
			souls.createInitialSouls();
		transition(DragonEventState.ALTAR_ACTIVE);
		save(null, null);
		setAltarState(AltarState.ACTIVE);
		audit.record("DRAGON_DEATH", "SYSTEM", completionMethod == null ? "UNKNOWN" : completionMethod);
		audit.record("ALTAR_AWAKEN", "SYSTEM", String.valueOf(sessionId));
		Bukkit.broadcast(plugin.messages().component("event-defeated"));
	}
	public void abort(UUID actor) {
		if (!EnumSet.of(DragonEventState.PREPARING, DragonEventState.SUMMONING, DragonEventState.ACTIVE)
				.contains(state))
			throw new IllegalStateException("Nothing to abort");
		transition(DragonEventState.ABORTED);
		cleanupCrystals();
		save(actor, "ABORTED");
		audit.record("EVENT_ABORT", actor.toString(), String.valueOf(sessionId));
	}
	public void complete() {
		if (state != DragonEventState.ALTAR_ACTIVE)
			return;
		transition(DragonEventState.COMPLETED);
		completedAt = Instant.now();
		save(null, null);
		setAltarState(AltarState.DORMANT);
		audit.record("EVENT_COMPLETED", "SYSTEM", "All three initial souls claimed");
	}
	public String rescan() {
		List<EnderDragon> matching = matchingSessionDragons();
		List<EnderCrystal> crystals = sessionCrystals();
		if (matching.size() > 1) {
			state = DragonEventState.RECOVERY_REQUIRED;
			save(null, "DUPLICATE_CANONICAL");
			notifyRecoveryRequired();
			return "Recovery required: multiple session dragons found";
		}
		if (matching.size() == 1) {
			dragonId = matching.getFirst().getUniqueId();
			if (state == DragonEventState.RECOVERY_REQUIRED)
				transition(DragonEventState.ACTIVE);
			else
				state = DragonEventState.ACTIVE;
			save(null, "RESCAN");
			return "Recovered canonical dragon " + dragonId;
		}
		return "No matching dragon; " + crystals.size() + " protected session crystals found";
	}
	public String recover() {
		if (state != DragonEventState.RECOVERY_REQUIRED)
			throw new IllegalStateException("Event is not awaiting recovery");
		List<EnderDragon> matching = matchingSessionDragons();
		List<EnderCrystal> crystals = sessionCrystals();
		return switch (RecoveryDecision.decide(souls.all().size(), matching.size(), crystals.size())) {
			case RESTORE_ALTAR -> {
				transition(DragonEventState.ALTAR_ACTIVE);
				save(null, "RECOVERED_ALTAR");
				if (souls.unclaimedCount() == 0) {
					complete();
					yield "Recovered completed event and dormant altar from persistent soul records";
				}
				setAltarState(AltarState.ACTIVE);
				yield "Recovered active altar from persistent soul records";
			}
			case RESTORE_ACTIVE_DRAGON -> {
				dragonId = matching.getFirst().getUniqueId();
				transition(DragonEventState.ACTIVE);
				save(null, "RESCAN");
				yield "Recovered canonical dragon " + dragonId;
			}
			case RESUME_SUMMONING -> {
				transition(DragonEventState.SUMMONING);
				save(null, "RECOVERED_SUMMONING");
				yield "Recovered summoning with four session crystals";
			}
			case REQUIRE_MANUAL_REPAIR -> {
				if (matching.size() > 1) {
					save(null, "DUPLICATE_CANONICAL");
					yield "Recovery required: multiple session dragons found";
				}
				yield "Recovery still required: expected four session crystals but found " + crystals.size()
						+ ". Use clear-crystals and abort/reset after inspection";
			}
			case ABORT -> {
				transition(DragonEventState.ABORTED);
				save(null, "RECOVERY_ABORT");
				yield "No canonical dragon or crystals found; event marked aborted";
			}
		};
	}
	public Optional<EnderDragon> canonicalDragon() {
		if (dragonId == null)
			return Optional.empty();
		for (World world : Bukkit.getWorlds()) {
			Entity entity = world.getEntity(dragonId);
			if (entity instanceof EnderDragon dragon)
				return Optional.of(dragon);
		}
		return Optional.empty();
	}
	public EnderDragon spawnTestDragon(Location location) {
		if (location.getWorld() == null)
			throw new IllegalArgumentException("World required");
		EnderDragon dragon = location.getWorld().spawn(location, EnderDragon.class, d -> {
			d.customName(net.kyori.adventure.text.Component.text("Test Ancient Dragon"));
			d.getPersistentDataContainer().set(testKey, PersistentDataType.BYTE, (byte) 1);
		});
		audit.record("TEST_DRAGON_SPAWN", "DEVELOPER", dragon.getUniqueId().toString());
		return dragon;
	}
	public void startTestVanilla(Location fountain, Collection<Location> locations) {
		if (fountain == null || fountain.getWorld() == null
				|| fountain.getWorld().getEnvironment() != World.Environment.THE_END)
			throw new IllegalArgumentException("Valid End fountain required");
		DragonBattle battle = Objects.requireNonNull(fountain.getWorld().getEnderDragonBattle(),
				"The configured world has no End dragon battle");
		if (battle.getRespawnPhase() != DragonBattle.RespawnPhase.NONE)
			throw new IllegalStateException("A vanilla dragon respawn sequence is already active");
		if (locations.size() != 4)
			throw new IllegalArgumentException("Four configured crystals required");
		if (!fountain.getWorld().getEntitiesByClass(EnderDragon.class).isEmpty())
			throw new IllegalStateException("An Ender Dragon is already active");
		for (Location location : locations) {
			location.getChunk().load();
			if (!location.getNearbyEntitiesByType(EnderCrystal.class, 1.25).isEmpty())
				throw new IllegalStateException("Crystal location occupied");
		}
		testVanillaUntil = System.currentTimeMillis() + 180_000;
		List<EnderCrystal> spawned = new ArrayList<>();
		for (Location location : locations)
			spawned.add(location.getWorld().spawn(location, EnderCrystal.class, c -> {
				c.setShowingBottom(true);
				c.getPersistentDataContainer().set(testCrystalKey, PersistentDataType.BYTE, (byte) 1);
			}));
		if (!battle.initiateRespawn(spawned)) {
			spawned.forEach(Entity::remove);
			throw new IllegalStateException("Paper rejected the configured crystals for the test respawn ritual");
		}
		audit.record("TEST_VANILLA_SUMMON", "DEVELOPER", "Test crystals placed; official progression unchanged");
	}
	public boolean isTestDragon(EnderDragon dragon) {
		return dragon.getPersistentDataContainer().has(testKey, PersistentDataType.BYTE);
	}
	public int clearTestDragons() {
		int count = 0;
		for (World world : Bukkit.getWorlds())
			for (EnderDragon dragon : world.getEntitiesByClass(EnderDragon.class))
				if (isTestDragon(dragon)) {
					dragon.remove();
					count++;
				}
		return count;
	}
	public int clearTestCrystals() {
		int count = 0;
		for (World world : Bukkit.getWorlds())
			for (EnderCrystal crystal : world.getEntitiesByClass(EnderCrystal.class))
				if (crystal.getPersistentDataContainer().has(testCrystalKey)) {
					crystal.remove();
					count++;
				}
		return count;
	}
	public void promoteTestDragon(EnderDragon dragon) {
		if (!isTestDragon(dragon))
			throw new IllegalArgumentException("Dragon is not a test dragon");
		if (state != DragonEventState.SUMMONING)
			throw new IllegalStateException("Official event must be summoning");
		dragon.getPersistentDataContainer().remove(testKey);
		trackSpawn(dragon);
		audit.record("TEST_DRAGON_PROMOTED", "DEVELOPER", dragon.getUniqueId().toString());
	}
	public void resetForBeta() {
		cleanupCrystals();
		clearTestCrystals();
		canonicalDragon().ifPresent(Entity::remove);
		state = DragonEventState.NOT_STARTED;
		sessionId = null;
		dragonId = null;
		startedAt = null;
		startedBy = null;
		completedAt = null;
		completionMethod = null;
		forceKilled = false;
		save(null, null);
		setAltarState(plugin.validateSetup().equals("Valid") ? AltarState.CONFIGURED : AltarState.UNCONFIGURED);
		audit.record("EVENT_RESET", "DEVELOPER", "Official event reset");
	}
	public void cleanupCrystals() {
		for (World world : Bukkit.getWorlds())
			for (EnderCrystal c : world.getEntitiesByClass(EnderCrystal.class))
				if (c.getPersistentDataContainer().has(crystalKey))
					c.remove();
	}
	private void save(UUID actor, String completion) {
		YamlConfiguration y = new YamlConfiguration();
		y.set("data-version", 1);
		String p = "dragon-event.";
		y.set(p + "state", state.name());
		y.set(p + "session-id", str(sessionId));
		y.set(p + "canonical-dragon-uuid", str(dragonId));
		y.set(p + "started-at", startedAt == null ? null : startedAt.toString());
		y.set(p + "started-by", str(startedBy));
		y.set(p + "completed-at", completedAt == null ? null : completedAt.toString());
		if (completion != null) {
			completionMethod = completion;
			forceKilled |= "SED_KILL".equals(completion);
		}
		y.set(p + "completion-method", completionMethod);
		y.set(p + "force-killed", forceKilled);
		World eventWorld = canonicalDragon().map(Entity::getWorld).orElseGet(() -> {
			Location fountain = plugin.configuredLocation("altar.yml", "fountain");
			return fountain == null
					? Bukkit.getWorld(plugin.getConfig().getString("event.end-world", "world_the_end"))
					: fountain.getWorld();
		});
		y.set(p + "world-uuid", eventWorld == null ? null : eventWorld.getUID().toString());
		y.set(p + "world-name",
				eventWorld == null
						? plugin.getConfig().getString("event.end-world", "world_the_end")
						: eventWorld.getName());
		y.set(p + "summoning-crystals",
				Bukkit.getWorlds().stream().flatMap(w -> w.getEntitiesByClass(EnderCrystal.class).stream())
						.filter(this::isEventCrystal).map(c -> c.getUniqueId().toString()).toList());
		store.save("event.yml", y);
	}
	private List<EnderDragon> matchingSessionDragons() {
		List<EnderDragon> matching = new ArrayList<>();
		for (World world : Bukkit.getWorlds())
			for (EnderDragon dragon : world.getEntitiesByClass(EnderDragon.class)) {
				String tag = dragon.getPersistentDataContainer().get(sessionKey, PersistentDataType.STRING);
				if (sessionId != null && sessionId.toString().equals(tag))
					matching.add(dragon);
			}
		return matching;
	}
	private List<EnderCrystal> sessionCrystals() {
		return Bukkit.getWorlds().stream().flatMap(world -> world.getEntitiesByClass(EnderCrystal.class).stream())
				.filter(this::isEventCrystal).toList();
	}
	private void notifyRecoveryRequired() {
		Bukkit.getOnlinePlayers().stream().filter(player -> player.hasPermission("dragonaltar.admin.event"))
				.forEach(player -> plugin.messages().send(player, "event-recovery-required"));
	}
	private static UUID uuid(String s) {
		try {
			return s == null ? null : UUID.fromString(s);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	private static Instant instant(String s) {
		try {
			return s == null ? null : Instant.parse(s);
		} catch (RuntimeException e) {
			return null;
		}
	}
	private static String str(UUID u) {
		return u == null ? null : u.toString();
	}
	private void transition(DragonEventState next) {
		DragonEventTransitions.require(state, next);
		state = next;
	}
	public void setAltarState(AltarState value) {
		AltarState previous = altarState;
		altarState = value;
		YamlConfiguration y = new YamlConfiguration();
		y.set("data-version", 1);
		y.set("altar.state", value.name());
		y.set("altar.updated-at", Instant.now().toString());
		store.save("altar-state.yml", y);
		if (previous != value) {
			if (value == AltarState.ACTIVE)
				Bukkit.getPluginManager().callEvent(new DragonAltarAwakenEvent());
			else if (value == AltarState.DORMANT)
				Bukkit.getPluginManager().callEvent(new DragonAltarDormantEvent());
		}
	}
}
