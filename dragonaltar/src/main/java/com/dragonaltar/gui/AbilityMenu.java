package com.dragonaltar.gui;

import com.dragonaltar.DragonAltarPlugin;
import com.dragonaltar.ability.*;
import com.dragonaltar.player.PlayerSettings;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class AbilityMenu implements Listener {
	private static final String SLOW_FALLING_TOGGLE = "setting:slowfall";
	private final DragonAltarPlugin plugin;
	private final AbilityService abilities;
	private final NamespacedKey key;
	private final Component title = Component.text("Dragon Abilities");
	public AbilityMenu(DragonAltarPlugin plugin, AbilityService abilities) {
		this.plugin = plugin;
		this.abilities = abilities;
		key = new NamespacedKey(plugin, "ability_button");
	}
	public void open(Player p) {
		Inventory inv = Bukkit.createInventory(null, 9, title);
		int slot = 2;
		for (DragonAbility ability : abilities.abilities(p)) {
			ItemStack item = new ItemStack(material(ability.category()));
			ItemMeta meta = item.getItemMeta();
			long remaining = abilities.cooldownSeconds(p, ability.id());
			meta.displayName(ability.displayName());
			meta.lore(java.util.List.of(Component.text("Energy: " + ability.energyCost()),
					Component.text("Cooldown: " + (remaining == 0 ? "Ready" : remaining + "s remaining")),
					Component.text("Base cooldown: " + ability.cooldownMillis() / 1000 + "s")));
			meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, ability.id());
			item.setItemMeta(meta);
			inv.setItem(slot++, item);
		}
		boolean enabled = plugin.players().settings(p.getUniqueId()).slowFalling();
		ItemStack toggle = new ItemStack(enabled ? Material.FEATHER : Material.GRAY_DYE);
		ItemMeta toggleMeta = toggle.getItemMeta();
		toggleMeta.displayName(Component.text("Slow Falling: " + (enabled ? "ON" : "OFF")));
		toggleMeta.lore(java.util.List.of(Component.text("Click to turn " + (enabled ? "off" : "on"))));
		toggleMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, SLOW_FALLING_TOGGLE);
		toggle.setItemMeta(toggleMeta);
		inv.setItem(8, toggle);
		p.openInventory(inv);
	}
	@EventHandler
	public void click(InventoryClickEvent e) {
		if (!e.getView().title().equals(title))
			return;
		e.setCancelled(true);
		if (!(e.getWhoClicked() instanceof Player p) || e.getCurrentItem() == null)
			return;
		String id = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
		if (SLOW_FALLING_TOGGLE.equals(id)) {
			PlayerSettings old = plugin.players().settings(p.getUniqueId());
			plugin.players().settings(p.getUniqueId(), old.withSlowFalling(!old.slowFalling()));
			plugin.dragonborn().apply(p);
			open(p);
			return;
		}
		if (id != null) {
			abilities.select(p, id);
			p.closeInventory();
		}
	}
	@EventHandler
	public void drag(InventoryDragEvent e) {
		if (e.getView().title().equals(title))
			e.setCancelled(true);
	}
	private static Material material(AbilityCategory c) {
		return switch (c) {
			case MOVEMENT -> Material.FEATHER;
			case OFFENSE -> Material.DRAGON_BREATH;
			case SENSES -> Material.ENDER_EYE;
			case DEFENSE -> Material.SHIELD;
		};
	}
}
