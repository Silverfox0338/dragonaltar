package com.dragonaltar.config;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

public final class ConfigValidator {
    private final ConfigService config;
    public ConfigValidator(ConfigService config){this.config=config;}
    public List<String> validate(){
        List<String> errors=new ArrayList<>();
        String mode=config.general().getString("server-mode","").toUpperCase(Locale.ROOT);if(!Set.of("BETA","PRODUCTION").contains(mode))errors.add("config.yml: invalid server-mode");
        long confirmationSeconds=config.general().getLong("event.confirmation-seconds",0);
        if(confirmationSeconds<=0||confirmationSeconds>300)errors.add("config.yml: event.confirmation-seconds must be from 1 to 300");
        if(config.general().getLong("event.scaled-dragon-reward-delay-ticks",-1)<0||config.general().getLong("event.altar-awakening-delay-ticks",-1)<0)errors.add("config.yml: event delays cannot be negative");
        if(config.general().getLong("event.scaled-dragon-reward-delay-ticks",0)>12_000||config.general().getLong("event.altar-awakening-delay-ticks",0)>12_000)errors.add("config.yml: event delays cannot exceed 12000 ticks");
        if(config.general().getDouble("event.nearby-player-radius",0)<=0||config.general().getDouble("event.nearby-player-radius",0)>512)errors.add("config.yml: event.nearby-player-radius must be from 0 to 512");
        if(config.general().getInt("transfer.natural-death-countdown-seconds",-1)<0||config.general().getInt("transfer.natural-death-countdown-seconds",0)>3600)errors.add("config.yml: transfer countdown must be from 0 to 3600 seconds");
        if(config.general().getLong("transfer.combat-tag-seconds",-1)<0||config.general().getLong("transfer.combat-tag-seconds",0)>3600)errors.add("config.yml: transfer.combat-tag-seconds must be from 0 to 3600");
        if(config.general().getLong("instability.threshold",6)<0)errors.add("config.yml: instability.threshold cannot be negative");
        double fractureChance=config.general().getDouble("instability.fracture-chance",.20);if(fractureChance<0||fractureChance>1)errors.add("config.yml: instability.fracture-chance must be from 0 to 1");
        long teleportMin=config.general().getLong("instability.teleport-min-seconds",45),teleportMax=config.general().getLong("instability.teleport-max-seconds",60);if(teleportMin<=0||teleportMax<teleportMin)errors.add("config.yml: invalid Fractured Soul teleport range");
        if(teleportMax>86_400)errors.add("config.yml: Fractured Soul teleport delay cannot exceed 86400 seconds");
        if(config.general().getInt("instability.teleport-radius",0)<8||config.general().getInt("instability.teleport-radius",0)>256)errors.add("config.yml: instability.teleport-radius must be from 8 to 256");
        if(config.general().getLong("forced-removal-ritual.backfire-limbo-hours",12)<=0)errors.add("config.yml: backfire-limbo-hours must be positive");
        if(!Set.of("RANDOM_ELIGIBLE","OPEN_RITUAL_SLOT","SOUL_DORMANT","PENDING_TRANSFER").contains(config.general().getString("transfer.dragonborn-killer-policy","").toUpperCase(Locale.ROOT)))errors.add("config.yml: invalid dragonborn-killer-policy");
        for(String file:List.of("messages.yml","altar.yml","ritual.yml","abilities.yml","animations.yml")){YamlConfiguration y=config.file(file);if(y==null)errors.add(file+": not loaded");else if(y.getInt("config-version",0)<1)errors.add(file+": missing config-version");}
        YamlConfiguration abilities=config.file("abilities.yml");if(abilities.getInt("energy.maximum",0)!=100)errors.add("abilities.yml: energy.maximum must remain 100");
        if(abilities.getInt("energy.regeneration",0)<0||abilities.getInt("energy.regeneration",0)>100)errors.add("abilities.yml: energy.regeneration must be from 0 to 100");
        if(abilities.getLong("energy.regeneration-interval-ticks",0)<=0||abilities.getLong("energy.regeneration-interval-ticks",0)>1200)errors.add("abilities.yml: regeneration interval must be from 1 to 1200 ticks");
        if(abilities.getLong("energy.delay-after-cast-ticks",-1)<0||abilities.getLong("energy.delay-after-cast-ticks",0)>72_000)errors.add("abilities.yml: energy delay must be from 0 to 72000 ticks");
        if(Material.matchMaterial(abilities.getString("focus.material",""))==null)errors.add("abilities.yml: invalid Focus material");
        double storedFraction=abilities.getDouble("abilities.titans-bulwark.stored-damage-fraction",-1);
        if(storedFraction<0||storedFraction>1)errors.add("abilities.yml: Bulwark stored-damage-fraction must be from 0 to 1");
        if(abilities.getDouble("abilities.titans-bulwark.stored-damage-cap",0)<=0)errors.add("abilities.yml: Bulwark stored-damage-cap must be positive");
        if(abilities.getDouble("abilities.titans-bulwark.expiry-max-damage",0)<abilities.getDouble("abilities.titans-bulwark.expiry-min-damage",0))errors.add("abilities.yml: Bulwark expiry damage range is invalid");
        if(abilities.getDouble("abilities.titans-bulwark.expiry-max-knockback",0)<abilities.getDouble("abilities.titans-bulwark.expiry-min-knockback",0))errors.add("abilities.yml: Bulwark expiry knockback range is invalid");
        if(abilities.getInt("abilities.infernos-wrath.maximum-mobility-ticks",0)<abilities.getInt("abilities.infernos-wrath.initial-mobility-ticks",0))errors.add("abilities.yml: Inferno maximum mobility must cover its initial mobility window");
        int maximumHeat=abilities.getInt("rev-hunt.heat.maximum",0);
        int mobilityHeat=abilities.getInt("rev-hunt.heat.mobility-threshold",-1);
        int trackingHeat=abilities.getInt("rev-hunt.heat.tracking-threshold",-1);
        if(maximumHeat<=0||mobilityHeat<0||trackingHeat<mobilityHeat||trackingHeat>maximumHeat)errors.add("abilities.yml: Rev Heat thresholds are invalid");
        if(abilities.getInt("rev-hunt.heat.per-target-gain-cooldown-ticks",0)<=0)errors.add("abilities.yml: Rev Heat target cooldown must be positive");
        if(abilities.getInt("rev-hunt.mark.maximum-targets",0)<=0)errors.add("abilities.yml: Rev mark target cap must be positive");
        if(abilities.getInt("abilities.revs-rend.recast-window-ticks",0)<=0||abilities.getDouble("abilities.revs-rend.recast-range",0)<=0)errors.add("abilities.yml: Rend recast window and range must be positive");
        if(abilities.getInt("abilities.wrath-of-rev.pulse-interval-ticks",0)<=0||abilities.getDouble("abilities.wrath-of-rev.radius",0)<=0)errors.add("abilities.yml: Wrath pulse interval and radius must be positive");
        if(abilities.getInt("abilities.infernos-wrath.hunt-duration-ticks",0)<=0||abilities.getDouble("abilities.infernos-wrath.radius",0)<=0)errors.add("abilities.yml: Inferno Hunt duration and radius must be positive");
        if(abilities.getInt("abilities.infernos-wrath.maximum-mobility-ticks",0)>abilities.getInt("abilities.infernos-wrath.hunt-duration-ticks",0))errors.add("abilities.yml: Inferno mobility cap cannot exceed Hunt duration");
        if(abilities.getInt("abilities.infernos-wrath.rampage-grants-per-target",0)<=0||abilities.getInt("abilities.infernos-wrath.maximum-rampage",0)<=0)errors.add("abilities.yml: Inferno Rampage limits must be positive");
        if(abilities.getDouble("resonances.unlock-range-blocks",0)<=0)errors.add("abilities.yml: resonance unlock range must be positive");
        double wardReduction=abilities.getDouble("resonances.glacial-bastion.ward-damage-reduction-fraction",-1);
        if(wardReduction<0||wardReduction>.9)errors.add("abilities.yml: Glacial ward reduction must be from 0 to 0.9");
        for(String resonance:List.of("thermal-convergence","volcanic-aegis","glacial-bastion","dragon-trinity")){
            if(abilities.getInt("resonances."+resonance+".cooldown-seconds",0)<=0)errors.add("abilities.yml: "+resonance+" cooldown must be positive");
            int cost=abilities.getInt("resonances."+resonance+".energy",-1);
            if(cost<0||cost>100)errors.add("abilities.yml: "+resonance+" energy must be from 0 to 100");
        }
        for(String ultimate:List.of("absolute-zero","infernos-wrath","titans-bulwark")){
            if(abilities.getInt("abilities."+ultimate+".downside.duration-seconds",-1)<0)errors.add("abilities.yml: "+ultimate+" downside duration cannot be negative");
            if(abilities.getInt("abilities."+ultimate+".downside.energy-regeneration-lock-seconds",-1)<0)errors.add("abilities.yml: "+ultimate+" regeneration lock cannot be negative");
        }
        for(String path:List.of("presentation.maximum-particles-per-emission","presentation.maximum-ring-points","presentation.maximum-flame-arc-particles","presentation.view-distance-blocks"))
            if(abilities.getInt(path,0)<=0)errors.add("abilities.yml: "+path+" must be positive");
        if(abilities.getInt("presentation.maximum-particles-per-emission",0)>512)errors.add("abilities.yml: maximum particles per emission cannot exceed 512");
        if(abilities.getInt("presentation.maximum-ring-points",0)>128)errors.add("abilities.yml: maximum ring points cannot exceed 128");
        if(abilities.getInt("presentation.view-distance-blocks",0)>128)errors.add("abilities.yml: presentation view distance cannot exceed 128 blocks");
        validateSpatialBounds(abilities,errors);
        validateAbilitySounds(abilities,errors);
        YamlConfiguration ritual=config.file("ritual.yml");if(!Set.of("INVENTORY_CONSUME","PEDESTAL_DEPOSIT","HYBRID").contains(ritual.getString("offering-mode","")))errors.add("ritual.yml: invalid offering-mode");
        for(Map<?,?> offering:ritual.getMapList("offerings")){
            if(Material.matchMaterial(String.valueOf(offering.get("material")))==null)errors.add("ritual.yml: invalid offering material "+offering.get("material"));
            int amount=number(offering.get("amount"),-1);if(amount<=0||amount>2304)errors.add("ritual.yml: offering amount must be from 1 to 2304");
        }
        if(ritual.getDouble("ritual-radius",0)<=0)errors.add("ritual.yml: ritual-radius must be positive");
        if(!Set.of("MOST_DAMAGED","LEAST_DAMAGED","FIRST_MATCH","LOWEST_ENCHANTMENT_VALUE").contains(ritual.getString("elytra.consumption-priority","MOST_DAMAGED").toUpperCase(Locale.ROOT)))errors.add("ritual.yml: invalid Elytra consumption priority");
        for(String phase:List.of("OFFERINGS_ACCEPTED","ALTAR_CHARGING","SOUL_AWAKENING","PLAYER_BINDING","ASCENSION","COMPLETION"))if(ritual.getLong("phases."+phase,-1)<0)errors.add("ritual.yml: invalid phase duration "+phase);
        try{net.kyori.adventure.bossbar.BossBar.Color.valueOf(ritual.getString("cinematic.boss-bar-color","PURPLE").toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ex){errors.add("ritual.yml: invalid boss-bar color");}
        try{org.bukkit.Particle.valueOf(ritual.getString("cinematic.particle","DRAGON_BREATH"));}catch(IllegalArgumentException ex){errors.add("ritual.yml: invalid cinematic particle");}
        if(ritual.getInt("cinematic.particle-count",0)<0||ritual.getInt("cinematic.particle-count",0)>512)errors.add("ritual.yml: cinematic particle-count must be from 0 to 512");
        YamlConfiguration altar=config.file("altar.yml");if(Material.matchMaterial(altar.getString("display.material",""))==null)errors.add("altar.yml: invalid display material");
        validateParticle(altar.getString("display.idle-particle",""),"altar.yml: invalid display particle",errors);
        validateSound(altar.getString("display.ambient-sound",""),"altar.yml: invalid ambient sound",errors);
        validateSound(altar.getString("display.heartbeat-sound",""),"altar.yml: invalid heartbeat sound",errors);
        if(altar.getInt("display.idle-particle-count",0)<0||altar.getInt("display.idle-particle-count",0)>128)errors.add("altar.yml: idle particle count must be from 0 to 128");
        if(altar.getInt("display.idle-particle-interval-ticks",0)<=0||altar.getInt("display.ambient-sound-interval-ticks",0)<=0||altar.getInt("display.heartbeat-interval-ticks",0)<=0)errors.add("altar.yml: display intervals must be positive");
        if(altar.getDouble("recipe-display.view-range",0)<=0||altar.getDouble("recipe-display.view-range",0)>128)errors.add("altar.yml: recipe display view range must be from 0 to 128");
        if(altar.getInt("recipe-display.line-width",0)<=0||altar.getInt("recipe-display.line-width",0)>2048)errors.add("altar.yml: recipe display line width must be from 1 to 2048");
        validateAnimations(config.file("animations.yml"),errors);
        return List.copyOf(errors);
    }

    private static void validateSpatialBounds(YamlConfiguration abilities,List<String> errors){
        for(String path:abilities.getKeys(true)){
            if(abilities.isConfigurationSection(path))continue;
            String key=path.substring(path.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
            if(!(key.equals("radius")||key.endsWith("-radius")||key.equals("range")||key.endsWith("-range")
                    ||key.endsWith("-distance")||key.equals("unlock-range-blocks")))continue;
            Object raw=abilities.get(path);if(!(raw instanceof Number number))continue;
            double value=number.doubleValue();
            if(value<0||value>64)errors.add("abilities.yml: "+path+" must be from 0 to 64");
        }
    }
    private static void validateAbilitySounds(YamlConfiguration abilities,List<String> errors){
        for(String path:abilities.getKeys(true)){
            if(abilities.isConfigurationSection(path))continue;
            String key=path.substring(path.lastIndexOf('.')+1);
            if(!path.contains(".sounds.")||key.endsWith("-volume")||key.endsWith("-pitch"))continue;
            validateSound(abilities.getString(path,""),"abilities.yml: invalid sound at "+path,errors);
        }
    }
    private static void validateAnimations(YamlConfiguration animations,List<String> errors){
        Set<String> types=Set.of("SOUND","PARTICLE","PARTICLE_RING","PARTICLE_SPIRAL","LIGHTNING_EFFECT","TITLE",
                "SUBTITLE","ACTION_BAR","BROADCAST","BOSS_BAR","PLAYER_GLOW","PLAYER_LEVITATE","TEMPORARY_WINGS",
                "DISPLAY_MOVE","DISPLAY_SCALE","DISPLAY_ROTATE","DISPLAY_FADE","WAIT");
        for(String id:animations.getKeys(false)){
            if(id.equals("config-version"))continue;
            for(Map<?,?> step:animations.getMapList(id)){
                String type=String.valueOf(step.get("type")).toUpperCase(Locale.ROOT);
                if(!types.contains(type)){errors.add("animations.yml: invalid type in "+id);continue;}
                long at=number(step.get("at-tick"),-1);if(at<0||at>72_000)errors.add("animations.yml: at-tick in "+id+" must be from 0 to 72000");
                int count=number(step.get("count"),0);if(count<0||count>512)errors.add("animations.yml: particle count in "+id+" must be from 0 to 512");
                long duration=number(step.get("duration"),0);if(duration<0||duration>72_000)errors.add("animations.yml: duration in "+id+" must be from 0 to 72000");
                Object particle=step.containsKey("particle")?step.get("particle"):"DRAGON_BREATH";
                if(type.startsWith("PARTICLE"))validateParticle(String.valueOf(particle),"animations.yml: invalid particle in "+id,errors);
                if(type.equals("SOUND"))validateSound(String.valueOf(step.get("sound")),"animations.yml: invalid sound in "+id,errors);
            }
        }
    }
    private static void validateParticle(String value,String message,List<String> errors){
        try{Particle.valueOf(value.toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ex){errors.add(message);}
    }
    private static void validateSound(String value,String message,List<String> errors){
        if(resolveSound(value)==null)errors.add(message);
    }
    private static org.bukkit.Sound resolveSound(String value){
        if(value==null||value.isBlank())return null;
        NamespacedKey key=NamespacedKey.fromString(value.toLowerCase(Locale.ROOT));
        org.bukkit.Sound sound=key==null?null:Registry.SOUNDS.get(key);
        if(sound!=null)return sound;
        try{
            Object legacy=org.bukkit.Sound.class.getField(value.toUpperCase(Locale.ROOT)).get(null);
            return legacy instanceof org.bukkit.Sound found?found:null;
        }catch(ReflectiveOperationException ex){return null;}
    }
    private static int number(Object value,int fallback){
        if(value instanceof Number number)return number.intValue();
        try{return Integer.parseInt(String.valueOf(value));}catch(NumberFormatException ex){return fallback;}
    }
}
