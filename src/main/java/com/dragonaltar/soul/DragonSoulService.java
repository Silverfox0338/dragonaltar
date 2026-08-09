package com.dragonaltar.soul;

import com.dragonaltar.audit.AuditService;
import com.dragonaltar.persistence.YamlDataStore;
import com.dragonaltar.api.event.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.time.Instant;
import java.util.*;

public final class DragonSoulService {
	private final YamlDataStore store;
	private final AuditService audit;
	private final Map<String, DragonSoul> souls = new LinkedHashMap<>();

	public DragonSoulService(YamlDataStore store, AuditService audit) {
		this.store = store;
		this.audit = audit;
	}

	public synchronized void load() {
		souls.clear();
		YamlConfiguration y = store.load("souls.yml");
		ConfigurationSection root = y.getConfigurationSection("souls");
		if (root == null)
			return;
		boolean loadRepaired = false;
		for (String id : root.getKeys(false)) {
			ConfigurationSection s = root.getConfigurationSection(id);
			if (s == null || !SoulIdentity.CANONICAL_IDS.contains(id))
				continue;
			try {
				souls.put(id,
						new DragonSoul(id, DragonSoulState.valueOf(s.getString("state", "UNCLAIMED")),
								uuid(s.getString("holder")), uuid(s.getString("reserved-for")),
								Instant.parse(s.getString("created-at")), instant(s.getString("claimed-at")),
								s.getInt("generation"), s.getInt("transfer-count"), s.getStringList("lineage")));
			} catch (RuntimeException ex) {
				DragonSoul replacement = DragonSoul.unclaimed(id);
				replacement.pending("LOAD_CORRUPTION");
				souls.put(id, replacement);
				loadRepaired = true;
				audit.record("SOUL_REPAIR", "SYSTEM",
						id + " malformed record replaced as pending: " + ex.getClass().getSimpleName());
			}
		}
		int repaired = repair();
		if (loadRepaired && repaired == 0)
			persist();
	}

	public synchronized void createInitialSouls() {
		if (!souls.isEmpty())
			throw new IllegalStateException("Souls already exist");
		for (SoulIdentity identity : SoulIdentity.values()) {
			String id = identity.id();
			souls.put(id, DragonSoul.unclaimed(id));
			Bukkit.getPluginManager().callEvent(new DragonSoulCreateEvent(id));
		}
		persist();
		audit.record("SOULS_CREATED", "SYSTEM", "Created exactly three initial souls");
	}

