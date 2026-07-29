package com.dragonaltar.ritual;

import com.dragonaltar.DragonAltarPlugin;
import com.dragonaltar.persistence.YamlDataStore;
import com.dragonaltar.soul.DragonSoul;
import com.dragonaltar.soul.DragonSoulState;
import com.dragonaltar.soul.SoulIdentity;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class SoulConsequenceService implements Listener {
    private final DragonAltarPlugin plugin;
    private final YamlDataStore store;
    private final NamespacedKey fracturedSoulKey;
    private final Map<String, FracturedRecord> fractured = new HashMap<>();
    private final Map<String, LimboRecord> limbo = new HashMap<>();
    private final Map<String, BossBar> bars = new HashMap<>();
    private final Set<String> spawning = new HashSet<>();
    private final Set<String> respawnsScheduled = new HashSet<>();
    private long ritualCasts;
    private long lastCoordinateUpdate;
    private BukkitTask task;

    public SoulConsequenceService(DragonAltarPlugin plugin, YamlDataStore store) {
        this.plugin = plugin;
        this.store = store;
        this.fracturedSoulKey = new NamespacedKey(plugin, "fractured_soul");
    }

    public void load() {
        stop();
        fractured.clear();
        limbo.clear();
        spawning.clear();
        respawnsScheduled.clear();
        YamlConfiguration y = store.load("consequences.yml");
        ritualCasts = Math.max(0, y.getLong("instability-casts"));
        ConfigurationSection fracturedRoot = y.getConfigurationSection("fractured");
        if (fracturedRoot != null) for (String id : fracturedRoot.getKeys(false)) {
            ConfigurationSection s = fracturedRoot.getConfigurationSection(id);
            if (s == null || plugin.souls().byId(id).map(DragonSoul::state).orElse(null) != DragonSoulState.FRACTURED) continue;
            Location location = location(s);
            UUID entityId = uuid(s.getString("entity"));
            long nextTeleport = s.getLong("next-teleport", nextTeleportAt());
            fractured.put(id, new FracturedRecord(entityId, location, nextTeleport));
        }
        ConfigurationSection limboRoot = y.getConfigurationSection("limbo");
        if (limboRoot != null) for (String id : limboRoot.getKeys(false)) {
            ConfigurationSection s = limboRoot.getConfigurationSection(id);
            if (s == null || plugin.souls().byId(id).map(DragonSoul::state).orElse(null) != DragonSoulState.MOTHER_SOUL_LIMBO) continue;
            limbo.put(id, new LimboRecord(s.getLong("release-at"), uuid(s.getString("former-holder"))));
        }
        for (DragonSoul soul : plugin.souls().all()) {
            if (soul.state() == DragonSoulState.FRACTURED && !fractured.containsKey(soul.id())) {
                Location fallback = plugin.configuredLocation("altar.yml", "ritual-center");
                if (fallback == null) fallback = plugin.configuredLocation("altar.yml", "altar-center");
                fractured.put(soul.id(), new FracturedRecord(null, fallback, nextTeleportAt()));
            }
            if (soul.state() == DragonSoulState.MOTHER_SOUL_LIMBO && !limbo.containsKey(soul.id())) {
                long hours = Math.max(1, plugin.getConfig().getLong("forced-removal-ritual.backfire-limbo-hours", 12));
                limbo.put(soul.id(), new LimboRecord(System.currentTimeMillis() + Duration.ofHours(hours).toMillis(), null));
                plugin.audit().record("LIMBO_RECOVERY", "SYSTEM", soul.id() + " missing timer rebuilt");
            }
        }
        removeStaleFracturedEntities();
        recoverFractured();
        persist();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (BossBar bar : bars.values()) Bukkit.getOnlinePlayers().forEach(player -> player.hideBossBar(bar));
        bars.clear();
    }

    public void reset() {
        stop();
        ritualCasts = 0;
        fractured.clear();
        limbo.clear();
        spawning.clear();
        respawnsScheduled.clear();
        for (World world : Bukkit.getWorlds()) for (Entity entity : world.getEntities())
            if (entity instanceof WitherSkeleton skeleton && soulId(skeleton) != null) skeleton.remove();
        persist();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
    }

    public long recordRitualCast() {
        ritualCasts++;
        persist();
        plugin.audit().record("RITUAL_INSTABILITY", "SYSTEM", "cast=" + ritualCasts);
        return ritualCasts;
    }

    public long ritualCasts() { return ritualCasts; }

    public Optional<Instant> limboReleaseAt(String soulId) {
        LimboRecord record = limbo.get(soulId);
        return record == null ? Optional.empty() : Optional.of(Instant.ofEpochMilli(record.releaseAt()));
    }

    public Optional<UUID> limboFormerHolder(String soulId) {
        LimboRecord record = limbo.get(soulId);
        return record == null ? Optional.empty() : Optional.ofNullable(record.formerHolder());
    }

    public boolean shouldFracture() {
        long threshold = plugin.getConfig().getLong("instability.threshold", 6);
        if (!AddonRules.instabilityActive(ritualCasts, threshold)) return false;
        double chance = Math.max(0, Math.min(1, plugin.getConfig().getDouble("instability.fracture-chance", .20)));
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    public void manifest(String soulId, Location origin, String reason) {
        plugin.souls().fractured(soulId, reason);
        FracturedRecord existing = fractured.get(soulId);
        if (existing != null) {
            WitherSkeleton mob = reconcileLoaded(soulId);
            if (mob != null) {
                ensureBar(soulId, mob);
                updateBar(soulId, mob, true);
            }
            persist();
            plugin.audit().record("FRACTURED_MANIFEST_REUSED", "SYSTEM", soulId + " already tracked");
            return;
        }
        Location spawn = origin == null ? plugin.configuredLocation("altar.yml", "ritual-center") : origin.clone();
        FracturedRecord record = new FracturedRecord(null, spawn, nextTeleportAt());
        fractured.put(soulId, record);
        spawn(soulId, record);
        persist();
        Bukkit.broadcast(plugin.messages().component("fractured-soul-manifest",
                "soul", SoulIdentity.displayName(soulId)));
    }

    public void sendToLimbo(Collection<DragonSoul> souls, Duration duration, String reason) {
        long releaseAt = System.currentTimeMillis() + Math.max(1, duration.toMillis());
        for (DragonSoul soul : souls) {
            UUID formerHolder = soul.holder();
            plugin.souls().limbo(soul.id(), reason);
            limbo.put(soul.id(), new LimboRecord(releaseAt, formerHolder));
        }
        persist();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        boolean coordinatesDue = now - lastCoordinateUpdate >=
                Math.max(5, plugin.getConfig().getLong("instability.bossbar-coordinate-update-seconds", 7)) * 1000L;
        for (String soulId : new ArrayList<>(fractured.keySet())) {
            FracturedRecord record = fractured.get(soulId);
            WitherSkeleton mob = entity(record.entityId());
            if (mob == null) {
                if (FracturedSoulRules.waitForTrackedChunk(record.entityId(), false, trackedChunkLoaded(record)))
                    continue;
                mob = reconcileLoaded(soulId);
                if (mob != null) {
                    record = new FracturedRecord(mob.getUniqueId(), mob.getLocation(), record.nextTeleport());
                    fractured.put(soulId, record);
                }
            }
            if (mob == null || !mob.isValid()) {
                spawn(soulId, record);
                continue;
            }
            updateBar(soulId, mob, coordinatesDue);
            if (now >= record.nextTeleport()) teleport(soulId, mob);
        }
        if (coordinatesDue) lastCoordinateUpdate = now;
        releaseDueLimbo(now);
    }

    private void releaseDueLimbo(long now) {
        for (String soulId : new ArrayList<>(limbo.keySet())) {
            LimboRecord record = limbo.get(soulId);
            if (record.releaseAt() > now) continue;
            List<Player> candidates = plugin.eligibility().eligible(Bukkit.getOnlinePlayers()).stream()
                    .filter(player -> !player.getUniqueId().equals(record.formerHolder()))
                    .toList();
            if (candidates.isEmpty()) continue;
            Player recipient = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            try {
                plugin.souls().assign(soulId, recipient.getUniqueId(), "MOTHER_SOUL_LIMBO_RELEASE");
            } catch (RuntimeException ex) {
                plugin.audit().record("LIMBO_RELEASE_PREVENTED", "SYSTEM", soulId + " " + ex.getMessage());
                continue;
            }
            limbo.remove(soulId);
            plugin.dragonborn().apply(recipient);
            plugin.animations().play("soul-arrive", recipient.getLocation(), recipient);
            Bukkit.broadcast(plugin.messages().component("limbo-soul-released",
                    "player", recipient.getName(), "soul", SoulIdentity.displayName(soulId)));
            persist();
        }
    }

    private void recoverFractured() {
        for (Map.Entry<String, FracturedRecord> entry : new ArrayList<>(fractured.entrySet())) {
            WitherSkeleton mob = entity(entry.getValue().entityId());
            if (mob == null) {
                if (FracturedSoulRules.waitForTrackedChunk(entry.getValue().entityId(), false,
                        trackedChunkLoaded(entry.getValue()))) continue;
                mob = reconcileLoaded(entry.getKey());
                if (mob != null) fractured.put(entry.getKey(), new FracturedRecord(
                        mob.getUniqueId(), mob.getLocation(), entry.getValue().nextTeleport()));
            }
            if (mob == null || !mob.isValid()) spawn(entry.getKey(), entry.getValue());
            else {
                ensureBar(entry.getKey(), mob);
                updateBar(entry.getKey(), mob, true);
            }
        }
    }

    private void removeStaleFracturedEntities() {
        for (World world : Bukkit.getWorlds()) for (Entity entity : world.getEntities()) {
            if (!(entity instanceof WitherSkeleton skeleton)) continue;
            String soulId = soulId(skeleton);
            if (soulId == null) continue;
            if (!fractured.containsKey(soulId)) skeleton.remove();
        }
        for(String soulId:new ArrayList<>(fractured.keySet()))reconcileLoaded(soulId);
    }

    private void spawn(String soulId, FracturedRecord old) {
        if (!spawning.add(soulId)) return;
        try {
        if (plugin.souls().byId(soulId).map(DragonSoul::state).orElse(null) != DragonSoulState.FRACTURED
                || fractured.get(soulId) != old) return;
        Location location = old.location();
        if (location == null || location.getWorld() == null) return;
        location.getChunk().load();
        WitherSkeleton existing = reconcileLoaded(soulId);
        if (existing != null) {
            ensureBar(soulId, existing);
            updateBar(soulId, existing, true);
            persist();
            return;
        }
        location = safeLocation(location).orElse(location.clone().add(0, 1, 0));
        WitherSkeleton mob = location.getWorld().spawn(location, WitherSkeleton.class, spawned -> {
            spawned.customName(Component.text("Fractured " + SoulIdentity.displayName(soulId), NamedTextColor.DARK_RED));
            spawned.setCustomNameVisible(true);
            spawned.setPersistent(true);
            spawned.setRemoveWhenFarAway(false);
            spawned.getPersistentDataContainer().set(fracturedSoulKey, PersistentDataType.STRING, soulId);
            setBase(spawned, Attribute.MAX_HEALTH, 150);
            setBase(spawned, Attribute.MOVEMENT_SPEED, .2875);
            setBase(spawned, Attribute.ATTACK_DAMAGE, ThreadLocalRandom.current().nextDouble(8, 12.0001));
            setBase(spawned, Attribute.KNOCKBACK_RESISTANCE, .4);
            spawned.setHealth(150);
        });
        FracturedRecord record = new FracturedRecord(mob.getUniqueId(), mob.getLocation(), old.nextTeleport());
        fractured.put(soulId, record);
        ensureBar(soulId, mob);
        updateBar(soulId, mob, true);
        persist();
        } finally {
            spawning.remove(soulId);
        }
    }

    private void teleport(String soulId, WitherSkeleton mob) {
        Location from = mob.getLocation();
        Optional<Location> target = safeLocation(from);
        if (target.isEmpty()) {
            fractured.put(soulId, new FracturedRecord(mob.getUniqueId(), from, nextTeleportAt()));
            return;
        }
        burst(from);
        mob.teleport(target.get());
        burst(mob.getLocation());
        fractured.put(soulId, new FracturedRecord(mob.getUniqueId(), mob.getLocation(), nextTeleportAt()));
        updateBar(soulId, mob, true);
        persist();
    }

    private Optional<Location> safeLocation(Location center) {
        World world = center.getWorld();
        if (world == null) return Optional.empty();
        int radius = Math.max(8, plugin.getConfig().getInt("instability.teleport-radius", 32));
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
            double distance = ThreadLocalRandom.current().nextDouble(8, radius + .001);
            int x = (int) Math.floor(center.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(center.getZ() + Math.sin(angle) * distance);
            for (int vertical = 0; vertical <= 12; vertical++) {
                for (int sign : vertical == 0 ? new int[]{1} : new int[]{1, -1}) {
                    int y = center.getBlockY() + vertical * sign;
                    if (y <= world.getMinHeight() || y + 1 >= world.getMaxHeight()) continue;
                    Location candidate = new Location(world, x + .5, y, z + .5);
                    if (!world.getWorldBorder().isInside(candidate)) continue;
                    if (candidate.getBlock().isPassable() && candidate.clone().add(0, 1, 0).getBlock().isPassable()
                            && candidate.clone().add(0, -1, 0).getBlock().getType().isSolid()) return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    private void burst(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, location.clone().add(0, 1, 0), 45, .8, 1.0, .8, .04);
        world.spawnParticle(Particle.PORTAL, location.clone().add(0, 1, 0), 70, .8, 1.0, .8, .2);
        world.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, .65f);
    }

    private void ensureBar(String soulId, WitherSkeleton mob) {
        BossBar bar = bars.computeIfAbsent(soulId, id -> BossBar.bossBar(
                Component.empty(), 1, BossBar.Color.RED, BossBar.Overlay.PROGRESS));
        Bukkit.getOnlinePlayers().forEach(player -> player.showBossBar(bar));
        updateBar(soulId, mob, true);
    }

    private void updateBar(String soulId, WitherSkeleton mob, boolean coordinates) {
        BossBar bar = bars.get(soulId);
        if (bar == null) {
            ensureBar(soulId, mob);
            return;
        }
        AttributeInstance max = mob.getAttribute(Attribute.MAX_HEALTH);
        double maximum = max == null ? 150 : max.getValue();
        bar.progress((float) Math.max(0, Math.min(1, mob.getHealth() / maximum)));
        if (coordinates) {
            Location at = mob.getLocation();
            bar.name(Component.text("Fractured " + SoulIdentity.displayName(soulId) + " - "
                    + at.getBlockX() + ", " + at.getBlockY() + ", " + at.getBlockZ(), NamedTextColor.RED));
        }
    }

    @EventHandler
    public void join(PlayerJoinEvent event) {
        bars.values().forEach(event.getPlayer()::showBossBar);
    }

    @EventHandler
    public void chunkLoad(ChunkLoadEvent event) {
        Set<String> loadedSoulIds = new HashSet<>();
        for (Entity entity : event.getChunk().getEntities())
            if (entity instanceof WitherSkeleton skeleton && soulId(skeleton) != null)
                loadedSoulIds.add(soulId(skeleton));
        for (String soulId : loadedSoulIds) {
            WitherSkeleton canonical = reconcileLoaded(soulId);
            if (canonical != null) {
                ensureBar(soulId, canonical);
                updateBar(soulId, canonical, true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void preventBoundKillingBlow(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof WitherSkeleton mob) || soulId(mob) == null) return;
        Player attacker = attacker(event.getDamager());
        if (attacker == null || !plugin.dragonborn().isDragonborn(attacker.getUniqueId())) return;
        if (mob.getHealth() - event.getFinalDamage() > 0) return;
        event.setCancelled(true);
        plugin.messages().send(attacker, "fractured-soul-already-bound");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void death(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof WitherSkeleton mob)) return;
        String soulId = soulId(mob);
        if (soulId == null) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        FracturedRecord tracked = fractured.get(soulId);
        if (tracked == null || tracked.entityId() != null && !tracked.entityId().equals(mob.getUniqueId())) {
            plugin.audit().record("FRACTURED_DUPLICATE_DEATH_IGNORED", "SYSTEM",
                    soulId + " entity=" + mob.getUniqueId());
            return;
        }
        Player killer = mob.getKiller();
        removeBar(soulId);
        fractured.remove(soulId);
        if (killer == null || plugin.dragonborn().isDragonborn(killer.getUniqueId())) {
            Location respawn = mob.getLocation().clone();
            fractured.put(soulId, new FracturedRecord(null, respawn, nextTeleportAt()));
            respawnLater(soulId);
            persist();
            return;
        }
        try {
            plugin.souls().assign(soulId, killer.getUniqueId(), "FRACTURED_SOUL_KILL");
        } catch (RuntimeException ex) {
            Location respawn = mob.getLocation().clone();
            fractured.put(soulId, new FracturedRecord(null, respawn, nextTeleportAt()));
            respawnLater(soulId);
            plugin.audit().record("FRACTURED_CLAIM_PREVENTED", killer.getUniqueId().toString(), soulId + " " + ex.getMessage());
            persist();
            return;
        }
        plugin.dragonborn().apply(killer);
        plugin.animations().play("soul-arrive", killer.getLocation(), killer);
        Bukkit.broadcast(plugin.messages().component("fractured-soul-claimed",
                "player", killer.getName(), "soul", SoulIdentity.displayName(soulId)));
        persist();
    }

    private void removeBar(String soulId) {
        BossBar bar = bars.remove(soulId);
        if (bar != null) Bukkit.getOnlinePlayers().forEach(player -> player.hideBossBar(bar));
    }

    private void respawnLater(String soulId) {
        if (!respawnsScheduled.add(soulId)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                FracturedRecord record = fractured.get(soulId);
                if (record != null && plugin.souls().byId(soulId).map(DragonSoul::state).orElse(null) == DragonSoulState.FRACTURED)
                    spawn(soulId, record);
            } finally {
                respawnsScheduled.remove(soulId);
            }
        });
    }

    private String soulId(WitherSkeleton mob) {
        return mob.getPersistentDataContainer().get(fracturedSoulKey, PersistentDataType.STRING);
    }

    private static Player attacker(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    private WitherSkeleton entity(UUID id) {
        if (id == null) return null;
        for (World world : Bukkit.getWorlds()) {
            Entity entity = world.getEntity(id);
            if (entity instanceof WitherSkeleton skeleton) return skeleton;
        }
        return null;
    }

    private WitherSkeleton entity(String soulId) {
        for (World world : Bukkit.getWorlds()) for (Entity entity : world.getEntities())
            if (entity instanceof WitherSkeleton skeleton && soulId.equals(soulId(skeleton))) return skeleton;
        return null;
    }

    private WitherSkeleton reconcileLoaded(String soulId) {
        FracturedRecord record = fractured.get(soulId);
        List<WitherSkeleton> candidates = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) for (Entity entity : world.getEntities())
            if (entity instanceof WitherSkeleton skeleton && soulId.equals(soulId(skeleton)))
                candidates.add(skeleton);
        if (record == null) {
            candidates.forEach(Entity::remove);
            return null;
        }
        WitherSkeleton tracked = entity(record.entityId());
        if (FracturedSoulRules.waitForTrackedChunk(record.entityId(), tracked != null, trackedChunkLoaded(record))) {
            int removed=candidates.size();
            candidates.forEach(Entity::remove);
            if(removed>0)plugin.audit().record("FRACTURED_DUPLICATES_REMOVED","SYSTEM",
                    soulId+" removed="+removed+" while canonical chunk unloaded");
            return null;
        }
        UUID canonicalId=FracturedSoulRules.canonical(record.entityId(),
                candidates.stream().map(Entity::getUniqueId).toList());
        WitherSkeleton canonical=candidates.stream()
                .filter(candidate->candidate.getUniqueId().equals(canonicalId)).findFirst().orElse(null);
        int removed=0;
        for(WitherSkeleton candidate:candidates)if(candidate!=canonical){candidate.remove();removed++;}
        if(removed>0)plugin.audit().record("FRACTURED_DUPLICATES_REMOVED","SYSTEM",
                soulId+" kept="+canonicalId+" removed="+removed);
        if(canonical!=null&&!canonical.getUniqueId().equals(record.entityId())){
            fractured.put(soulId,new FracturedRecord(canonical.getUniqueId(),canonical.getLocation(),record.nextTeleport()));
            persist();
        }
        return canonical;
    }

    private boolean trackedChunkLoaded(FracturedRecord record) {
        Location location=record.location();
        if(location==null||location.getWorld()==null)return true;
        return location.getWorld().isChunkLoaded(location.getBlockX()>>4,location.getBlockZ()>>4);
    }

    private long nextTeleportAt() {
        long minimum = Math.max(1, plugin.getConfig().getLong("instability.teleport-min-seconds", 45));
        long maximum = Math.max(minimum, plugin.getConfig().getLong("instability.teleport-max-seconds", 60));
        return System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(minimum, maximum + 1) * 1000L;
    }

    private void persist() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("data-version", 1);
        y.set("instability-casts", ritualCasts);
        for (Map.Entry<String, FracturedRecord> entry : fractured.entrySet()) {
            String p = "fractured." + entry.getKey() + ".";
            FracturedRecord record = entry.getValue();
            y.set(p + "entity", record.entityId() == null ? null : record.entityId().toString());
            y.set(p + "next-teleport", record.nextTeleport());
            writeLocation(y, p, record.location());
        }
        for (Map.Entry<String, LimboRecord> entry : limbo.entrySet()) {
            String p = "limbo." + entry.getKey() + ".";
            y.set(p + "release-at", entry.getValue().releaseAt());
            y.set(p + "former-holder", entry.getValue().formerHolder() == null ? null : entry.getValue().formerHolder().toString());
        }
        store.save("consequences.yml", y);
    }

    private static void setBase(WitherSkeleton mob, Attribute attribute, double value) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    private static void writeLocation(YamlConfiguration y, String p, Location location) {
        if (location == null || location.getWorld() == null) return;
        y.set(p + "world", location.getWorld().getName());
        y.set(p + "world-uuid", location.getWorld().getUID().toString());
        y.set(p + "x", location.getX());
        y.set(p + "y", location.getY());
        y.set(p + "z", location.getZ());
    }

    private static Location location(ConfigurationSection s) {
        World world = null;
        UUID worldId = uuid(s.getString("world-uuid"));
        if (worldId != null) world = Bukkit.getWorld(worldId);
        if (world == null) world = Bukkit.getWorld(s.getString("world", ""));
        return world == null ? null : new Location(world, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"));
    }

    private static UUID uuid(String value) {
        try { return value == null ? null : UUID.fromString(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private record FracturedRecord(UUID entityId, Location location, long nextTeleport) {}
    private record LimboRecord(long releaseAt, UUID formerHolder) {}
}
