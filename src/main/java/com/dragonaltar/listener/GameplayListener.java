package com.dragonaltar.listener;

import com.dragonaltar.ability.*;
import com.dragonaltar.DragonAltarPlugin;
import com.dragonaltar.dragonborn.DragonbornService;
import com.dragonaltar.dragonborn.CombatTagService;
import com.dragonaltar.dragonborn.FocusProtectionRules;
import com.dragonaltar.player.SelectorMode;
import com.dragonaltar.api.event.DragonbornGainEvent;
import com.dragonaltar.api.event.DragonbornLoseEvent;
import com.dragonaltar.dragonevent.DragonEventManager;
import com.dragonaltar.eligibility.EligibilityService;
import com.dragonaltar.soul.*;
import com.dragonaltar.soul.SoulIdentity;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;

import java.util.*;

public final class GameplayListener implements Listener {
	private final DragonAltarPlugin plugin;
	private final DragonEventManager event;
	private final DragonSoulService souls;
	private final DragonbornService dragonborn;
	private final AbilityService abilities;
	private final EligibilityService eligibility;
	private final CombatTagService combatTags;
	private final Set<String> scheduledTransfers = new HashSet<>();
	public GameplayListener(DragonAltarPlugin plugin, DragonEventManager event, DragonSoulService souls,
			DragonbornService dragonborn, AbilityService abilities, EligibilityService eligibility,
			CombatTagService combatTags) {
		this.plugin = plugin;
		this.event = event;
		this.souls = souls;
		this.dragonborn = dragonborn;
		this.abilities = abilities;
		this.eligibility = eligibility;
		this.combatTags = combatTags;
	}
	@EventHandler
	public void spawn(CreatureSpawnEvent e) {
		if (e.getEntity() instanceof EnderDragon d && !event.isTestDragon(d))
			event.trackVanillaSpawn(d, e.getSpawnReason());
	}
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void protectCrystal(EntityDamageEvent e) {
		if (e.getEntity() instanceof EnderCrystal crystal && event.isEventCrystal(crystal))
			e.setCancelled(true);
	}
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void protectDisplay(EntityDamageEvent e) {
		if (plugin.displays().owns(e.getEntity()))
			e.setCancelled(true);
	}
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void protectDisplayInteraction(org.bukkit.event.player.PlayerInteractEntityEvent e) {
		if (plugin.displays().owns(e.getRightClicked()))
			e.setCancelled(true);
	}
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void protectCrystalExplosion(EntityExplodeEvent e) {
		if (e.getEntity() instanceof EnderCrystal crystal && event.isEventCrystal(crystal))
			e.setCancelled(true);
	}
	@EventHandler(priority = EventPriority.MONITOR)
	public void death(EntityDeathEvent e) {
		if (e.getEntity() instanceof EnderDragon d)
			event.defeated(d, plugin.scaledDragon().completionMethod());
	}
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerCommand(PlayerCommandPreprocessEvent e) {
		plugin.scaledDragon().observeCommand(e.getMessage());
	}
	@EventHandler(priority = EventPriority.LOWEST)
	public void protectFocusCommand(PlayerCommandPreprocessEvent e) {
		Player player = e.getPlayer();
		List<String> blocked = plugin.configService().file("abilities.yml")
				.getStringList("focus.blocked-command-prefixes");
		List<String> bulkBlocked = plugin.configService().file("abilities.yml")
				.getStringList("focus.blocked-inventory-command-prefixes");
		boolean heldFocus = dragonborn.isFocus(player.getInventory().getItemInMainHand())
				|| dragonborn.isFocus(player.getInventory().getItemInOffHand())
				|| dragonborn.isFocus(player.getItemOnCursor());
		boolean inventoryFocus = heldFocus
				|| Arrays.stream(player.getInventory().getContents()).anyMatch(dragonborn::isFocus);
		if (!(heldFocus && FocusProtectionRules.blocksCommand(e.getMessage(), blocked))
				&& !(inventoryFocus && FocusProtectionRules.blocksCommand(e.getMessage(), bulkBlocked)))
			return;
		e.setCancelled(true);
		plugin.messages().send(player, "focus-protected");
		plugin.audit().record("FOCUS_COMMAND_BLOCKED", player.getUniqueId().toString(), e.getMessage());
	}
	@EventHandler(priority = EventPriority.MONITOR)
	public void serverCommand(ServerCommandEvent e) {
		plugin.scaledDragon().observeCommand(e.getCommand());
	}
	@EventHandler
	public void join(PlayerJoinEvent e) {
		eligibility.markJoined(e.getPlayer());
		if (!plugin.validateSetup().equals("Valid") && e.getPlayer().hasPermission("dragonaltar.setup"))
			plugin.messages().send(e.getPlayer(), "setup-incomplete");
		if (event.state() == com.dragonaltar.dragonevent.DragonEventState.RECOVERY_REQUIRED
				&& e.getPlayer().hasPermission("dragonaltar.admin.event"))
			plugin.messages().send(e.getPlayer(), "event-recovery-required");
		Bukkit.getScheduler().runTask(plugin, () -> {
			dragonborn.apply(e.getPlayer());
			plugin.rituals().refundPending(e.getPlayer());
			assignPending(e.getPlayer());
		});
		long grace = plugin.getConfig().getLong("eligibility.recently-joined-grace-seconds", 0);
		if (grace > 0)
			Bukkit.getScheduler().runTaskLater(plugin, () -> assignPending(e.getPlayer()), grace * 20 + 1);
	}
	private void assignPending(Player player) {
		if (!player.isOnline())
			return;
		for (DragonSoul soul : souls.all())
			if (soul.state() == DragonSoulState.TRANSFER_PENDING && !scheduledTransfers.contains(soul.id())
					&& eligibility.check(player).eligible()) {
				souls.assign(soul.id(), player.getUniqueId(), "JOIN_PENDING");
				dragonborn.apply(player);
				plugin.animations().play("soul-arrive", player.getLocation(), player);
				Bukkit.broadcast(plugin.messages().component("reincarnation", "player", player.getName(), "soul",
						SoulIdentity.displayName(soul.id())));
				break;
			}
	}
	@EventHandler
	public void respawn(PlayerRespawnEvent e) {
		Bukkit.getScheduler().runTaskLater(plugin, () -> dragonborn.apply(e.getPlayer()), 1);
	}
	@EventHandler
	public void worldChange(PlayerChangedWorldEvent e) {
		Bukkit.getScheduler().runTask(plugin, () -> dragonborn.apply(e.getPlayer()));
	}
	@EventHandler
	public void dragonbornGain(DragonbornGainEvent e) {
		plugin.players().recordHistory(e.player(), "GAIN", e.soulId());
		Player player = Bukkit.getPlayer(e.player());
		if (player != null) {
			dragonborn.apply(player);
			abilities.setEnergy(player, abilities.maxEnergy());
			abilities.select(player, "wings");
		}
	}
	@EventHandler
	public void dragonbornLose(DragonbornLoseEvent e) {
		plugin.players().recordHistory(e.player(), "LOSE", e.soulId());
		Player player = Bukkit.getPlayer(e.player());
		if (player != null) {
			abilities.resetCombatState(player);
			dragonborn.remove(player);
		}
	}
	@EventHandler(ignoreCancelled = true)
	public void ritualMove(PlayerMoveEvent e) {
		plugin.rituals().active().filter(r -> r.playerId().equals(e.getPlayer().getUniqueId())).ifPresent(r -> {
			if (plugin.configService().file("ritual.yml").getBoolean("restrict-movement", true)
					&& e.hasChangedPosition()) {
				Location from = e.getFrom(), to = e.getTo();
				e.setTo(new Location(from.getWorld(), from.getX(), from.getY(), from.getZ(), to.getYaw(),
						to.getPitch()));
			}
		});
	}
	@EventHandler
	public void playerDeath(PlayerDeathEvent e) {
		Player victim = e.getPlayer();
		abilities.resetCombatState(victim);
		e.getDrops().removeIf(dragonborn::isFocus);
		Optional<DragonSoul> held = souls.byHolder(victim.getUniqueId());
		if (held.isEmpty())
			return;
		DragonSoul soul = held.get();
		Player killer = victim.getKiller();
		if (killer != null && !dragonborn.isDragonborn(killer.getUniqueId()) && eligibility.check(killer).eligible()) {
			souls.assign(soul.id(), killer.getUniqueId(), "PVP_INHERITANCE");
			dragonborn.remove(victim);
			dragonborn.apply(killer);
			plugin.animations().play("soul-depart", victim.getLocation(), victim);
			plugin.animations().play("pvp-transfer", killer.getLocation(), killer);
			Bukkit.broadcast(plugin.messages().component("dragonborn-transfer", "from", victim.getName(), "soul",
					SoulIdentity.displayName(soul.id()), "to", killer.getName()));
		} else if (killer != null && dragonborn.isDragonborn(killer.getUniqueId())) {
			String policy = plugin.getConfig().getString("transfer.dragonborn-killer-policy", "RANDOM_ELIGIBLE");
			dragonborn.remove(victim);
			String killerContext = ";killer=" + killer.getUniqueId();
			if ("OPEN_RITUAL_SLOT".equalsIgnoreCase(policy)) {
				souls.unclaimed(soul.id(), "DRAGONBORN_KILLER" + killerContext);
				plugin.dragonEvent().setAltarState(com.dragonaltar.altar.AltarState.ACTIVE);
				plugin.displays().resetPreview();
			} else if ("SOUL_DORMANT".equalsIgnoreCase(policy))
				souls.disable(soul.id(), "DRAGONBORN_KILLER_DORMANT" + killerContext);
			else {
				souls.pending(soul.id(), "DRAGONBORN_KILLER_" + policy + killerContext);
				if ("RANDOM_ELIGIBLE".equalsIgnoreCase(policy))
					scheduleReincarnation(soul);
			}
		} else {
			souls.pending(soul.id(), "NATURAL_DEATH");
			dragonborn.remove(victim);
			plugin.animations().play("soul-depart", victim.getLocation(), victim);
			scheduleReincarnation(soul);
		}
		combatTags.clear(victim.getUniqueId());
	}
	private void scheduleReincarnation(DragonSoul soul) {
		if (!scheduledTransfers.add(soul.id()))
			return;
		int countdown = plugin.getConfig().getInt("transfer.natural-death-countdown-seconds", 10);
		Bukkit.broadcast(plugin.messages().component("natural-death", "seconds", Integer.toString(countdown)));
		for (int remaining = countdown; remaining > 0; remaining--) {
			int value = remaining;
			Bukkit.getScheduler().runTaskLater(plugin,
					() -> Bukkit.broadcast(
							plugin.messages().component("natural-countdown", "seconds", Integer.toString(value))),
					(countdown - remaining) * 20L);
		}
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			scheduledTransfers.remove(soul.id());
			DragonSoul current = souls.byId(soul.id()).orElse(null);
			if (current == null || current.state() != DragonSoulState.TRANSFER_PENDING)
				return;
			List<Player> candidates = eligibility.eligible(Bukkit.getOnlinePlayers());
			if (candidates.isEmpty()) {
				Bukkit.broadcast(plugin.messages().component("no-eligible-recipient"));
				return;
			}
			Player chosen = candidates.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(candidates.size()));
			souls.assign(soul.id(), chosen.getUniqueId(), "NATURAL_REINCARNATION");
			dragonborn.apply(chosen);
			plugin.animations().play("soul-arrive", chosen.getLocation(), chosen);
			plugin.animations().play("natural-transfer", chosen.getLocation(), chosen);
			Bukkit.broadcast(plugin.messages().component("reincarnation", "player", chosen.getName(), "soul",
					SoulIdentity.displayName(soul.id())));
		}, countdown * 20L);
	}
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void combat(EntityDamageByEntityEvent e) {
		abilities.handleCombatEffects(e);
		abilities.handleResonanceRetaliation(e);
		abilities.handleBulwarkMelee(e);
		if (e.getEntity() instanceof Player victim) {
			Player attacker = null;
			if (e.getDamager() instanceof Player p)
				attacker = p;
			else if (e.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p)
				attacker = p;
			if (attacker != null && attacker != victim)
				combatTags.tag(attacker, victim);
		}
	}
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void bulwarkPreventedDamage(EntityDamageEvent e) {
		abilities.handleBulwarkDamage(e);
	}
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void resonanceDefense(EntityDamageEvent e) {
		abilities.handleResonanceDefense(e);
	}
	@EventHandler(ignoreCancelled = true)
	public void passiveDamage(EntityDamageEvent e) {
		if (e.getEntity() instanceof Player ritualPlayer
				&& plugin.rituals().active().filter(r -> r.playerId().equals(ritualPlayer.getUniqueId())).isPresent()
				&& plugin.configService().file("ritual.yml").getBoolean("failure.cancel-on-damage", true)) {
			plugin.rituals().cancel(plugin.configService().file("ritual.yml").getBoolean("refund-on-cancel", true));
			return;
		}
		if (!(e.getEntity() instanceof Player p) || !dragonborn.isDragonborn(p.getUniqueId()))
			return;
		if (dragonborn.hasSoul(p, SoulIdentity.AKUMA) && e.getCause() == EntityDamageEvent.DamageCause.FREEZE) {
			e.setCancelled(true);
			p.setFreezeTicks(0);
		}
		if (dragonborn.hasSoul(p, SoulIdentity.REV) && EnumSet
				.of(EntityDamageEvent.DamageCause.FIRE, EntityDamageEvent.DamageCause.FIRE_TICK,
						EntityDamageEvent.DamageCause.LAVA, EntityDamageEvent.DamageCause.HOT_FLOOR)
				.contains(e.getCause()))
			e.setCancelled(true);
		if (dragonborn.hasSoul(p, SoulIdentity.LAMARI) && e.getCause() == EntityDamageEvent.DamageCause.FALL)
			e.setCancelled(true);
	}
	@EventHandler(ignoreCancelled = true)
	public void endermanTarget(EntityTargetLivingEntityEvent e) {
		if (e.getEntityType() == EntityType.ENDERMAN && e.getTarget() instanceof Player p
				&& dragonborn.isDragonborn(p.getUniqueId())
				&& plugin.configService().file("abilities.yml").getBoolean("passives.neutral-endermen", true))
			e.setCancelled(true);
	}
	@EventHandler
	public void quit(PlayerQuitEvent e) {
		Player victim = e.getPlayer();
		abilities.handleLogout(victim);
		if (!combatTags.tagged(victim.getUniqueId()))
			return;
		Optional<DragonSoul> soul = souls.byHolder(victim.getUniqueId());
		if (soul.isEmpty())
			return;
		combatTags.opponent(victim.getUniqueId()).map(Bukkit::getPlayer).filter(Objects::nonNull)
				.filter(p -> !dragonborn.isDragonborn(p.getUniqueId()) && eligibility.check(p).eligible())
				.ifPresentOrElse(killer -> {
					souls.assign(soul.get().id(), killer.getUniqueId(), "COMBAT_LOG");
					dragonborn.remove(victim);
					dragonborn.apply(killer);
					plugin.animations().play("pvp-transfer", killer.getLocation(), killer);
					Bukkit.broadcast(plugin.messages().component("combat-log-transfer", "player", victim.getName(),
							"soul", SoulIdentity.displayName(soul.get().id()), "recipient", killer.getName()));
				}, () -> souls.pending(soul.get().id(), "COMBAT_LOG_NO_RECIPIENT"));
		combatTags.clear(victim.getUniqueId());
	}
	@EventHandler
	public void held(PlayerItemHeldEvent e) {
		Player p = e.getPlayer();
		if (!dragonborn.isDragonborn(p.getUniqueId()))
			return;
		ItemStack old = p.getInventory().getItem(e.getPreviousSlot());
		if (!dragonborn.isUsableFocus(p, old))
			return;
		if (plugin.players().settings(p.getUniqueId()).selector() == SelectorMode.SNEAK_SCROLL && !p.isSneaking())
			return;
		abilities.cycle(p, e.getNewSlot() > e.getPreviousSlot() ? 1 : -1);
		e.setCancelled(true);
	}
	@EventHandler
	public void interact(PlayerInteractEvent e) {
		if (!e.getAction().isRightClick() || e.getHand() != EquipmentSlot.HAND)
			return;
		if (!dragonborn.isFocus(e.getItem())) {
			Location interaction = plugin.configuredLocation("altar.yml", "interaction");
			if (e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null
					&& interaction != null && interaction.getWorld().equals(e.getClickedBlock().getWorld())
					&& interaction.getBlock().equals(e.getClickedBlock())) {
				e.setCancelled(true);
				var state = plugin.dragonEvent().altarState();
				if (plugin.dragonEvent().state().ordinal() < com.dragonaltar.dragonevent.DragonEventState.DEFEATED
						.ordinal()) {
					e.getPlayer().sendMessage("The altar has not yet awakened.");
					return;
				}
				if (state == com.dragonaltar.altar.AltarState.DORMANT || plugin.souls().unclaimedCount() == 0) {
					e.getPlayer()
							.sendMessage("The altar is dormant. Its three souls have already chosen their vessels.");
					return;
				}
				if (state != com.dragonaltar.altar.AltarState.ACTIVE) {
					e.getPlayer().sendMessage("The altar is silent.");
					return;
				}
				if (plugin.dragonborn().isDragonborn(e.getPlayer().getUniqueId())) {
					e.getPlayer().sendMessage("A Dragon Soul already resides within you.");
					return;
				}
				if (plugin.rituals().active().isPresent()) {
					e.getPlayer().sendMessage("The altar is currently bound to another ritual.");
					return;
				}
				plugin.ritualMenu().open(e.getPlayer());
			}
			return;
		}
		if (!dragonborn.isUsableFocus(e.getPlayer(), e.getItem())) {
			e.setCancelled(true);
			plugin.messages().send(e.getPlayer(), "focus-protected");
			dragonborn.ensureFocus(e.getPlayer());
			return;
		}
		e.setCancelled(true);
		AbilityResult result = abilities.cast(e.getPlayer());
		if (!result.success()) {
			if (result.message().startsWith("ability-"))
				plugin.messages().send(e.getPlayer(), result.message());
			else
				e.getPlayer().sendMessage(result.message());
		}
	}
	@EventHandler
	public void drop(PlayerDropItemEvent e) {
		if (dragonborn.isFocus(e.getItemDrop().getItemStack()))
			e.setCancelled(true);
	}
	@EventHandler(priority = EventPriority.LOWEST)
	public void pickup(EntityPickupItemEvent e) {
		if (!dragonborn.isFocus(e.getItem().getItemStack()))
			return;
		Optional<UUID> owner = dragonborn.focusOwner(e.getItem().getItemStack());
		e.setCancelled(true);
		e.getItem().remove();
		Player restore = owner.map(Bukkit::getPlayer).orElse(e.getEntity() instanceof Player player ? player : null);
		if (restore != null)
			Bukkit.getScheduler().runTask(plugin, () -> dragonborn.ensureFocus(restore));
	}
	@EventHandler(priority = EventPriority.LOWEST)
	public void focusSpawn(ItemSpawnEvent e) {
		if (!dragonborn.isFocus(e.getEntity().getItemStack()))
			return;
		Player owner = dragonborn.focusOwner(e.getEntity().getItemStack()).map(Bukkit::getPlayer).orElse(null);
		e.setCancelled(true);
		if (owner != null)
			Bukkit.getScheduler().runTask(plugin, () -> dragonborn.ensureFocus(owner));
	}
	@EventHandler(ignoreCancelled = true)
	public void focusEntity(PlayerInteractEntityEvent e) {
		if (dragonborn.isFocus(e.getPlayer().getInventory().getItemInMainHand())
				&& (e.getRightClicked() instanceof ItemFrame || e.getRightClicked() instanceof ArmorStand))
			e.setCancelled(true);
	}
	@EventHandler(priority = EventPriority.LOWEST)
	public void inventory(InventoryClickEvent e) {
		if (!(e.getWhoClicked() instanceof Player p))
			return;
		boolean focus = dragonborn.isFocus(e.getCurrentItem()) || dragonborn.isFocus(e.getCursor())
				|| (e.getHotbarButton() >= 0 && dragonborn.isFocus(p.getInventory().getItem(e.getHotbarButton())));
		boolean duplicates = e.getAction() == org.bukkit.event.inventory.InventoryAction.CLONE_STACK
				|| e.getAction() == org.bukkit.event.inventory.InventoryAction.COLLECT_TO_CURSOR;
		if (focus && (duplicates || e.isShiftClick() || e.getClickedInventory() != e.getWhoClicked().getInventory())) {
			e.setCancelled(true);
			plugin.messages().send(p, "focus-protected");
		}
	}
	@EventHandler(priority = EventPriority.LOWEST)
	public void inventoryDrag(InventoryDragEvent e) {
		if (dragonborn.isFocus(e.getOldCursor())
				&& e.getRawSlots().stream().anyMatch(slot -> slot < e.getView().getTopInventory().getSize()))
			e.setCancelled(true);
	}
	@EventHandler(priority = EventPriority.LOWEST)
	public void inventoryMove(InventoryMoveItemEvent e) {
		if (dragonborn.isFocus(e.getItem()))
			e.setCancelled(true);
	}
	@EventHandler
	public void inventoryClose(InventoryCloseEvent e) {
		if (e.getPlayer() instanceof Player player && dragonborn.isDragonborn(player.getUniqueId()))
			Bukkit.getScheduler().runTask(plugin, () -> dragonborn.ensureFocus(player));
	}
	@EventHandler
	public void swap(PlayerSwapHandItemsEvent e) {
		if (!dragonborn.isFocus(e.getMainHandItem()) && !dragonborn.isFocus(e.getOffHandItem()))
			return;
		e.setCancelled(true);
		if (!dragonborn.isUsableFocus(e.getPlayer(), e.getMainHandItem())
				&& !dragonborn.isUsableFocus(e.getPlayer(), e.getOffHandItem())) {
			plugin.messages().send(e.getPlayer(), "focus-protected");
			dragonborn.ensureFocus(e.getPlayer());
			return;
		}
		if (e.getPlayer().isSneaking())
			plugin.abilityMenu().open(e.getPlayer());
		else
			abilities.cycleCategory(e.getPlayer());
	}
}