	public synchronized Optional<DragonSoul> byId(String id) {
		return Optional.ofNullable(souls.get(id));
	}
	public synchronized Optional<DragonSoul> byHolder(UUID holder) {
		return souls.values().stream().filter(s -> holder.equals(s.holder())).findFirst();
	}
	public synchronized List<DragonSoul> all() {
		return List.copyOf(souls.values());
	}
	public synchronized long unclaimedCount() {
		return souls.values().stream().filter(s -> s.state() == DragonSoulState.UNCLAIMED).count();
	}
	public synchronized Optional<DragonSoul> reserveFirst(UUID player) {
		if (byHolder(player).isPresent())
			return Optional.empty();
		Optional<DragonSoul> soul = souls.values().stream().filter(s -> s.state() == DragonSoulState.UNCLAIMED)
				.findFirst();
		soul = soul.filter(s -> {
			DragonSoulReserveEvent event = new DragonSoulReserveEvent(s.id(), player);
			Bukkit.getPluginManager().callEvent(event);
			return !event.isCancelled();
		});
		soul.ifPresent(s -> {
			s.reserve(player);
			persist();
			audit.record("SOUL_RESERVED", player.toString(), s.id());
		});
		return soul;
	}
	public synchronized void reserve(String id, UUID player) {
		if (byHolder(player).isPresent())
			throw new IllegalStateException("Player already holds a soul");
		DragonSoul soul = require(id);
		DragonSoulReserveEvent event = new DragonSoulReserveEvent(id, player);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled())
			throw new IllegalStateException("Soul reservation cancelled by another plugin");
		soul.reserve(player);
		persist();
		audit.record("SOUL_RESERVED", player.toString(), id);
	}
	public synchronized void assign(String id, UUID player, String reason) {
		DragonSoul target = require(id);
		Optional<DragonSoul> duplicate = byHolder(player);
		if (duplicate.isPresent() && duplicate.get() != target)
			throw new IllegalStateException("Player already holds a soul");
		long embodied = souls.values().stream().filter(soul -> soul.holder() != null).count();
		if (target.holder() == null && duplicate.isEmpty() && embodied >= SoulIdentity.MAX_DRAGONBORN)
			throw new IllegalStateException("Only three Dragonborn may exist at once");
		UUID old = target.holder();
		if (old != null && !old.equals(player)) {
			DragonSoulTransferStartEvent startEvent = new DragonSoulTransferStartEvent(id, old, player);
			Bukkit.getPluginManager().callEvent(startEvent);
			if (startEvent.isCancelled())
				throw new IllegalStateException("Soul transfer cancelled by another plugin");
			DragonSoulTransferEvent compatibilityEvent = new DragonSoulTransferEvent(id, old, player);
			Bukkit.getPluginManager().callEvent(compatibilityEvent);
			if (compatibilityEvent.isCancelled())
				throw new IllegalStateException("Soul transfer cancelled by another plugin");
		}
		target.assign(player, reason);
		persist();
		audit.record("SOUL_ASSIGNED", player.toString(), id + " " + reason);
		if (old == null)
			Bukkit.getPluginManager().callEvent(new DragonbornGainEvent(player, id));
		else if (!old.equals(player)) {
			Bukkit.getPluginManager().callEvent(new DragonbornLoseEvent(old, id));
			Bukkit.getPluginManager().callEvent(new DragonbornGainEvent(player, id));
			Bukkit.getPluginManager().callEvent(new DragonSoulTransferCompleteEvent(id, old, player));
		}
	}
	public synchronized void pending(String id, String reason) {
		DragonSoul soul = require(id);
		UUID old = soul.holder();
		soul.pending(reason);
		persist();
		audit.record("SOUL_PENDING", "SYSTEM", id + " " + reason);
		if (old != null)
			Bukkit.getPluginManager().callEvent(new DragonbornLoseEvent(old, id));
	}
	public synchronized void fractured(String id, String reason) {
		DragonSoul soul = require(id);
		UUID old = soul.holder();
		soul.fracture(reason);
		persist();
		audit.record("SOUL_FRACTURED", "SYSTEM", id + " " + reason);
		if (old != null)
			Bukkit.getPluginManager().callEvent(new DragonbornLoseEvent(old, id));
	}
	public synchronized void limbo(String id, String reason) {
		DragonSoul soul = require(id);
		UUID old = soul.holder();
		soul.limbo(reason);
		persist();
		audit.record("SOUL_LIMBO", "SYSTEM", id + " " + reason);
		if (old != null)
			Bukkit.getPluginManager().callEvent(new DragonbornLoseEvent(old, id));
	}
	public synchronized void unclaimed(String id, String reason) {
		DragonSoul soul = require(id);
		UUID old = soul.holder();
		soul.makeUnclaimed(reason);
		persist();
		audit.record("SOUL_UNCLAIMED", "SYSTEM", id + " " + reason);
		if (old != null)
			Bukkit.getPluginManager().callEvent(new DragonbornLoseEvent(old, id));
	}
	public synchronized void removeHolder(UUID player, String reason) {
		DragonSoul soul = byHolder(player).orElseThrow(() -> new IllegalArgumentException("Player holds no soul"));
		pending(soul.id(), reason);
	}
	public synchronized void create(String id) {
		if (!SoulIdentity.CANONICAL_IDS.contains(id))
			throw new IllegalArgumentException("Only soul-1, soul-2, and soul-3 may exist");
		if (souls.size() >= SoulIdentity.MAX_DRAGONBORN)
			throw new IllegalStateException("Three souls already exist");
		if (souls.containsKey(id))
			throw new IllegalStateException("Soul already exists");
		DragonSoul soul = DragonSoul.unclaimed(id);
		souls.put(id, soul);
		persist();
		Bukkit.getPluginManager().callEvent(new DragonSoulCreateEvent(id));
	}
	public synchronized void delete(String id) {
		DragonSoul removed = souls.remove(id);
		if (removed == null)
			throw new IllegalArgumentException("Unknown soul");
		persist();
		audit.record("SOUL_DELETED", "ADMIN", id);
		if (removed.holder() != null)
			Bukkit.getPluginManager().callEvent(new DragonbornLoseEvent(removed.holder(), id));
	}
	public synchronized void reset() {
		List<DragonSoul> previous = List.copyOf(souls.values());
		souls.clear();
		persist();
		audit.record("SOULS_RESET", "ADMIN", "All souls removed");
		for (DragonSoul soul : previous)
			if (soul.holder() != null)
				Bukkit.getPluginManager().callEvent(new DragonbornLoseEvent(soul.holder(), soul.id()));
	}
	public synchronized void setState(String id, DragonSoulState state) {
		setState(id, state, "");
	}
	public synchronized void setState(String id, DragonSoulState state, String metadata) {
		String reason = "DEV_SETSTATE" + (metadata == null ? "" : metadata);
		switch (state) {
			case UNCLAIMED -> unclaimed(id, reason);
			case TRANSFER_PENDING -> pending(id, reason);
			case FRACTURED -> fractured(id, reason);
			case MOTHER_SOUL_LIMBO -> limbo(id, reason);
			case DISABLED -> {
				require(id).disable(reason);
				persist();
			}
			case UNCREATED -> delete(id);
			default -> throw new IllegalArgumentException("Use reserve/assign commands for " + state);
		}
	}
	public synchronized void disable(String id, String reason) {
		DragonSoul soul = require(id);
		UUID old = soul.holder();
		soul.disable(reason);
		persist();
		audit.record("SOUL_DISABLED", "SYSTEM", id + " " + reason);
		if (old != null)
			Bukkit.getPluginManager().callEvent(new DragonbornLoseEvent(old, id));
	}
	public synchronized void clearHistory() {
		souls.values().forEach(DragonSoul::clearLineage);
		persist();
		audit.record("HISTORY_RESET", "ADMIN", "Soul lineage cleared");
	}
	public synchronized void release(String id) {
		require(id).release();
		persist();
		audit.record("SOUL_RELEASED", "SYSTEM", id);
	}
	public synchronized void validate() {
		SoulRules.validate(souls.values());
	}
	public synchronized int repair() {
		int repaired = 0;
		Set<UUID> holders = new HashSet<>();
		for (DragonSoul soul : souls.values()) {
			if (soul.repair()) {
				repaired++;
				audit.record("SOUL_REPAIR", "SYSTEM", soul.id() + " invalid state repaired");
			}
			if (soul.holder() != null && !holders.add(soul.holder())) {
				soul.pending("DUPLICATE_HOLDER_REPAIR");
				repaired++;
				audit.record("SOUL_REPAIR", "SYSTEM", soul.id() + " duplicate holder moved pending");
			}
		}
		validate();
		if (repaired > 0)
			persist();
		return repaired;
	}
	private DragonSoul require(String id) {
		return Optional.ofNullable(souls.get(id)).orElseThrow(() -> new IllegalArgumentException("Unknown soul"));
	}
	private void persist() {
		validate();
		YamlConfiguration y = new YamlConfiguration();
		y.set("data-version", 1);
		for (DragonSoul s : souls.values()) {
			String p = "souls." + s.id() + ".";
			y.set(p + "state", s.state().name());
			y.set(p + "holder", str(s.holder()));
			y.set(p + "reserved-for", str(s.reservedFor()));
			y.set(p + "created-at", s.createdAt().toString());
			y.set(p + "claimed-at", s.claimedAt() == null ? null : s.claimedAt().toString());
			y.set(p + "generation", s.generation());
			y.set(p + "transfer-count", s.transferCount());
			y.set(p + "lineage", s.lineage());
		}
		store.save("souls.yml", y);
		YamlConfiguration pending = new YamlConfiguration();
		pending.set("data-version", 1);
		for (DragonSoul soul : souls.values())
			if (soul.state() == DragonSoulState.TRANSFER_PENDING) {
				pending.set("pending." + soul.id() + ".state", soul.state().name());
				pending.set("pending." + soul.id() + ".lineage-size", soul.lineage().size());
			}
		store.save("pending-transfers.yml", pending);
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
}
