package com.dragonaltar.dragonborn;

import com.dragonaltar.soul.DragonSoulService;
import com.dragonaltar.soul.SoulIdentity;
import com.dragonaltar.config.ConfigService;
import com.dragonaltar.player.PlayerDataService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;

import java.util.Optional;
import java.util.UUID;

public final class DragonbornService {
	private final JavaPlugin plugin;
	private final DragonSoulService souls;
	private final ConfigService config;
	private final PlayerDataService players;
	private final NamespacedKey focusKey;
	private final NamespacedKey focusOwnerKey;
	private final NamespacedKey focusSoulKey;
	private final NamespacedKey healthKey;
	private final NamespacedKey slowFallingKey;
	private final NamespacedKey frostveilSpeedKey;
	private final NamespacedKey stoneheartToughnessKey;
	private final MiniMessage mini = MiniMessage.miniMessage();

	public DragonbornService(JavaPlugin plugin, DragonSoulService souls, ConfigService config,
			PlayerDataService players) {
		this.plugin = plugin;
		this.souls = souls;
		this.config = config;
		this.players = players;
		focusKey = new NamespacedKey(plugin, "dragon_focus");
		focusOwnerKey = new NamespacedKey(plugin, "dragon_focus_owner");
		focusSoulKey = new NamespacedKey(plugin, "dragon_focus_soul");
		healthKey = new NamespacedKey(plugin, "dragonborn_health");
		slowFallingKey = new NamespacedKey(plugin, "dragonborn_slow_falling");
		frostveilSpeedKey = new NamespacedKey(plugin, "frostveil_speed");
		stoneheartToughnessKey = new NamespacedKey(plugin, "stoneheart_toughness");
	}
	public boolean isDragonborn(UUID id) {
		return souls.byHolder(id).isPresent();
	}
	public void apply(Player player) {
		if (!isDragonborn(player.getUniqueId())) {
			remove(player);
			return;
		}
		AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
		if (max != null && max.getModifier(healthKey) == null) {
			double hearts = config.file("abilities.yml").getDouble("passives.additional-hearts", 2);
			max.addTransientModifier(
					new AttributeModifier(healthKey, hearts * 2, AttributeModifier.Operation.ADD_NUMBER));
		}
		if (config.file("abilities.yml").getBoolean("passives.slow-falling", true)
				&& players.settings(player.getUniqueId()).slowFalling()) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, PotionEffect.INFINITE_DURATION, 0,
					true, false, true));
			player.getPersistentDataContainer().set(slowFallingKey, PersistentDataType.BYTE, (byte) 1);
		} else
			removeSlowFalling(player);
		ensureFocus(player);
		refreshEnvironmentalPassives(player);
	}
	public void remove(Player player) {
		AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
		if (max != null)
			max.removeModifier(healthKey);
		removeSlowFalling(player);
		removeModifier(player, Attribute.MOVEMENT_SPEED, frostveilSpeedKey);
		removeModifier(player, Attribute.ARMOR_TOUGHNESS, stoneheartToughnessKey);
		removeFocusItems(player);
	}
	public boolean ensureFocus(Player player) {
		if (!isDragonborn(player.getUniqueId())) {
			removeFocusItems(player);
			return false;
		}
		for (ItemStack item : player.getEnderChest().getContents())
			if (isFocus(item))
				item.setAmount(0);
		ItemStack canonical = null;
		for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
			ItemStack item = player.getInventory().getItem(slot);
			if (!isFocus(item))
				continue;
			if (canonical == null && isOwnedBy(player, item)) {
				canonical = item;
				if (canonical.getAmount() != 1)
					canonical.setAmount(1);
				updateFocusIdentity(player, canonical);
			} else
				player.getInventory().setItem(slot, null);
		}
		ItemStack cursor = player.getItemOnCursor();
		if (isFocus(cursor)) {
			if (canonical == null && isOwnedBy(player, cursor)) {
				canonical = cursor;
				if (canonical.getAmount() != 1)
					canonical.setAmount(1);
				updateFocusIdentity(player, canonical);
			} else
				player.setItemOnCursor(null);
		}
		if (canonical != null)
			return true;
		Material material = Material
				.matchMaterial(config.file("abilities.yml").getString("focus.material", "ECHO_SHARD"));
		ItemStack focus = new ItemStack(material == null ? Material.ECHO_SHARD : material);
		ItemMeta meta = focus.getItemMeta();
		meta.displayName(
				mini.deserialize(config.file("abilities.yml").getString("focus.name", "<light_purple>Dragon Focus")));
		meta.getPersistentDataContainer().set(focusKey, PersistentDataType.BYTE, (byte) 1);
		meta.setEnchantmentGlintOverride(true);
		focus.setItemMeta(meta);
		updateFocusIdentity(player, focus);
		if (!player.getInventory().addItem(focus).isEmpty()) {
			plugin.getLogger()
					.warning("Could not restore Dragon Focus for " + player.getName() + ": inventory is full.");
			return false;
		}
		return true;
	}
	public int removeEscapedFocusEntities() {
		int removed = 0;
		for (World world : Bukkit.getWorlds())
			for (Item item : world.getEntitiesByClass(Item.class))
				if (isFocus(item.getItemStack())) {
					item.remove();
					removed++;
				}
		return removed;
	}
	private void removeFocusItems(Player player) {
		for (ItemStack item : player.getInventory().getContents())
			if (isFocus(item))
				item.setAmount(0);
		for (ItemStack item : player.getEnderChest().getContents())
			if (isFocus(item))
				item.setAmount(0);
		if (isFocus(player.getItemOnCursor()))
			player.setItemOnCursor(null);
	}
	private void updateFocusIdentity(Player player, ItemStack focus) {
		ItemMeta meta = focus.getItemMeta();
		meta.getPersistentDataContainer().set(focusOwnerKey, PersistentDataType.STRING,
				player.getUniqueId().toString());
		soul(player).ifPresent(identity -> {
			meta.getPersistentDataContainer().set(focusSoulKey, PersistentDataType.STRING, identity.id());
			meta.lore(java.util.List.of(
					net.kyori.adventure.text.Component.text("Soul: " + identity.displayName(),
							net.kyori.adventure.text.format.NamedTextColor.GRAY),
					net.kyori.adventure.text.Component.text("Soulbound to " + player.getName(),
							net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)));
		});
		focus.setItemMeta(meta);
	}
	private void removeSlowFalling(Player player) {
		PotionEffect falling = player.getPotionEffect(PotionEffectType.SLOW_FALLING);
		if (player.getPersistentDataContainer().has(slowFallingKey, PersistentDataType.BYTE) && falling != null
				&& falling.getDuration() == PotionEffect.INFINITE_DURATION && falling.getAmplifier() == 0
				&& falling.isAmbient())
			player.removePotionEffect(PotionEffectType.SLOW_FALLING);
		player.getPersistentDataContainer().remove(slowFallingKey);
	}
	public boolean isFocus(ItemStack item) {
		return item != null && item.hasItemMeta()
				&& item.getItemMeta().getPersistentDataContainer().has(focusKey, PersistentDataType.BYTE);
	}
	public boolean isUsableFocus(Player player, ItemStack item) {
		return isFocus(item) && isDragonborn(player.getUniqueId()) && isOwnedBy(player, item);
	}
	public Optional<UUID> focusOwner(ItemStack item) {
		if (!isFocus(item))
			return Optional.empty();
		String raw = item.getItemMeta().getPersistentDataContainer().get(focusOwnerKey, PersistentDataType.STRING);
		if (raw == null)
			return Optional.empty();
		try {
			return Optional.of(UUID.fromString(raw));
		} catch (IllegalArgumentException ignored) {
			return Optional.empty();
		}
	}
	private boolean isOwnedBy(Player player, ItemStack item) {
		return focusOwner(item).filter(player.getUniqueId()::equals).isPresent();
	}
	public Optional<SoulIdentity> soul(Player player) {
		return souls.byHolder(player.getUniqueId()).map(value -> SoulIdentity.fromId(value.id()));
	}
	public boolean hasSoul(Player player, SoulIdentity identity) {
		return soul(player).map(value -> value == identity).orElse(false);
	}
	public void refreshEnvironmentalPassives(Player player) {
		Optional<SoulIdentity> identity = soul(player);
		if (identity.isEmpty())
			return;
		if (identity.get() == SoulIdentity.AKUMA && player.getLocation().getBlock().getTemperature() <= config
				.file("abilities.yml").getDouble("named-souls.akuma.cold-temperature-threshold", .15)) {
			addModifier(player, Attribute.MOVEMENT_SPEED, frostveilSpeedKey,
					config.file("abilities.yml").getDouble("named-souls.akuma.cold-speed-bonus", .15),
					AttributeModifier.Operation.ADD_SCALAR);
			player.setFreezeTicks(0);
		} else
			removeModifier(player, Attribute.MOVEMENT_SPEED, frostveilSpeedKey);
		if (identity.get() == SoulIdentity.LAMARI)
			addModifier(player, Attribute.ARMOR_TOUGHNESS, stoneheartToughnessKey,
					config.file("abilities.yml").getDouble("named-souls.lamari.armor-toughness", 4),
					AttributeModifier.Operation.ADD_NUMBER);
		else
			removeModifier(player, Attribute.ARMOR_TOUGHNESS, stoneheartToughnessKey);
	}
	private static void addModifier(Player player, Attribute attribute, NamespacedKey key, double amount,
			AttributeModifier.Operation operation) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance != null && instance.getModifier(key) == null)
			instance.addTransientModifier(new AttributeModifier(key, amount, operation));
	}
	private static void removeModifier(Player player, Attribute attribute, NamespacedKey key) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance != null)
			instance.removeModifier(key);
	}
}
