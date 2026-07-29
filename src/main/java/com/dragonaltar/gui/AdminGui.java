package com.dragonaltar.gui;

import com.dragonaltar.DragonAltarPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public final class AdminGui implements Listener {
    private final DragonAltarPlugin plugin;private final NamespacedKey buttonKey;private final Component title=Component.text("DragonAltar Admin");
    private static final Map<String,Material> BUTTONS=Map.ofEntries(
            Map.entry("event",Material.END_CRYSTAL),Map.entry("altar",Material.DRAGON_EGG),Map.entry("ritual",Material.NETHER_STAR),
            Map.entry("souls",Material.ECHO_SHARD),Map.entry("dragonborn",Material.DRAGON_HEAD),Map.entry("abilities",Material.FEATHER),
            Map.entry("energy",Material.REDSTONE),Map.entry("protection",Material.SHIELD),Map.entry("integrations",Material.COMPARATOR),
            Map.entry("health",Material.HEART_OF_THE_SEA),Map.entry("beta",Material.COMMAND_BLOCK));
    public AdminGui(DragonAltarPlugin plugin){this.plugin=plugin;buttonKey=new NamespacedKey(plugin,"admin_button");}
    public void open(Player player){Inventory inventory=Bukkit.createInventory(null,36,title);int slot=9;for(var entry:BUTTONS.entrySet()){ItemStack item=new ItemStack(entry.getValue());ItemMeta meta=item.getItemMeta();meta.displayName(Component.text(capitalize(entry.getKey())));meta.getPersistentDataContainer().set(buttonKey,PersistentDataType.STRING,entry.getKey());item.setItemMeta(meta);inventory.setItem(slot++,item);}player.openInventory(inventory);}
    @EventHandler public void click(InventoryClickEvent e){if(!e.getView().title().equals(title))return;e.setCancelled(true);if(!(e.getWhoClicked() instanceof Player p)||e.getCurrentItem()==null)return;String action=e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(buttonKey,PersistentDataType.STRING);if(action==null)return;
        p.closeInventory();switch(action){
            case "event"->dispatch(p,"dragon event preview");
            case "altar"->dispatch(p,"dragon altar status");
            case "ritual"->dispatch(p,"dragon ritual status");
            case "souls","dragonborn"->dispatch(p,"dragon admin list");
            case "abilities"->plugin.abilityMenu().open(p);
            case "energy"->dispatch(p,"dragon admin energy view "+p.getName());
            case "protection"->dispatch(p,"dragon protection status");
            case "integrations"->dispatch(p,"dragon system integrations");
            case "health"->dispatch(p,"dragon system health");
            case "beta"->{if(plugin.configService().serverMode().name().equals("PRODUCTION"))p.sendMessage("Beta tools are disabled in production.");else dispatch(p,"dragon dev perf status");}
            default->throw new IllegalStateException("Unknown GUI action");
        }}
    @EventHandler public void drag(InventoryDragEvent e){if(e.getView().title().equals(title))e.setCancelled(true);}
    private static String capitalize(String value){return Character.toUpperCase(value.charAt(0))+value.substring(1);}
    private static void dispatch(Player player,String command){Bukkit.dispatchCommand(player,command);}
}
