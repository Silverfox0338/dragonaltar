package com.dragonaltar.gui;

import com.dragonaltar.DragonAltarPlugin;
import com.dragonaltar.player.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class SettingsMenu implements Listener {
    private final DragonAltarPlugin plugin;
    private final NamespacedKey key;
    private final Component title=Component.text("Dragon Settings");
    public SettingsMenu(DragonAltarPlugin plugin){this.plugin=plugin;key=new NamespacedKey(plugin,"settings_button");}
    public void open(Player player){
        PlayerSettings s=plugin.players().settings(player.getUniqueId());Inventory inv=Bukkit.createInventory(null,27,title);
        inv.setItem(10,toggle(Material.COMPASS,"HUD",s.hud(),"hud"));
        inv.setItem(11,toggle(Material.FEATHER,"Slow Falling",s.slowFalling(),"slowfall"));
        inv.setItem(12,toggle(Material.BLAZE_POWDER,"Passive Particles",s.passiveParticles(),"passive-particles"));
        inv.setItem(13,toggle(Material.FIREWORK_STAR,"Animation Particles",s.animationParticles(),"animation-particles"));
        inv.setItem(14,toggle(Material.NOTE_BLOCK,"Sounds",s.sounds(),"sounds"));
        inv.setItem(15,toggle(Material.NAME_TAG,"Titles",s.titles(),"titles"));
        inv.setItem(16,toggle(Material.ENDER_EYE,"Screen Effects",s.screenEffects(),"screen-effects"));
        inv.setItem(21,button(Material.REPEATER,"Selector: "+s.selector().name().replace('_',' '),"selector",List.of(Component.text("Click to switch mode"))));
        inv.setItem(22,button(Material.GLOWSTONE_DUST,"Effect Level: "+s.effects().name(),"effects",List.of(Component.text("Click to cycle"))));
        inv.setItem(23,button(Material.ECHO_SHARD,"Recover Dragon Focus","focus",List.of(Component.text("Restores it only if missing"))));
        player.openInventory(inv);
    }
    private ItemStack toggle(Material onMaterial,String name,boolean enabled,String action){return button(enabled?onMaterial:Material.GRAY_DYE,name+": "+(enabled?"ON":"OFF"),action,List.of(Component.text("Click to turn "+(enabled?"off":"on"))));}
    private ItemStack button(Material material,String name,String action,List<Component> lore){ItemStack item=new ItemStack(material);ItemMeta meta=item.getItemMeta();meta.displayName(Component.text(name));meta.lore(lore);meta.getPersistentDataContainer().set(key,PersistentDataType.STRING,action);item.setItemMeta(meta);return item;}
    @EventHandler public void click(InventoryClickEvent event){
        if(!event.getView().title().equals(title))return;event.setCancelled(true);if(!(event.getWhoClicked() instanceof Player player)||event.getCurrentItem()==null)return;
        String action=event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(key,PersistentDataType.STRING);if(action==null)return;
        PlayerSettings old=plugin.players().settings(player.getUniqueId());PlayerSettings next=switch(action){
            case "hud"->old.withHud(!old.hud());case "slowfall"->old.withSlowFalling(!old.slowFalling());
            case "passive-particles"->old.withPassiveParticles(!old.passiveParticles());case "animation-particles"->old.withAnimationParticles(!old.animationParticles());
            case "sounds"->old.withSounds(!old.sounds());case "titles"->old.withTitles(!old.titles());case "screen-effects"->old.withScreenEffects(!old.screenEffects());
            case "selector"->old.withSelector(old.selector()==SelectorMode.LOCKED?SelectorMode.SNEAK_SCROLL:SelectorMode.LOCKED);
            case "effects"->old.withEffects(EffectMode.values()[(old.effects().ordinal()+1)%EffectMode.values().length]);
            case "focus"->{plugin.dragonborn().ensureFocus(player);yield old;}default->old;};
        if(next!=old)plugin.players().settings(player.getUniqueId(),next);plugin.dragonborn().apply(player);open(player);
    }
    @EventHandler public void drag(InventoryDragEvent event){if(event.getView().title().equals(title))event.setCancelled(true);}
}
