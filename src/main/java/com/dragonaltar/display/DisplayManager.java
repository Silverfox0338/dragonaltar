package com.dragonaltar.display;

import com.dragonaltar.soul.DragonSoulService;
import com.dragonaltar.DragonAltarPlugin;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;
import java.util.function.Supplier;

public final class DisplayManager {
    private final DragonAltarPlugin plugin;
    private final DragonSoulService souls;
    private final NamespacedKey key;
    private final NamespacedKey hologramKey;
    private final NamespacedKey displayIdKey;
    private final NamespacedKey legacyEggKey,legacyHologramKey;
    private BlockDisplay egg;
    private TextDisplay hologram;
    private BukkitTask task;
    private float angle;
    private long ticks;
    private boolean recovered;
    private Integer previewCount;
    private long holdEmptyUntil;
    public DisplayManager(DragonAltarPlugin plugin, DragonSoulService souls) { this.plugin = plugin; this.souls = souls; key = new NamespacedKey(plugin, "altar_egg");hologramKey=new NamespacedKey(plugin,"recipe_display");displayIdKey=new NamespacedKey(plugin,"display_id");legacyEggKey=new NamespacedKey(plugin,"egg_display");legacyHologramKey=new NamespacedKey(plugin,"egg_hologram"); }
    public void start(Supplier<Location> location) {
        if(task!=null&&!task.isCancelled())return;
        task=Bukkit.getScheduler().runTaskTimer(plugin,()->{
            Location desired=location.get();
            if(!recovered&&desired!=null){recover(desired);recovered=true;}
            if(ticks%100==0&&desired!=null)recover(desired);
            long count=effectiveCount();boolean visible=count>0||System.currentTimeMillis()<holdEmptyUntil;if(visible&&egg==null&&desired!=null)recover(desired);
            if(!visible&&egg!=null){egg.remove();egg=null;}
            if(!visible&&hologram!=null){hologram.remove();hologram=null;}
            if(hologram!=null)hologram.text(recipeText(count));
            if(count>0&&egg!=null&&egg.isValid()){var cfg=plugin.configService().file("altar.yml");angle+=(float)Math.toRadians(cfg.getDouble("display.rotation-degrees-per-tick",2)*2);ticks+=2;Transformation t=egg.getTransformation();t.getLeftRotation().set(new AxisAngle4f(angle,0,1,0));float sin=(float)Math.sin(angle);float cos=(float)Math.cos(angle);t.getTranslation().set(.5f-(cos+sin)*.5f,0,.5f-(-sin+cos)*.5f);t.getScale().set(1);egg.setTransformation(t);
                int particleInterval=Math.max(2,cfg.getInt("display.idle-particle-interval-ticks",10));if(ticks%particleInterval<2){try{Particle particle=Particle.valueOf(cfg.getString("display.idle-particle","DRAGON_BREATH"));egg.getWorld().spawnParticle(particle,egg.getLocation().add(.5,.5,.5),cfg.getInt("display.idle-particle-count",2),.4,.4,.4,.01);}catch(IllegalArgumentException ignored){}}
                if(ticks%Math.max(2,cfg.getInt("display.ambient-sound-interval-ticks",200))<2)playConfiguredSound(cfg.getString("display.ambient-sound","minecraft:block.beacon.ambient"),.5f,1f);
                if(ticks%Math.max(2,cfg.getInt("display.heartbeat-interval-ticks",100))<2)playConfiguredSound(cfg.getString("display.heartbeat-sound","minecraft:entity.warden.heartbeat"),.6f,.7f);
            }
        },20,2);
    }
    public void recover(Location location) {
        if (location == null || location.getWorld() == null) return;
        List<BlockDisplay> tagged = Bukkit.getWorlds().stream().flatMap(world->world.getEntitiesByClass(BlockDisplay.class).stream())
                .filter(d -> d.getPersistentDataContainer().has(key)||d.getPersistentDataContainer().has(legacyEggKey)).toList();
        List<TextDisplay> texts=Bukkit.getWorlds().stream().flatMap(world->world.getEntitiesByClass(TextDisplay.class).stream()).filter(d->d.getPersistentDataContainer().has(hologramKey)||d.getPersistentDataContainer().has(legacyHologramKey)).toList();
        texts.stream().filter(d->d.getPersistentDataContainer().has(legacyHologramKey)&&!d.getPersistentDataContainer().has(hologramKey)).forEach(Entity::remove);
        tagged.stream().filter(d->!d.getWorld().equals(location.getWorld())||d.getLocation().distanceSquared(location)>.01).forEach(Entity::remove);
        Location recipeLocation=recipeLocation(location);
        texts.stream().filter(d->!d.getWorld().equals(recipeLocation.getWorld())||d.getLocation().distanceSquared(recipeLocation)>.01).forEach(Entity::remove);
        tagged=tagged.stream().filter(d->d.isValid()&&d.getWorld().equals(location.getWorld())&&d.getLocation().distanceSquared(location)<=.01).toList();
        texts=texts.stream().filter(d->d.isValid()&&d.getWorld().equals(recipeLocation.getWorld())&&d.getLocation().distanceSquared(recipeLocation)<=.01).toList();
        egg = tagged.isEmpty() ? null : tagged.getFirst();
        hologram=texts.isEmpty()?null:texts.getFirst();
        if(egg!=null){egg.setRotation(0,0);egg.getPersistentDataContainer().set(key,PersistentDataType.BYTE,(byte)1);egg.getPersistentDataContainer().set(displayIdKey,PersistentDataType.STRING,"altar-egg");}
        if(hologram!=null){hologram.getPersistentDataContainer().set(hologramKey,PersistentDataType.BYTE,(byte)1);hologram.getPersistentDataContainer().set(displayIdKey,PersistentDataType.STRING,"ritual-recipe");}
        tagged.stream().skip(1).forEach(Entity::remove);
        texts.stream().skip(1).forEach(Entity::remove);
        boolean visible=effectiveCount()>0||System.currentTimeMillis()<holdEmptyUntil;
        if(visible&&egg==null)spawn(location);
        if(visible&&hologram==null&&plugin.configService().file("altar.yml").getBoolean("recipe-display.enabled",true))spawnHologram(location);
        if(!visible){if(egg!=null)egg.remove();if(hologram!=null)hologram.remove();egg=null;hologram=null;}
    }
    public void spawn(Location location) {
        remove();var cfg=plugin.configService().file("altar.yml");Material material=Material.matchMaterial(cfg.getString("display.material","DRAGON_EGG"));Location displayLocation=location.clone();displayLocation.setYaw(0);displayLocation.setPitch(0);egg = location.getWorld().spawn(displayLocation, BlockDisplay.class, d -> {
            d.setBlock(Bukkit.createBlockData(material==null?Material.DRAGON_EGG:material)); d.setGlowing(cfg.getBoolean("display.glow",true));
            try{d.setGlowColorOverride(Color.fromRGB(Integer.parseInt(cfg.getString("display.glow-color","AA00AA"),16)));}catch(IllegalArgumentException ignored){}
            d.setGravity(false);d.setInvulnerable(true);d.setPersistent(true);d.setInterpolationDuration(2); d.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);d.getPersistentDataContainer().set(displayIdKey,PersistentDataType.STRING,"altar-egg");
        });
        resetTransformation();
        if(hologram==null&&plugin.configService().file("altar.yml").getBoolean("recipe-display.enabled",true))spawnHologram(location);
    }
    private void spawnHologram(Location location){var cfg=plugin.configService().file("altar.yml");hologram=location.getWorld().spawn(recipeLocation(location),TextDisplay.class,d->{d.text(recipeText(effectiveCount()));try{d.setBillboard(Display.Billboard.valueOf(cfg.getString("recipe-display.billboard","CENTER").toUpperCase(Locale.ROOT)));}catch(IllegalArgumentException ex){d.setBillboard(Display.Billboard.CENTER);}d.setShadowed(cfg.getBoolean("recipe-display.shadowed",true));d.setSeeThrough(cfg.getBoolean("recipe-display.see-through",false));d.setLineWidth(cfg.getInt("recipe-display.line-width",220));d.setViewRange((float)cfg.getDouble("recipe-display.view-range",32));d.setInvulnerable(true);d.setPersistent(true);d.getPersistentDataContainer().set(hologramKey,PersistentDataType.BYTE,(byte)1);d.getPersistentDataContainer().set(displayIdKey,PersistentDataType.STRING,"ritual-recipe");});}
    private Location recipeLocation(Location anchor){var cfg=plugin.configService().file("altar.yml");return anchor.clone().add(cfg.getDouble("recipe-display.offset.x",0),cfg.getDouble("recipe-display.offset.y",1.8),cfg.getDouble("recipe-display.offset.z",0));}
    private net.kyori.adventure.text.Component recipeText(long count){List<String> lines=plugin.configService().file("altar.yml").getStringList("recipe-display.lines");return MiniMessage.miniMessage().deserialize(String.join("\n",lines).replace("%remaining_souls%",Long.toString(count)));}
    public void reload(){recovered=false;Location location=plugin.configuredLocation("altar.yml","egg-display");if(location!=null)recover(location);}
    public int duplicateCount(){int eggs=0,texts=0;for(World world:Bukkit.getWorlds()){eggs+=world.getEntitiesByClass(BlockDisplay.class).stream().filter(d->d.getPersistentDataContainer().has(key)).count();texts+=world.getEntitiesByClass(TextDisplay.class).stream().filter(d->d.getPersistentDataContainer().has(hologramKey)).count();}return Math.max(0,eggs-1)+Math.max(0,texts-1);}
    public String inspect(){return "egg="+describe(egg)+", recipe="+describe(hologram)+", duplicates="+duplicateCount();}
    public boolean owns(Entity entity){return entity.getPersistentDataContainer().has(key)||entity.getPersistentDataContainer().has(hologramKey)||entity.getPersistentDataContainer().has(displayIdKey);}
    private static String describe(Entity entity){return entity==null?"missing":entity.getUniqueId()+" @ "+entity.getWorld().getName()+" "+entity.getLocation().toVector();}
    public void removeRecipe(){if(hologram!=null)hologram.remove();hologram=null;for(World world:Bukkit.getWorlds())world.getEntitiesByClass(TextDisplay.class).stream().filter(d->d.getPersistentDataContainer().has(hologramKey)).forEach(Entity::remove);}
    public void resetTransformation(){if(egg!=null){egg.setTransformation(new Transformation(new org.joml.Vector3f(),new org.joml.AxisAngle4f(),new org.joml.Vector3f(1,1,1),new org.joml.AxisAngle4f()));angle=0;}}
    public void previewCount(int count){if(count<0||count>3)throw new IllegalArgumentException("Preview count must be 0-3");previewCount=count;}
    public void resetPreview(){previewCount=null;recovered=false;}
    public void holdEmptyFor(long ticks){holdEmptyUntil=Math.max(holdEmptyUntil,System.currentTimeMillis()+Math.max(0,ticks)*50L);}
    private long effectiveCount(){return previewCount==null?(plugin.dragonEvent().altarState()==com.dragonaltar.altar.AltarState.ACTIVE?souls.unclaimedCount():0):previewCount;}
    public void remove() { if (egg != null) egg.remove();if(hologram!=null)hologram.remove(); egg = null;hologram=null; }
    public void stop(){if(task!=null)task.cancel();remove();}
    private void playConfiguredSound(String value,float volume,float pitch){NamespacedKey key=NamespacedKey.fromString(value);if(key==null)return;Sound sound=Registry.SOUNDS.get(key);if(sound!=null&&egg!=null)egg.getWorld().playSound(egg.getLocation(),sound,volume,pitch);}
}
