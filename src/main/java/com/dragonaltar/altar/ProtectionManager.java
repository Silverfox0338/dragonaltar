package com.dragonaltar.altar;

import com.dragonaltar.DragonAltarPlugin;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;

public final class ProtectionManager implements Listener {
    private final DragonAltarPlugin plugin;
    public ProtectionManager(DragonAltarPlugin plugin) { this.plugin = plugin; }
    public boolean contains(Location location) {
        if(!plugin.protectionEnabled())return false;
        Location a=plugin.configuredLocation("altar.yml","protection.pos1"), b=plugin.configuredLocation("altar.yml","protection.pos2");
        if(a==null||b==null||location.getWorld()==null||!location.getWorld().equals(a.getWorld())||!a.getWorld().equals(b.getWorld()))return false;
        return between(location.getX(),a.getX(),b.getX())&&between(location.getY(),a.getY(),b.getY())&&between(location.getZ(),a.getZ(),b.getZ());
    }
    private boolean allowed(Player p){return p!=null&&plugin.hasProtectionBypass(p);}
    private static boolean between(double v,double a,double b){return v>=Math.min(a,b)&&v<=Math.max(a,b);}
    @EventHandler(ignoreCancelled=true) public void breakBlock(BlockBreakEvent e){if(contains(e.getBlock().getLocation())&&!allowed(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void place(BlockPlaceEvent e){if(contains(e.getBlock().getLocation())&&!allowed(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void bucketEmpty(PlayerBucketEmptyEvent e){if(contains(e.getBlock().getLocation())&&!allowed(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void bucketFill(PlayerBucketFillEvent e){if(contains(e.getBlock().getLocation())&&!allowed(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void ignite(BlockIgniteEvent e){if(contains(e.getBlock().getLocation())&&!allowed(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void burn(BlockBurnEvent e){if(contains(e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void fade(BlockFadeEvent e){if(contains(e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void flow(BlockFromToEvent e){if(contains(e.getBlock().getLocation())||contains(e.getToBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void piston(BlockPistonExtendEvent e){if(e.getBlocks().stream().map(Block::getLocation).anyMatch(this::contains))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void piston(BlockPistonRetractEvent e){if(e.getBlocks().stream().map(Block::getLocation).anyMatch(this::contains))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void explosion(EntityExplodeEvent e){e.blockList().removeIf(b->contains(b.getLocation()));}
    @EventHandler(ignoreCancelled=true) public void blockExplosion(BlockExplodeEvent e){e.blockList().removeIf(b->contains(b.getLocation()));}
    @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGHEST) public void entityDamage(EntityDamageEvent e){
        if(!(e.getEntity() instanceof Display||e.getEntity() instanceof ArmorStand||e.getEntity() instanceof ItemFrame)||!contains(e.getEntity().getLocation()))return;
        Player attacker=e instanceof EntityDamageByEntityEvent byEntity&&byEntity.getDamager() instanceof Player p?p:null;
        if(!allowed(attacker))e.setCancelled(true);
    }
    @EventHandler(ignoreCancelled=true) public void dragon(EntityChangeBlockEvent e){if(contains(e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void hanging(HangingBreakEvent e){if(contains(e.getEntity().getLocation()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void armor(PlayerArmorStandManipulateEvent e){if(contains(e.getRightClicked().getLocation())&&!allowed(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void grow(StructureGrowEvent e){if(e.getBlocks().stream().anyMatch(b->contains(b.getLocation())))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void fertilize(BlockFertilizeEvent e){if(e.getBlocks().stream().anyMatch(b->contains(b.getLocation()))&&!allowed(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void dispense(BlockDispenseEvent e){Location target=e.getBlock().getLocation();if(e.getBlock().getBlockData() instanceof org.bukkit.block.data.Directional directional)target=e.getBlock().getRelative(directional.getFacing()).getLocation();if(contains(e.getBlock().getLocation())||contains(target))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void entityPlace(EntityPlaceEvent e){if(contains(e.getEntity().getLocation())&&!allowed(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void hangingPlace(HangingPlaceEvent e){if(contains(e.getEntity().getLocation())&&!allowed(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void vehicle(VehicleCreateEvent e){if(contains(e.getVehicle().getLocation()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void interact(PlayerInteractEvent e){if(e.getClickedBlock()!=null&&contains(e.getClickedBlock().getLocation())&&!allowed(e.getPlayer())){
        String block=e.getClickedBlock().getType().name(),item=e.getPlayer().getInventory().getItemInMainHand().getType().name();
        if(e.getAction()==Action.PHYSICAL||item.contains("SPAWN_EGG")||item.equals("END_CRYSTAL")||item.equals("DRAGON_EGG")||block.equals("DRAGON_EGG")||block.endsWith("_BED")||block.equals("RESPAWN_ANCHOR"))e.setCancelled(true);}}
}
