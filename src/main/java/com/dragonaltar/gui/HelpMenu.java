package com.dragonaltar.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class HelpMenu implements Listener {
	private final Component title = Component.text("DragonAltar Help");
	public void open(Player player) {
		Inventory inv = Bukkit.createInventory(null, 27, title);
		inv.setItem(10, item(Material.ECHO_SHARD, "Dragon Focus", List.of(
				"Hold it and use your controls to select and cast abilities.", "Run /dragon focus if it is missing.")));
		inv.setItem(12, item(Material.REDSTONE, "Dragon Energy",
				List.of("Energy regenerates automatically.", "Your HUD shows energy, ability, and cooldown.")));
		inv.setItem(14,
				item(Material.NETHER_STAR, "Abilities", List.of("Open the ability menu to read and select abilities.",
						"Each ability uses energy and has a cooldown.")));
		inv.setItem(16,
				item(Material.COMPARATOR, "Settings", List.of("Run /dragon settings for accessibility controls.",
						"HUD, particles, sounds, titles, and effects are adjustable.")));
		inv.setItem(22,
				item(Material.DRAGON_EGG, "Ritual", List.of("Right-click the configured altar interaction block.",
						"Bring every item shown on the floating recipe display.")));
		player.openInventory(inv);
	}
	private static ItemStack item(Material material, String name, List<String> lines) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text(name));
		meta.lore(lines.stream().map(Component::text).toList());
		item.setItemMeta(meta);
		return item;
	}
	@EventHandler
	public void click(InventoryClickEvent event) {
		if (event.getView().title().equals(title))
			event.setCancelled(true);
	}
	@EventHandler
	public void drag(InventoryDragEvent event) {
		if (event.getView().title().equals(title))
			event.setCancelled(true);
	}
}
