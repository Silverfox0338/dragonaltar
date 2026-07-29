package com.dragonaltar.ritual;

import com.dragonaltar.DragonAltarPlugin;
import com.dragonaltar.dragonevent.DragonEventState;
import com.dragonaltar.api.event.*;
import com.dragonaltar.persistence.YamlDataStore;
import com.dragonaltar.soul.DragonSoul;
import com.dragonaltar.soul.SoulIdentity;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class RitualManager {
    private final DragonAltarPlugin plugin; private final YamlDataStore store; private RitualSession active; private BukkitTask task;
    private BossBar bossBar;private final OfferingService offerings;
    private final Map<UUID,List<ItemStack>> pendingRefunds=new HashMap<>();
    public RitualManager(DragonAltarPlugin plugin,YamlDataStore store){this.plugin=plugin;this.store=store;this.offerings=new OfferingService(plugin);}
    public OfferingService offerings(){return offerings;}
    public OfferingService.Plan plan(Player player){return offerings.plan(player);}
    public Optional<RitualSession> active(){return Optional.ofNullable(active);}
    public void recover(){
        if(task!=null){task.cancel();task=null;}if(bossBar!=null){Bukkit.getOnlinePlayers().forEach(p->p.hideBossBar(bossBar));bossBar=null;}active=null;
        pendingRefunds.clear();
        YamlConfiguration y=store.load("rituals.yml");
        var refunds=y.getConfigurationSection("pending-refunds");if(refunds!=null)for(String key:refunds.getKeys(false)){try{UUID id=UUID.fromString(key);List<ItemStack> items=new ArrayList<>();for(Object value:refunds.getList(key,List.of()))if(value instanceof ItemStack item)items.add(item);pendingRefunds.put(id,items);}catch(IllegalArgumentException ignored){}}
        String player=y.getString("active.player"),soul=y.getString("active.soul");
        if(player!=null&&soul!=null){try{UUID playerId=UUID.fromString(player);List<ItemStack> items=new ArrayList<>();for(Object value:y.getList("active.consumed",List.of()))if(value instanceof ItemStack item)items.add(item);if(!items.isEmpty())pendingRefunds.computeIfAbsent(playerId,k->new ArrayList<>()).addAll(items);}catch(IllegalArgumentException ignored){}
            plugin.souls().byId(soul).filter(x->x.state()==com.dragonaltar.soul.DragonSoulState.RITUAL_RESERVED).ifPresent(x->plugin.souls().release(soul));
            plugin.getLogger().warning("Recovered interrupted ritual "+soul+" and queued consumed offerings for refund.");plugin.audit().record("RITUAL_RECOVERY","SYSTEM",soul+" interrupted; offerings queued");clear();}
        for(DragonSoul candidate:plugin.souls().all())if(candidate.state()==com.dragonaltar.soul.DragonSoulState.RITUAL_RESERVED){plugin.souls().release(candidate.id());plugin.getLogger().warning("Released orphan ritual reservation "+candidate.id()+".");}
    }
    public void refundPending(Player player){List<ItemStack> items=pendingRefunds.remove(player.getUniqueId());if(items==null)return;List<ItemStack> left=refund(player,items);if(!left.isEmpty())pendingRefunds.put(player.getUniqueId(),left);persist();plugin.messages().send(player,"ritual-refund");}
    public int pendingRefundCount(UUID player){return pendingRefunds.getOrDefault(player,List.of()).size();}
    public void consumeForDiagnostic(Player player){OfferingService.Plan plan=offerings.plan(player);List<ItemStack> consumed=offerings.consume(player,plan);pendingRefunds.computeIfAbsent(player.getUniqueId(),k->new ArrayList<>()).addAll(consumed);persist();plugin.audit().record("DEV_RECIPE_CONSUME",player.getUniqueId().toString(),"Exact offerings queued for recipe-refund");}
    public void start(Player player){
        if(active!=null)throw new IllegalStateException("A ritual is already active");
        if(plugin.dragonEvent().altarState()!=com.dragonaltar.altar.AltarState.ACTIVE)throw new IllegalStateException("The altar is not active");
        if(!plugin.eligibility().check(player).eligible())throw new IllegalStateException("Player is not eligible");
        if(plugin.combatTags().tagged(player.getUniqueId()))throw new IllegalStateException("Combat-tagged players cannot begin a ritual");
        Location interaction=plugin.configuredLocation("altar.yml","interaction");
        if(interaction==null||!interaction.getWorld().equals(player.getWorld())||interaction.distanceSquared(player.getLocation())>16)throw new IllegalStateException("Stand at the ritual interaction point");
        Location center=ritualCenter();double radius=plugin.configService().file("ritual.yml").getDouble("ritual-radius",8.0);if(center==null||!center.getWorld().equals(player.getWorld())||center.distanceSquared(player.getLocation())>radius*radius)throw new IllegalStateException("Stand within the ritual radius");
        DragonSoul soul=plugin.souls().reserveFirst(player.getUniqueId()).orElseThrow(()->new IllegalStateException("No soul is available"));
        OfferingService.Plan plan=offerings.plan(player);if(!plan.complete()){plugin.souls().release(soul.id());throw new IllegalStateException("Required offerings are missing");}
        List<ItemStack> consumed=plan.exactItems().stream().map(ItemStack::clone).toList();
        pendingRefunds.computeIfAbsent(player.getUniqueId(),k->new ArrayList<>()).addAll(consumed);persist();
        try{consumed=offerings.consume(player,plan);}catch(RuntimeException ex){pendingRefunds.remove(player.getUniqueId());persist();plugin.souls().release(soul.id());throw ex;}
        DragonRitualStartEvent apiEvent=new DragonRitualStartEvent(player,soul.id());Bukkit.getPluginManager().callEvent(apiEvent);
        if(apiEvent.isCancelled()){pendingRefunds.remove(player.getUniqueId());plugin.souls().release(soul.id());queueOverflow(player,refund(player,consumed));persist();throw new IllegalStateException("Ritual cancelled by another plugin");}
        active=new RitualSession(player.getUniqueId(),soul.id(),RitualPhase.OFFERINGS_ACCEPTED,consumed,UUID.randomUUID());pendingRefunds.remove(player.getUniqueId());persist();
        plugin.consequences().recordRitualCast();
        plugin.audit().record("RITUAL_START",player.getUniqueId().toString(),soul.id()+" session="+active.sessionId());
        plugin.animations().play("ritual-start",player.getLocation(),player);
        Location egg=plugin.configuredLocation("altar.yml","egg-display");if(egg!=null)plugin.animations().play("ritual-egg",egg,player);
        runPhase(0);
    }
    private void runPhase(int index){
        RitualPhase[] phases=RitualPhase.values();if(index>=phases.length){complete();return;}
        active=active.withPhase(phases[index]);persist();Player p=Bukkit.getPlayer(active.playerId());
        if(p!=null&&phases[index]==RitualPhase.ASCENSION){Location arrival=plugin.configuredLocation("altar.yml","arrival");if(arrival!=null)p.teleportAsync(arrival);}
        float progress=Math.max(0.01F,(index+1F)/phases.length);
        String phaseName=phases[index].name().replace('_',' '),configuredTitle=plugin.configService().file("ritual.yml").getString("cinematic.boss-bar-title","Dragon Ritual: {phase}");
        Component phaseTitle=Component.text(configuredTitle.replace("{phase}",phaseName));
        BossBar.Color color;try{color=BossBar.Color.valueOf(plugin.configService().file("ritual.yml").getString("cinematic.boss-bar-color","PURPLE").toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ex){color=BossBar.Color.PURPLE;}
        if(bossBar==null)bossBar=BossBar.bossBar(phaseTitle,progress,color,BossBar.Overlay.PROGRESS);
        else{bossBar.name(phaseTitle);bossBar.progress(progress);}
        if(p!=null){var settings=plugin.players().settings(p.getUniqueId());if(plugin.configService().file("ritual.yml").getBoolean("cinematic.boss-bar",true)&&settings.screenEffects())p.showBossBar(bossBar);else p.hideBossBar(bossBar);if(plugin.configService().file("ritual.yml").getBoolean("cinematic.titles",true)&&settings.titles())p.showTitle(net.kyori.adventure.title.Title.title(phaseTitle,Component.empty()));if(settings.animationParticles())try{Particle particle=Particle.valueOf(plugin.configService().file("ritual.yml").getString("cinematic.particle","DRAGON_BREATH"));Location center=ritualCenter();Object data=particle.getDataType()==Float.class?1.0f:null;int count=Math.max(0,Math.min(512,plugin.configService().file("ritual.yml").getInt("cinematic.particle-count",30)));p.getWorld().spawnParticle(particle,center==null?p.getLocation():center,count,.8,1,.8,.02,data);}catch(IllegalArgumentException ignored){}}
        long ticks=Math.max(0,Math.min(72_000,plugin.configService().file("ritual.yml").getLong("phases."+phases[index].name(),40)));
        task=Bukkit.getScheduler().runTaskLater(plugin,()->runPhase(index+1),ticks);
    }
    public void complete(){
        if(active==null)return;Player p=Bukkit.getPlayer(active.playerId());if(p==null){cancel(true);return;}
        DragonSoul reserved=plugin.souls().byId(active.soulId()).orElseThrow(()->new IllegalStateException("Reserved soul no longer exists"));
        if(reserved.state()!=com.dragonaltar.soul.DragonSoulState.RITUAL_RESERVED||!active.playerId().equals(reserved.reservedFor())){cancel(true);throw new IllegalStateException("Ritual reservation is no longer valid");}
        if(plugin.consequences().shouldFracture()){
            String soulId=active.soulId();
            plugin.consequences().manifest(soulId,p.getLocation(),"INITIAL_RITUAL_INSTABILITY");
            plugin.animations().play("soul-depart",p.getLocation(),p);
            plugin.audit().record("RITUAL_FRACTURE",p.getUniqueId().toString(),soulId+" session="+active.sessionId());
            clear();
            finishAltarIfDepleted(p);
            return;
        }
        plugin.souls().assign(active.soulId(),active.playerId(),"INITIAL_RITUAL");plugin.dragonborn().apply(p);
        Location arrival=plugin.configuredLocation("altar.yml","arrival");plugin.animations().play("ritual-complete",arrival==null?p.getLocation():arrival,p);
        Location egg=plugin.configuredLocation("altar.yml","egg-display");if(egg!=null)plugin.animations().play("egg-claim",egg,p);
        Bukkit.getPluginManager().callEvent(new DragonRitualCompleteEvent(p,active.soulId()));plugin.audit().record("RITUAL_COMPLETE",p.getUniqueId().toString(),active.soulId()+" session="+active.sessionId());
        Bukkit.broadcast(plugin.messages().component("dragonborn-gain","player",p.getName(),"soul",SoulIdentity.displayName(active.soulId())));clear();
        finishAltarIfDepleted(p);
    }
    public void cancel(boolean refund){
        if(active==null)return;if(task!=null)task.cancel();Player p=Bukkit.getPlayer(active.playerId());
        plugin.audit().record("RITUAL_CANCEL",active.playerId().toString(),active.soulId()+" refund="+refund+" phase="+active.phase());
        if(refund){if(p!=null)queueOverflow(p,refund(p,active.consumed()));else pendingRefunds.computeIfAbsent(active.playerId(),k->new ArrayList<>()).addAll(active.consumed());}
        plugin.souls().byId(active.soulId()).filter(x->active.playerId().equals(x.reservedFor())).ifPresent(x->plugin.souls().release(x.id()));clear();
    }
    public void stop(){
        if(active!=null)cancel(true);
        else clear();
    }
    private void persist(){YamlConfiguration y=new YamlConfiguration();y.set("data-version",1);if(active!=null){y.set("active.player",active.playerId().toString());y.set("active.soul",active.soulId());y.set("active.phase",active.phase().name());y.set("active.session",active.sessionId().toString());y.set("active.consumed",active.consumed());}for(var entry:pendingRefunds.entrySet())y.set("pending-refunds."+entry.getKey(),entry.getValue());store.save("rituals.yml",y);}
    private static List<ItemStack> refund(Player player,List<ItemStack> items){List<ItemStack> overflow=new ArrayList<>();for(ItemStack item:items)overflow.addAll(player.getInventory().addItem(item.clone()).values());return overflow;}
    private void queueOverflow(Player player,List<ItemStack> items){if(items.isEmpty())return;pendingRefunds.computeIfAbsent(player.getUniqueId(),k->new ArrayList<>()).addAll(items);persist();player.sendMessage("Your inventory was full. Use /dragon refunds to collect the remaining exact items.");}
    private void clear(){if(task!=null){task.cancel();task=null;}if(bossBar!=null){Bukkit.getOnlinePlayers().forEach(p->p.hideBossBar(bossBar));bossBar=null;}active=null;persist();}
    private void finishAltarIfDepleted(Player player){if(plugin.souls().unclaimedCount()==0){plugin.displays().holdEmptyFor(40);plugin.animations().play("egg-deplete",plugin.configuredLocation("altar.yml","egg-display"),player);plugin.dragonEvent().complete();plugin.dragonEvent().setAltarState(com.dragonaltar.altar.AltarState.DORMANT);}}
    private Location ritualCenter(){Location center=plugin.configuredLocation("altar.yml","ritual-center");return center==null?plugin.configuredLocation("altar.yml","altar-center"):center;}
}
