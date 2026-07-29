package com.dragonaltar.ritual;

import com.dragonaltar.DragonAltarPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.*;

public final class OfferingService {
    public record Requirement(String id,Material material,int amount,String displayName){}
    public record Selection(int slot,ItemStack original,int amount,boolean equipped){}
    public record EntitySelection(UUID entityId,ItemStack original,int amount){}
    public record Plan(List<Requirement> requirements,List<Selection> selections,List<EntitySelection> entitySelections,List<ItemStack> exactItems,Map<String,Integer> available,boolean complete){
        public Plan{requirements=List.copyOf(requirements);selections=List.copyOf(selections);entitySelections=List.copyOf(entitySelections);exactItems=exactItems.stream().map(ItemStack::clone).toList();available=Map.copyOf(available);}
    }
    private final DragonAltarPlugin plugin;
    public OfferingService(DragonAltarPlugin plugin){this.plugin=plugin;}
    public List<Requirement> requirements(){
        List<Requirement> result=new ArrayList<>();int index=0;
        for(Map<?,?> raw:plugin.configService().file("ritual.yml").getMapList("offerings")){
            Material material=Material.matchMaterial(String.valueOf(raw.get("material")));if(material==null)continue;
            int amount=Math.max(1,Integer.parseInt(String.valueOf(raw.get("amount"))));
            String id=String.valueOf(raw.containsKey("id")?raw.get("id"):material.name().toLowerCase(Locale.ROOT).replace('_','-'));
            String name=String.valueOf(raw.containsKey("display-name")?raw.get("display-name"):material.name());result.add(new Requirement(id,material,amount,name));index++;
        }return result;
    }
    public Plan plan(Player player){
        List<Requirement> requirements=requirements();List<Selection> selected=new ArrayList<>();List<EntitySelection> entitySelected=new ArrayList<>();Map<String,Integer> available=new LinkedHashMap<>();boolean complete=true;
        OfferingMode mode;try{mode=OfferingMode.valueOf(plugin.configService().file("ritual.yml").getString("offering-mode","INVENTORY_CONSUME"));}catch(IllegalArgumentException ex){mode=OfferingMode.INVENTORY_CONSUME;}
        List<Item> pedestalItems=pedestalItems();
        for(Requirement requirement:requirements){
            List<Integer> candidates=mode==OfferingMode.PEDESTAL_DEPOSIT?List.of():candidateSlots(player,requirement.material());
            List<Item> entities=mode==OfferingMode.INVENTORY_CONSUME?List.of():pedestalItems.stream().filter(entity->matches(entity.getItemStack(),requirement.material())).toList();
            int total=candidates.stream().mapToInt(slot->item(player,slot).getAmount()).sum()+entities.stream().mapToInt(entity->entity.getItemStack().getAmount()).sum();available.put(requirement.id(),total);
            if(total<requirement.amount()){complete=false;continue;}
            int remaining=requirement.amount();
            if(requirement.material()==Material.ELYTRA){
                Source source=selectElytra(player,candidates,entities);if(source.slot()!=null){ItemStack stack=item(player,source.slot());selected.add(new Selection(source.slot(),stack.clone(),1,source.slot()==38));}else{Item entity=source.entity();entitySelected.add(new EntitySelection(entity.getUniqueId(),entity.getItemStack().clone(),1));}
            }else{for(int slot:candidates){ItemStack stack=item(player,slot);int take=Math.min(remaining,stack.getAmount());selected.add(new Selection(slot,stack.clone(),take,slot==38));remaining-=take;if(remaining==0)break;}
                for(Item entity:entities){if(remaining==0)break;ItemStack stack=entity.getItemStack();int take=Math.min(remaining,stack.getAmount());entitySelected.add(new EntitySelection(entity.getUniqueId(),stack.clone(),take));remaining-=take;}}
        }
        List<ItemStack> exact=new ArrayList<>();for(Selection selection:selected){ItemStack copy=selection.original().clone();copy.setAmount(selection.amount());exact.add(copy);}for(EntitySelection selection:entitySelected){ItemStack copy=selection.original().clone();copy.setAmount(selection.amount());exact.add(copy);}
        return new Plan(requirements,selected,entitySelected,exact,available,complete);
    }
    public List<ItemStack> consume(Player player,Plan plan){
        if(!plan.complete())throw new IllegalStateException("Required offerings are missing");
        for(Selection selection:plan.selections()){ItemStack current=item(player,selection.slot());if(current==null||!current.isSimilar(selection.original())||current.getAmount()<selection.amount())throw new IllegalStateException("Inventory changed; no offerings were consumed");}
        Map<UUID,Item> entities=new HashMap<>();for(EntitySelection selection:plan.entitySelections()){var raw=plugin.getServer().getEntity(selection.entityId());if(!(raw instanceof Item entity)||!entity.isValid()||!entity.getItemStack().isSimilar(selection.original())||entity.getItemStack().getAmount()<selection.amount())throw new IllegalStateException("Pedestal offerings changed; no offerings were consumed");entities.put(selection.entityId(),entity);}
        for(Selection selection:plan.selections()){ItemStack current=item(player,selection.slot());int left=current.getAmount()-selection.amount();setItem(player,selection.slot(),left==0?null:withAmount(current,left));}
        for(EntitySelection selection:plan.entitySelections()){Item entity=entities.get(selection.entityId());ItemStack current=entity.getItemStack();int left=current.getAmount()-selection.amount();if(left==0)entity.remove();else entity.setItemStack(withAmount(current,left));}
        return plan.exactItems().stream().map(ItemStack::clone).toList();
    }
    public String inspectElytra(Player player){
        List<Integer> slots=candidateSlots(player,Material.ELYTRA);if(slots.isEmpty())return "No acceptable Elytra found.";
        int selected=Objects.requireNonNull(selectElytra(player,slots,List.of()).slot());StringBuilder out=new StringBuilder();
        for(int slot:slots){ItemStack item=item(player,slot);Damageable damage=(Damageable)item.getItemMeta();int max=item.getType().getMaxDurability(),used=damage.getDamage();
            out.append("slot=").append(slot).append(" equipped=").append(slot==38).append(" damage=").append(used).append(" max=").append(max).append(" remaining=").append(max-used).append(" enchantments=").append(item.getEnchantments()).append(" custom-name=").append(item.getItemMeta().hasDisplayName()).append(" passes=true selected=").append(slot==selected).append('\n');}
        return out.toString().stripTrailing();
    }
    private List<Integer> candidateSlots(Player player,Material material){
        List<Integer> slots=new ArrayList<>();for(int slot=0;slot<player.getInventory().getStorageContents().length;slot++){ItemStack item=player.getInventory().getItem(slot);if(matches(item,material))slots.add(slot);}
        if(material==Material.ELYTRA&&plugin.configService().file("ritual.yml").getBoolean("elytra.include-equipped-chest-slot",false)&&matches(player.getInventory().getChestplate(),material))slots.add(38);
        return slots;
    }
    private boolean matches(ItemStack item,Material material){
        if(item==null||item.getType().isAir()||item.getType()!=material)return false;if(material!=Material.ELYTRA)return true;
        YamlConfiguration cfg=plugin.configService().file("ritual.yml");var meta=item.getItemMeta();
        if(!cfg.getBoolean("elytra.accept-enchanted",true)&&!item.getEnchantments().isEmpty())return false;
        if(!cfg.getBoolean("elytra.accept-renamed",true)&&meta.hasDisplayName())return false;
        if(!cfg.getBoolean("elytra.accept-custom-lore",true)&&meta.hasLore())return false;
        for(String raw:cfg.getStringList("elytra.blocked-pdc-keys")){NamespacedKey key=NamespacedKey.fromString(raw);if(key!=null&&meta.getPersistentDataContainer().has(key))return false;}
        return true;
    }
    private Source selectElytra(Player player,List<Integer> slots,List<Item> entities){
        ElytraSelection.Policy priority;try{priority=ElytraSelection.Policy.valueOf(plugin.configService().file("ritual.yml").getString("elytra.consumption-priority","MOST_DAMAGED").toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ex){priority=ElytraSelection.Policy.MOST_DAMAGED;}
        List<Source> sources=new ArrayList<>();for(int slot:slots)sources.add(new Source(slot,null,item(player,slot)));for(Item entity:entities)sources.add(new Source(null,entity,entity.getItemStack()));
        Map<Integer,Source> indexed=new HashMap<>();List<ElytraSelection.Candidate> candidates=new ArrayList<>();for(int i=0;i<sources.size();i++){Source source=sources.get(i);int order=source.slot()==null?1000+i:source.slot();indexed.put(order,source);candidates.add(new ElytraSelection.Candidate(order,damage(source.stack()),source.stack().getEnchantments().values().stream().mapToInt(Integer::intValue).sum()));}
        return indexed.get(ElytraSelection.select(candidates,priority).slot());
    }
    private List<Item> pedestalItems(){Map<UUID,Item> unique=new LinkedHashMap<>();for(org.bukkit.Location pedestal:plugin.pedestalLocations())for(Item item:pedestal.getWorld().getNearbyEntitiesByType(Item.class,pedestal,1.5))unique.put(item.getUniqueId(),item);return new ArrayList<>(unique.values());}
    private static int damage(ItemStack item){return item.getItemMeta() instanceof Damageable damage?damage.getDamage():0;}
    private static ItemStack item(Player player,int slot){return slot==38?player.getInventory().getChestplate():player.getInventory().getItem(slot);}
    private static void setItem(Player player,int slot,ItemStack item){if(slot==38)player.getInventory().setChestplate(item);else player.getInventory().setItem(slot,item);}
    private static ItemStack withAmount(ItemStack item,int amount){ItemStack copy=item.clone();copy.setAmount(amount);return copy;}
    private record Source(Integer slot,Item entity,ItemStack stack){}
}
