package com.dragonaltar.ritual;

import com.dragonaltar.DragonAltarPlugin;
import com.dragonaltar.soul.DragonSoul;
import com.dragonaltar.soul.SoulIdentity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A player-built, single-chest ritual which forcibly reincarnates one existing
 * Dragon Soul. Offerings remain in the chest until a target is confirmed.
 */
public final class DragonbornRemovalRitual implements Listener {
	private static final int PHANTOM_MEMBRANES = 128;
	private static final int NETHER_STARS = 1;
	private static final int NETHERITE_BLOCKS = 1;
	private static final int WEAKNESS_DURATION = 12 * 60 * 60 * 20;
	private static final int WEAKNESS_AMPLIFIER = 15;

	private final DragonAltarPlugin plugin;
	private final NamespacedKey targetKey;
	private final NamespacedKey weaknessUntilKey;
	private final Map<UUID, Session> sessions = new HashMap<>();
	private final Set<BlockKey> lockedChests = new HashSet<>();
	private final Set<UUID> animating = new HashSet<>();
	private final Map<UUID, BukkitTask> sessionTasks = new HashMap<>();
	private static final List<int[]> PAD_OFFSETS = List.of(new int[]{0, -2}, new int[]{2, 0}, new int[]{0, 2},
			new int[]{-2, 0});

	public DragonbornRemovalRitual(DragonAltarPlugin plugin) {
		this.plugin = plugin;
		this.targetKey = new NamespacedKey(plugin, "removal_ritual_target");
		this.weaknessUntilKey = new NamespacedKey(plugin, "removal_ritual_weakness_until");
	}

	public void stop() {
		sessionTasks.values().forEach(BukkitTask::cancel);
		sessionTasks.clear();
		for (UUID leaderId : new ArrayList<>(sessions.keySet())) {
			Player leader = Bukkit.getPlayer(leaderId);
			if (leader != null && leader.getOpenInventory().getTopInventory().getHolder(false) instanceof TargetHolder)
				leader.closeInventory();
		}
		sessions.clear();
		lockedChests.clear();
		animating.clear();
	}

	@EventHandler
	public void close(InventoryCloseEvent event) {
		if (event.getInventory().getHolder(false) instanceof TargetHolder) {
			if (event.getPlayer() instanceof Player player && !animating.contains(player.getUniqueId()))
				finishSession(player);
			return;
		}
		if (!(event.getPlayer() instanceof Player leader)
				|| !(event.getInventory().getHolder(false) instanceof Chest chest))
			return;
		if (!isSingleChest(chest) || !hasOfferings(event.getInventory()))
			return;
		Bukkit.getScheduler().runTask(plugin, () -> prepare(leader, chest));
	}

