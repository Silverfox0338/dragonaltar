package com.dragonaltar.gui;

import com.dragonaltar.DragonAltarPlugin;
import com.dragonaltar.ritual.OfferingService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public final class RitualMenu implements Listener {
    private final DragonAltarPlugin plugin;private final NamespacedKey buttonKey;
    public RitualMenu(DragonAltarPlugin plugin){this.plugin=plugin;buttonKey=new NamespacedKey(plugin,"ritual_gui_button");}
    public void open(Player player){
        OfferingService.Plan plan=plugin.rituals().plan(player);MenuHolder holder=new MenuHolder();Inventory inventory=plugin.getServer().createInventory(holder,27,Component.text(plan.complete()?"Confirm Dragonborn Ritual":"Dragonborn Ritual Recipe"));holder.inventory=inventory;
        int slot=1;for(OfferingService.Requirement requirement:plan.requirements()){int available=plan.available().getOrDefault(requirement.id(),0);boolean ready=available>=requirement.amount();ItemStack icon=new ItemStack(requirement.material());ItemMeta meta=icon.getItemMeta();NamedTextColor color=ready?NamedTextColor.GREEN:NamedTextColor.RED;meta.displayName(Component.text(pretty(requirement.id()),color));meta.lore(List.of(Component.text("Required: "+requirement.amount()),Component.text("Available: "+available),Component.text("Status: "+(ready?"Ready":"Missing"),color)));icon.setItemMeta(meta);inventory.setItem(slot,icon);slot+=2;}
        ItemStack action=button(plan.complete()?Material.LIME_CONCRETE:Material.RED_CONCRETE,plan.complete()?"confirm":"missing",plan.complete()?"Begin ritual - permanently consume listed offerings":"Missing offerings");
        if(plan.complete()){ItemMeta meta=action.getItemMeta();List<Component> lore=new ArrayList<>();lore.add(Component.text("These offerings will be permanently consumed:"));for(var requirement:plan.requirements())lore.add(Component.text("• "+requirement.amount()+" "+pretty(requirement.id())));meta.lore(lore);action.setItemMeta(meta);}
        inventory.setItem(22,action);inventory.setItem(26,button(Material.BARRIER,"close","Cancel / Close"));player.openInventory(inventory);
    }
    private ItemStack button(Material material,String id,String name){ItemStack item=new ItemStack(material);ItemMeta meta=item.getItemMeta();meta.displayName(Component.text(name));meta.getPersistentDataContainer().set(buttonKey,PersistentDataType.STRING,id);item.setItemMeta(meta);return item;}
    @EventHandler public void click(InventoryClickEvent event){
        if(!(event.getView().getTopInventory().getHolder(false) instanceof MenuHolder))return;event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player player)||event.getClickedInventory()!=event.getView().getTopInventory()||event.getCurrentItem()==null)return;
        String action=event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(buttonKey,PersistentDataType.STRING);if(action==null)return;
        if(action.equals("close"))player.closeInventory();else if(action.equals("confirm")){player.closeInventory();try{plugin.rituals().start(player);}catch(IllegalStateException ex){plugin.messages().send(player,"ritual-error","reason",ex.getMessage()==null?"Unknown error":ex.getMessage());}}
    }
    @EventHandler public void drag(InventoryDragEvent event){if(event.getView().getTopInventory().getHolder(false) instanceof MenuHolder)event.setCancelled(true);}
    private static String pretty(String id){String[] words=id.split("-");StringJoiner joiner=new StringJoiner(" ");for(String word:words)joiner.add(word.isEmpty()?word:Character.toUpperCase(word.charAt(0))+word.substring(1));return joiner.toString();}
    private static final class MenuHolder implements InventoryHolder {private Inventory inventory;@Override public Inventory getInventory(){return inventory;}}
}