	private void prepare(Player leader, Chest chest) {
		if (!leader.isOnline() || chest.getBlock().getType() != Material.CHEST || !hasOfferings(chest.getInventory()))
			return;
		if (!isCompleteAltar(chest)) {
			plugin.messages().send(leader, "removal-ritual-invalid-altar");
			return;
		}
		BlockKey altar = BlockKey.of(chest.getLocation());
		if (lockedChests.contains(altar)) {
			plugin.messages().send(leader, "removal-ritual-busy");
			return;
		}

		List<Participant> participants = participantsAtPads(chest.getLocation());
		if (participants == null
				|| participants.stream().noneMatch(player -> player.id().equals(leader.getUniqueId()))) {
			plugin.messages().send(leader, "removal-ritual-participants", "count",
					Integer.toString(PAD_OFFSETS.size()));
			return;
		}
		if (plugin.souls().all().stream().noneMatch(soul -> soul.holder() != null)) {
			plugin.messages().send(leader, "removal-ritual-no-targets");
			return;
		}

		Session old = sessions.remove(leader.getUniqueId());
		if (old != null)
			lockedChests.remove(old.altar());
		Session session = new Session(altar, chest.getLocation().clone(), participants);
		sessions.put(leader.getUniqueId(), session);
		lockedChests.add(altar);
		openTargetMenu(leader, session);

		long timeout = plugin.getConfig().getLong("forced-removal-ritual.selection-timeout-seconds", 60);
		BukkitTask timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
			if (sessions.remove(leader.getUniqueId(), session)) {
				lockedChests.remove(session.altar());
				sessionTasks.remove(leader.getUniqueId());
				if (leader.isOnline())
					plugin.messages().send(leader, "removal-ritual-expired");
			}
		}, Math.max(1, timeout) * 20L);
		replaceSessionTask(leader.getUniqueId(), timeoutTask);
	}

	private void openTargetMenu(Player leader, Session session) {
		List<DragonSoul> targets = plugin.souls().all().stream().filter(soul -> soul.holder() != null).toList();
		int size = Math.max(9, ((targets.size() + 8) / 9) * 9);
		TargetHolder holder = new TargetHolder();
		Inventory menu = Bukkit.createInventory(holder, size,
				Component.text("Choose a Dragonborn", NamedTextColor.DARK_PURPLE));
		holder.inventory = menu;

		int slot = 0;
		for (DragonSoul soul : targets) {
			OfflinePlayer target = Bukkit.getOfflinePlayer(Objects.requireNonNull(soul.holder()));
			ItemStack head = new ItemStack(Material.PLAYER_HEAD);
			SkullMeta meta = (SkullMeta) head.getItemMeta();
			meta.setOwningPlayer(target);
			String name = target.getName() == null ? soul.holder().toString() : target.getName();
			meta.displayName(Component.text(name, NamedTextColor.RED));
			meta.lore(List.of(Component.text("Soul: " + SoulIdentity.displayName(soul.id()), NamedTextColor.GRAY),
					Component.text("Click to force this Dragonborn's soul away.", NamedTextColor.YELLOW),
					Component.text("Ritualists: " + participantNames(session), NamedTextColor.DARK_GRAY)));
			meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, soul.holder().toString());
			head.setItemMeta(meta);
			menu.setItem(slot++, head);
		}
		leader.openInventory(menu);
	}

	@EventHandler
	public void click(InventoryClickEvent event) {
		if (!(event.getView().getTopInventory().getHolder(false) instanceof TargetHolder))
			return;
		event.setCancelled(true);
		if (!(event.getWhoClicked() instanceof Player leader)
				|| event.getClickedInventory() != event.getView().getTopInventory() || event.getCurrentItem() == null)
			return;
		String rawTarget = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(targetKey,
				PersistentDataType.STRING);
		if (rawTarget == null)
			return;
		try {
			beginCeremony(leader, UUID.fromString(rawTarget));
		} catch (IllegalArgumentException ignored) {
			fail(leader, "That Dragonborn is no longer available.");
		}
	}

	private void beginCeremony(Player leader, UUID targetId) {
		Session session = sessions.get(leader.getUniqueId());
		if (session == null) {
			fail(leader, "This ritual has expired.");
			return;
		}
		DragonSoul soul = plugin.souls().byHolder(targetId).orElse(null);
		if (soul == null) {
			fail(leader, "That player is no longer Dragonborn.");
			return;
		}
		Chest chest = chest(session);
		if (chest == null || !hasOfferings(chest.getInventory())) {
			fail(leader, "The altar offerings have changed.");
			return;
		}
		List<Player> ritualists = validateParticipants(session);
		if (ritualists == null) {
			fail(leader, "All " + PAD_OFFSETS.size() + " ritualists must remain standing at the altar.");
			return;
		}
		if (!animating.add(leader.getUniqueId()))
			return;
		plugin.consequences().recordRitualCast();
		leader.closeInventory();
		Bukkit.broadcast(plugin.messages().component("removal-ritual-invocation", "ritualists",
				participantNames(session), "target", playerName(targetId)));
		animateCeremony(leader, session, targetId);
	}

	private void animateCeremony(Player leader, Session session, UUID targetId) {
		long duration = Math.max(40L,
				plugin.getConfig().getLong("forced-removal-ritual.animation-duration-ticks", 160L));
		BukkitTask ceremonyTask = new BukkitRunnable() {
			private long elapsed;
			@Override
			public void run() {
				if (!leader.isOnline() || sessions.get(leader.getUniqueId()) != session) {
					cancel();
					animating.remove(leader.getUniqueId());
					finishSession(leader);
					return;
				}
				particleStreams(session, elapsed);
				elapsed += 2;
				if (elapsed < duration)
					return;
				cancel();
				animating.remove(leader.getUniqueId());
				resolveCeremony(leader, targetId);
			}
		}.runTaskTimer(plugin, 0L, 2L);
		replaceSessionTask(leader.getUniqueId(), ceremonyTask);
	}

	private void resolveCeremony(Player leader, UUID targetId) {
		Session session = sessions.get(leader.getUniqueId());
		if (session == null)
			return;
		DragonSoul soul = plugin.souls().byHolder(targetId).orElse(null);
		Chest chest = chest(session);
		List<Player> ritualists = validateParticipants(session);
		if (soul == null || chest == null || !hasOfferings(chest.getInventory()) || ritualists == null) {
			fail(leader, "The Mother Soul fell silent because the altar, offerings, target, or ritualists changed.");
			return;
		}
		ItemStack[] before = cloneContents(chest.getInventory().getContents());
		consume(chest.getInventory(), Material.PHANTOM_MEMBRANE, PHANTOM_MEMBRANES);
		consumeWeaknessPotions(chest.getInventory(), PAD_OFFSETS.size());
		consume(chest.getInventory(), Material.NETHER_STAR, NETHER_STARS);
		consume(chest.getInventory(), Material.NETHERITE_BLOCK, NETHERITE_BLOCKS);
		List<DragonSoul> callerSouls = ritualists.stream().filter(player -> !player.getUniqueId().equals(targetId))
				.map(player -> plugin.souls().byHolder(player.getUniqueId()).orElse(null)).filter(Objects::nonNull)
				.toList();
		int backfireChance = AddonRules.backfireChancePercent(callerSouls.size());
		if (backfireChance > 0 && ThreadLocalRandom.current().nextInt(100) < backfireChance) {
			List<DragonSoul> soulsToLimbo = new ArrayList<>(callerSouls);
			if (AddonRules.totalBlackoutBackfire(callerSouls.size()))
				soulsToLimbo.add(soul);
			Map<String, Player> formerHolders = new HashMap<>();
			for (DragonSoul strippedSoul : soulsToLimbo) {
				Player oldHolder = Bukkit.getPlayer(Objects.requireNonNull(strippedSoul.holder()));
				if (oldHolder != null)
					formerHolders.put(strippedSoul.id(), oldHolder);
			}
			finishSession(leader);
			leader.closeInventory();
			applyCallerWeakness(ritualists);
			plugin.consequences().sendToLimbo(soulsToLimbo,
					Duration.ofHours(
							Math.max(1, plugin.getConfig().getLong("forced-removal-ritual.backfire-limbo-hours", 12))),
					withCallers("DRAGONBORN_CALLER_BACKFIRE", ritualists));
			for (DragonSoul strippedSoul : soulsToLimbo) {
				Player oldHolder = formerHolders.get(strippedSoul.id());
				if (oldHolder != null) {
					plugin.dragonborn().remove(oldHolder);
					plugin.animations().play("soul-depart", oldHolder.getLocation(), oldHolder);
				}
			}
			plugin.audit().record("REMOVAL_RITUAL_BACKFIRE", leader.getUniqueId().toString(),
					"chance=" + backfireChance + " souls=" + soulsToLimbo.stream().map(DragonSoul::id).toList());
			Bukkit.broadcast(plugin.messages().component("removal-ritual-backfire", "count",
					Integer.toString(soulsToLimbo.size()), "hours", Long.toString(Math.max(1,
							plugin.getConfig().getLong("forced-removal-ritual.backfire-limbo-hours", 12)))));
			if (soulsToLimbo.size() == SoulIdentity.MAX_DRAGONBORN)
				Bukkit.broadcast(plugin.messages().component("all-dragonborn-silenced", "hours", Long.toString(
						Math.max(1, plugin.getConfig().getLong("forced-removal-ritual.backfire-limbo-hours", 12)))));
			return;
		}

		if (plugin.consequences().shouldFracture()) {
			finishSession(leader);
			leader.closeInventory();
			applyCallerWeakness(ritualists);
			Player oldHolder = Bukkit.getPlayer(targetId);
			plugin.consequences().manifest(soul.id(),
					oldHolder == null ? session.chestLocation() : oldHolder.getLocation(),
					withCallers("FORCED_REMOVAL_RITUAL_INSTABILITY", ritualists));
			if (oldHolder != null)
				plugin.dragonborn().remove(oldHolder);
			plugin.animations().play("soul-depart",
					oldHolder == null ? session.chestLocation() : oldHolder.getLocation(), oldHolder);
			plugin.audit().record("FORCED_REMOVAL_FRACTURE", leader.getUniqueId().toString(), soul.id());
			return;
		}

		List<Player> candidates = plugin.eligibility().eligible(Bukkit.getOnlinePlayers()).stream()
				.filter(player -> !player.getUniqueId().equals(targetId)).toList();
		if (candidates.isEmpty()) {
			chest.getInventory().setContents(before);
			fail(leader, "There is no eligible online player to receive the Dragon Soul.");
			return;
		}
		Player recipient = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
		try {
			plugin.souls().assign(soul.id(), recipient.getUniqueId(), withCallers("FORCED_REMOVAL_RITUAL", ritualists));
		} catch (RuntimeException ex) {
			chest.getInventory().setContents(before);
			fail(leader, "The soul transfer was prevented; the offerings were restored.");
			return;
		}

		finishSession(leader);
		leader.closeInventory();
		applyCallerWeakness(ritualists);
		Player oldHolder = Bukkit.getPlayer(targetId);
		if (oldHolder != null)
			plugin.dragonborn().remove(oldHolder);
		plugin.dragonborn().apply(recipient);
		plugin.animations().play("soul-depart", oldHolder == null ? session.chestLocation() : oldHolder.getLocation(),
				oldHolder);
		plugin.animations().play("soul-arrive", recipient.getLocation(), recipient);
		plugin.audit().record("FORCED_REMOVAL_RITUAL", leader.getUniqueId().toString(),
				soul.id() + " " + targetId + " -> " + recipient.getUniqueId());
		Bukkit.broadcast(plugin.messages().component("removal-ritual-cleansing-complete", "target",
				playerName(targetId), "recipient", recipient.getName(), "soul", SoulIdentity.displayName(soul.id())));
	}

	private static String withCallers(String reason, Collection<Player> callers) {
		return reason + ";callers=" + callers.stream().map(player -> player.getUniqueId().toString())
				.collect(java.util.stream.Collectors.joining(","));
	}

	@EventHandler
	public void drag(InventoryDragEvent event) {
		if (event.getView().getTopInventory().getHolder(false) instanceof TargetHolder)
			event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void open(InventoryOpenEvent event) {
		if (event.getInventory().getHolder(false) instanceof Chest chest
				&& lockedChests.contains(BlockKey.of(chest.getLocation()))) {
			event.setCancelled(true);
			if (event.getPlayer() instanceof Player player)
				plugin.messages().send(player, "removal-ritual-busy");
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void breakChest(BlockBreakEvent event) {
		if (lockedChests.contains(BlockKey.of(event.getBlock().getLocation())))
			event.setCancelled(true);
	}

	@EventHandler
	public void join(PlayerJoinEvent event) {
		applyWeaknessRemaining(event.getPlayer());
	}

	@EventHandler
	public void respawn(PlayerRespawnEvent event) {
		Bukkit.getScheduler().runTask(plugin, () -> applyWeaknessRemaining(event.getPlayer()));
	}

	private void applyWeaknessRemaining(Player player) {
		Long until = player.getPersistentDataContainer().get(weaknessUntilKey, PersistentDataType.LONG);
		if (until == null)
			return;
		long remainingMillis = until - System.currentTimeMillis();
		if (remainingMillis <= 0) {
			player.getPersistentDataContainer().remove(weaknessUntilKey);
			return;
		}
		int ticks = (int) Math.min(Integer.MAX_VALUE, Math.max(1, (remainingMillis + 49L) / 50L));
		player.removePotionEffect(PotionEffectType.WEAKNESS);
		player.addPotionEffect(
				new PotionEffect(PotionEffectType.WEAKNESS, ticks, WEAKNESS_AMPLIFIER, false, true, true));
	}

	private void applyCallerWeakness(List<Player> ritualists) {
		long weaknessUntil = System.currentTimeMillis() + (WEAKNESS_DURATION * 50L);
		ritualists.forEach(player -> {
			player.getPersistentDataContainer().set(weaknessUntilKey, PersistentDataType.LONG, weaknessUntil);
			applyWeaknessRemaining(player);
		});
	}

	private List<Player> validateParticipants(Session session) {
		List<Player> result = new ArrayList<>();
		for (Participant participant : session.participants()) {
			Player player = Bukkit.getPlayer(participant.id());
			if (player == null || !player.isOnline() || !player.getWorld().equals(participant.position().getWorld())
					|| player.getLocation().distanceSquared(participant.position()) > 0.85 * 0.85)
				return null;
			result.add(player);
		}
		return result;
	}

	private List<Participant> participantsAtPads(Location chest) {
		List<Participant> result = new ArrayList<>();
		Set<UUID> used = new HashSet<>();
		for (int[] offset : PAD_OFFSETS) {
			Location standing = new Location(chest.getWorld(), chest.getBlockX() + offset[0] + 0.5,
					chest.getBlockY() - 0.1, chest.getBlockZ() + offset[1] + 0.5);
			Player occupant = chest.getWorld().getPlayers().stream().filter(Player::isOnline)
					.filter(player -> !used.contains(player.getUniqueId()))
					.filter(player -> player.getLocation().distanceSquared(standing) <= 0.85 * 0.85)
					.min(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(standing)))
					.orElse(null);
			if (occupant == null)
				return null;
			used.add(occupant.getUniqueId());
			result.add(new Participant(occupant.getUniqueId(), standing));
		}
		return List.copyOf(result);
	}

	private Chest chest(Session session) {
		return session.chestLocation().getBlock().getState() instanceof Chest chest && isSingleChest(chest)
				&& isCompleteAltar(chest) ? chest : null;
	}

	private void fail(Player leader, String message) {
		finishSession(leader);
		leader.closeInventory();
		leader.sendMessage(Component.text(message, NamedTextColor.RED));
	}

	private void finishSession(Player leader) {
		BukkitTask task = sessionTasks.remove(leader.getUniqueId());
		if (task != null)
			task.cancel();
		animating.remove(leader.getUniqueId());
		Session removed = sessions.remove(leader.getUniqueId());
		if (removed != null)
			lockedChests.remove(removed.altar());
	}

	private void replaceSessionTask(UUID leaderId, BukkitTask replacement) {
		BukkitTask previous = sessionTasks.put(leaderId, replacement);
		if (previous != null)
			previous.cancel();
	}

	private static boolean isSingleChest(Chest chest) {
		return chest.getBlockData() instanceof org.bukkit.block.data.type.Chest data
				&& data.getType() == org.bukkit.block.data.type.Chest.Type.SINGLE;
	}

	private static boolean hasOfferings(Inventory inventory) {
		return count(inventory, Material.PHANTOM_MEMBRANE) >= PHANTOM_MEMBRANES
				&& countWeaknessPotions(inventory) >= PAD_OFFSETS.size()
				&& count(inventory, Material.NETHER_STAR) >= NETHER_STARS
				&& count(inventory, Material.NETHERITE_BLOCK) >= NETHERITE_BLOCKS;
	}

	private static int count(Inventory inventory, Material material) {
		return Arrays.stream(inventory.getStorageContents()).filter(Objects::nonNull)
				.filter(item -> item.getType() == material).mapToInt(ItemStack::getAmount).sum();
	}

	private static void consume(Inventory inventory, Material material, int amount) {
		ItemStack[] contents = inventory.getStorageContents();
		for (int slot = 0; slot < contents.length && amount > 0; slot++) {
			ItemStack item = contents[slot];
			if (item == null || item.getType() != material)
				continue;
			int taken = Math.min(amount, item.getAmount());
			item.setAmount(item.getAmount() - taken);
			if (item.getAmount() == 0)
				contents[slot] = null;
			amount -= taken;
		}
		inventory.setStorageContents(contents);
	}

	private static int countWeaknessPotions(Inventory inventory) {
		return Arrays.stream(inventory.getStorageContents()).filter(DragonbornRemovalRitual::isWeaknessPotion)
				.mapToInt(ItemStack::getAmount).sum();
	}

	private static boolean isWeaknessPotion(ItemStack item) {
		if (item == null || item.getType() != Material.POTION || !(item.getItemMeta() instanceof PotionMeta meta))
			return false;
		PotionType type = meta.getBasePotionType();
		return type != null && (type.name().equals("WEAKNESS") || type.name().equals("LONG_WEAKNESS"));
	}

	private static void consumeWeaknessPotions(Inventory inventory, int amount) {
		ItemStack[] contents = inventory.getStorageContents();
		for (int slot = 0; slot < contents.length && amount > 0; slot++) {
			ItemStack item = contents[slot];
			if (!isWeaknessPotion(item))
				continue;
			int taken = Math.min(amount, item.getAmount());
			item.setAmount(item.getAmount() - taken);
			if (item.getAmount() == 0)
				contents[slot] = null;
			amount -= taken;
		}
		inventory.setStorageContents(contents);
	}

	private static boolean isCompleteAltar(Chest chest) {
		Location at = chest.getLocation();
		if (at.clone().add(0, -1, 0).getBlock().getType() != Material.CRYING_OBSIDIAN)
			return false;
		for (int[] offset : List.of(new int[]{0, -1}, new int[]{1, 0}, new int[]{0, 1}, new int[]{-1, 0}))
			if (at.clone().add(offset[0], -1, offset[1]).getBlock().getType() != Material.OBSIDIAN)
				return false;
		for (int[] offset : List.of(new int[]{1, 1}, new int[]{1, -1}, new int[]{-1, 1}, new int[]{-1, -1})) {
			if (at.clone().add(offset[0], -1, offset[1]).getBlock().getType() != Material.POLISHED_BLACKSTONE_BRICKS)
				return false;
			if (!(at.clone().add(offset[0], 0, offset[1]).getBlock()
					.getBlockData() instanceof org.bukkit.block.data.type.Candle candle)
					|| at.clone().add(offset[0], 0, offset[1]).getBlock().getType() != Material.WHITE_CANDLE
					|| !candle.isLit())
				return false;
		}
		for (int[] offset : PAD_OFFSETS)
			if (at.clone().add(offset[0], -1, offset[1]).getBlock().getType() != Material.SOUL_SAND)
				return false;
		return true;
	}

	private void particleStreams(Session session, long elapsed) {
		List<Player> players = validateParticipants(session);
		if (players == null)
			return;
		Location end = session.chestLocation().clone().add(0.5, 1.0, 0.5);
		Particle.DustOptions white = new Particle.DustOptions(Color.WHITE, 1.15f);
		double phase = (elapsed % 24L) / 24.0;
		for (Player player : players) {
			Location start = player.getLocation().clone().add(0, 1.0, 0);
			Vector path = end.toVector().subtract(start.toVector());
			for (int trail = 0; trail < 4; trail++) {
				double progress = phase - trail * 0.13;
				if (progress < 0)
					progress += 1.0;
				Location point = start.clone().add(path.clone().multiply(progress));
				player.getWorld().spawnParticle(org.bukkit.Particle.DUST, point, 1, 0.02, 0.02, 0.02, 0, white);
			}
		}
		end.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, end, 2, 0.25, 0.35, 0.25, 0.01);
	}

	private static ItemStack[] cloneContents(ItemStack[] contents) {
		ItemStack[] clone = new ItemStack[contents.length];
		for (int i = 0; i < contents.length; i++)
			clone[i] = contents[i] == null ? null : contents[i].clone();
		return clone;
	}

	private static String participantNames(Session session) {
		return session.participants().stream().map(participant -> playerName(participant.id()))
				.reduce((left, right) -> left + ", " + right).orElse("");
	}

	private static String playerName(UUID id) {
		String name = Bukkit.getOfflinePlayer(id).getName();
		return name == null ? id.toString() : name;
	}

	private record Participant(UUID id, Location position) {
	}
	private record Session(BlockKey altar, Location chestLocation, List<Participant> participants) {
	}
	private record BlockKey(UUID world, int x, int y, int z) {
		private static BlockKey of(Location location) {
			return new BlockKey(Objects.requireNonNull(location.getWorld()).getUID(), location.getBlockX(),
					location.getBlockY(), location.getBlockZ());
		}
	}
	private static final class TargetHolder implements InventoryHolder {
		private Inventory inventory;
		@Override
		public Inventory getInventory() {
			return inventory;
		}
	}
}
