package com.dragonaltar.command;

import com.dragonaltar.DragonAltarPlugin;
import com.dragonaltar.ability.DragonAbility;
import com.dragonaltar.config.ServerMode;
import com.dragonaltar.soul.DragonSoul;
import com.dragonaltar.soul.SoulIdentity;
import com.dragonaltar.player.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.Duration;
import java.util.*;

public final class DragonCommand implements TabExecutor {
    private final DragonAltarPlugin plugin;
    private final Map<UUID,PendingDanger> dangerous=new HashMap<>();
    private final Map<UUID,UUID> animationSessions=new HashMap<>();
    public DragonCommand(DragonAltarPlugin plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!sender.hasPermission("dragonaltar.use")){plugin.messages().send(sender,"no-permission");return true;}
        if (args.length == 0) { status(sender); return true; }
        try {
            return switch (args[0].toLowerCase()) {
                case "status" -> { status(sender); yield true; }
                case "help" -> { help(sender); yield true; }
                case "abilities" -> { Collection<DragonAbility> list=sender instanceof Player p?plugin.abilities().abilities(p):plugin.abilities().abilities();plugin.messages().send(sender,"abilities-list","abilities",String.join(", ",list.stream().map(DragonAbility::id).toList()));yield true; }
                case "focus" -> { Player p=player(sender);if(!plugin.dragonborn().isDragonborn(p.getUniqueId()))plugin.messages().send(sender,"focus-unavailable");else{plugin.dragonborn().ensureFocus(p);plugin.messages().send(sender,"focus-restored");}yield true; }
                case "settings" -> { settings(sender,args); yield true; }
                case "history" -> { history(sender, args); yield true; }
                case "refunds" -> {Player p=player(sender);plugin.rituals().refundPending(p);p.sendMessage("Pending refund entries: "+plugin.rituals().pendingRefundCount(p.getUniqueId()));yield true;}
                case "event" -> event(sender, args);
                case "setup" -> setup(sender, args);
                case "altar" -> altar(sender, args);
                case "ritual" -> ritual(sender, args);
                case "protection" -> protection(sender, args);
                case "admin" -> admin(sender, args);
                case "system" -> system(sender, args);
                case "dev" -> dev(sender, args);
                case "confirm" -> {confirmDanger(sender,args);yield true;}
                default -> { help(sender); yield true; }
            };
        } catch (IllegalArgumentException | IllegalStateException ex) {plugin.messages().send(sender,"command-error","message",ex.getMessage()==null?"Unknown error":ex.getMessage());return true;}
    }
    private boolean event(CommandSender s, String[] a) {
        require(s,"dragonaltar.admin.event");
        String sub=a.length>1?a[1]:"status";
        if (sub.equals("status")) { s.sendMessage("Ancient Dragon Event: "+plugin.dragonEvent().state()); return true; }
        if(sub.equals("preview")){eventPreview(s,false);return true;}
        if (sub.equals("start")) {
            eventPreview(s,true);return true;
        }
        if (sub.equals("confirm-start")&&a.length>2) {
            Player p=player(s); if(!plugin.confirmations().consume(p.getUniqueId(),a[2],"event-start",List.of())) throw new IllegalArgumentException("Invalid or expired confirmation");
            plugin.ensureRuntimeTasks();plugin.dragonEvent().start(p,Objects.requireNonNull(plugin.configuredLocation("altar.yml","fountain"),"Fountain is unconfigured"),plugin.crystalLocations()); return true;
        }
        if(sub.equals("abort")) {danger(s,"event-abort",List.of(),()->plugin.dragonEvent().abort(senderId(s)));return true;}
        if(sub.equals("rescan")){s.sendMessage(plugin.dragonEvent().rescan());return true;}
        if(sub.equals("recover")){s.sendMessage(plugin.dragonEvent().recover());return true;}
        if(sub.equals("locate")){var dragon=plugin.dragonEvent().canonicalDragon().orElseThrow(()->new IllegalStateException("Canonical dragon is not loaded"));Location l=dragon.getLocation();s.sendMessage("Canonical dragon: "+l.getWorld().getName()+" "+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ());return true;}
        if(sub.equals("dragon-info")){s.sendMessage(plugin.dragonEvent().canonicalDragon().map(d->"UUID="+d.getUniqueId()+" health="+d.getHealth()+"/"+Objects.requireNonNull(d.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)).getValue()+" phase="+d.getPhase()).orElse("Canonical dragon not loaded"));return true;}
        s.sendMessage("Supported: status, preview, start, confirm-start, abort"); return true;
    }
    private void eventPreview(CommandSender s,boolean issueToken){
        Location f=plugin.configuredLocation("altar.yml","fountain");List<String> errors=plugin.dragonEvent().validateStart(f,plugin.crystalLocations());
        if(issueToken&&!errors.isEmpty())throw new IllegalStateException(String.join("; ",errors));
        double radius=plugin.getConfig().getDouble("event.nearby-player-radius",128);
        StringBuilder text=new StringBuilder("Ancient Dragon Event\nWorld: ").append(f==null?"unconfigured":f.getWorld().getName())
                .append("\nFountain: ").append(f==null?"unconfigured":f.getBlockX()+", "+f.getBlockY()+", "+f.getBlockZ())
                .append("\nNearby players: ").append(f==null?0:f.getNearbyPlayers(radius).size())
                .append("\nScaledEnderDragon: ").append(plugin.integrations().contains("ScaledEnderDragon")?"Detected":"Not detected")
                .append("\nAltar configured: ").append(plugin.validateSetup().equals("Valid"))
                .append("\nOptional internal protection: ").append(plugin.protectionEnabled()?(plugin.protectionConfigured()?"Enabled/configured":"Enabled/incomplete"):"Disabled")
                .append("\nAvailable Dragon Souls after victory: 3")
                .append("\nPreflight: ").append(errors.isEmpty()?"Ready":String.join("; ",errors));
        if(issueToken){Player p=player(s);String token=plugin.confirmations().issue(p.getUniqueId(),"event-start",List.of(),confirmationDuration());text.append("\nRun: /dragon event confirm-start ").append(token).append("\nThe confirmation expires in ").append(confirmationDuration().toSeconds()).append(" seconds.");}
        s.sendMessage(text.toString());
    }
    private boolean setup(CommandSender s,String[] a) {
        require(s,"dragonaltar.setup"); Player p=player(s); String sub=a.length>1?a[1]:"status";
        if(sub.equals("begin")){plugin.setup().begin(p);s.sendMessage("Setup session started; it expires after 15 minutes of inactivity.");return true;}
        if(sub.equals("status")) { s.sendMessage(plugin.setupStatus()+" | session="+plugin.setup().active(p)+(plugin.setup().active(p)?" staged="+plugin.setup().staged(p):"")); return true; }
        Map<String,String> points=Map.of("setaltarcenter","altar-center","setritualcenter","ritual-center","setegg","egg-display","setinteraction","interaction",
                "setfountain","fountain","setarrival","arrival","pos1","protection.pos1","pos2","protection.pos2");
        if(points.containsKey(sub)) {
            Location location=setupLocation(p,a,2);
            plugin.setup().set(p,points.get(sub),location);
            s.sendMessage(sub+" staged at "+formatLocation(location)+".");
            return true;
        }
        if(sub.equals("setcrystal")&&a.length>2) {
            if(!List.of("north","south","east","west").contains(a[2].toLowerCase()))throw new IllegalArgumentException("Direction must be north, south, east, or west");
            Location location=setupLocation(p,a,3);
            plugin.setup().set(p,"crystals."+a[2].toLowerCase(),location);
            s.sendMessage("setcrystal "+a[2].toLowerCase()+" staged at "+formatLocation(location)+".");
            return true;
        }
        if(sub.equals("setpedestal")&&a.length>2){plugin.setup().set(p,"pedestals."+a[2],p.getLocation());return true;}
        if(sub.equals("removepedestal")&&a.length>2){plugin.setup().remove(p,"pedestals."+a[2]);return true;}
        if(sub.equals("validate")){s.sendMessage(plugin.validationReport()+"\nStaged changes: "+plugin.setup().staged(p));return true;}
        if(sub.equals("preview")){plugin.setup().preview(p);return true;}
        if(sub.equals("save")){plugin.setup().save(p);if(plugin.validateSetup().equals("Valid")){plugin.dragonEvent().setAltarState(com.dragonaltar.altar.AltarState.CONFIGURED);plugin.ensureRuntimeTasks();}s.sendMessage("Setup saved. "+plugin.validateSetup());return true;}
        if(sub.equals("cancel")){plugin.setup().cancel(p);s.sendMessage("Staged setup changes discarded.");return true;}
        s.sendMessage("See COMMANDS.md for setup commands."); return true;
    }
    private boolean altar(CommandSender s,String[] a) {
        require(s,"dragonaltar.admin.altar");String sub=a.length>1?a[1]:"status";
        if(sub.equals("status")||sub.equals("validate")){s.sendMessage(plugin.validationReport()+"\nUnclaimed="+plugin.souls().unclaimedCount());return true;}
        if(sub.equals("teleport")){Player p=player(s);Location l=plugin.configuredLocation("altar.yml","altar-center");if(l==null)throw new IllegalStateException("Altar center unconfigured");p.teleportAsync(l);return true;}
        if(sub.equals("preview")||sub.equals("awaken")){if(sub.equals("awaken"))requireForcedStateAllowed();Location l=plugin.configuredLocation("altar.yml","altar-center");if(l==null)throw new IllegalStateException("Altar center unconfigured");plugin.animations().play("altar-awaken",l,s instanceof Player p?p:null);if(sub.equals("awaken"))plugin.dragonEvent().setAltarState(com.dragonaltar.altar.AltarState.ACTIVE);return true;}
        if(sub.equals("deactivate")||sub.equals("dormancy")){requireForcedStateAllowed();plugin.dragonEvent().setAltarState(com.dragonaltar.altar.AltarState.DORMANT);plugin.displays().previewCount(0);plugin.displays().remove();return true;}
        if(sub.equals("activate")){requireForcedStateAllowed();plugin.dragonEvent().setAltarState(com.dragonaltar.altar.AltarState.ACTIVE);plugin.displays().resetPreview();plugin.displays().recover(plugin.configuredLocation("altar.yml","egg-display"));return true;}
        if(sub.equals("egg")&&a.length>2){Location l=plugin.configuredLocation("altar.yml","egg-display");switch(a[2]){case "spawn"->{plugin.displays().previewCount((int)Math.max(1,plugin.souls().unclaimedCount()));plugin.displays().recover(l);}case "reset","teleport-to-config"->{plugin.displays().resetPreview();plugin.displays().recover(l);plugin.displays().resetTransformation();}case "remove"->{plugin.displays().previewCount(0);plugin.displays().remove();}case "inspect"->s.sendMessage(plugin.displays().inspect());case "animate"->{if(a.length<4)throw new IllegalArgumentException("Animation required");String animation=Map.of("idle","egg-idle","awaken","altar-awaken","claim","egg-claim","deplete","egg-deplete").getOrDefault(a[3],a[3]);plugin.animations().play(animation,l,s instanceof Player p?p:null);}case "count"->{if(a.length<4)throw new IllegalArgumentException("Count 0-3 required");plugin.displays().previewCount(Integer.parseInt(a[3]));plugin.displays().recover(l);}default->throw new IllegalArgumentException("Unknown egg action");}return true;}
        if(sub.equals("recipe")&&a.length>2){Location l=plugin.configuredLocation("altar.yml","egg-display");switch(a[2]){
            case "spawn","refresh","preview"->{plugin.displays().resetPreview();plugin.displays().recover(l);}
            case "move"->{
                if(l==null)throw new IllegalStateException("Egg display location is unconfigured");
                Location target=setupLocation(player(s),a,3);
                if(!target.getWorld().equals(l.getWorld()))throw new IllegalArgumentException("Recipe display must be in the egg display world");
                plugin.setAltarValue("recipe-display.offset.x",target.getX()-l.getX());
                plugin.setAltarValue("recipe-display.offset.y",target.getY()-l.getY());
                plugin.setAltarValue("recipe-display.offset.z",target.getZ()-l.getZ());
                plugin.displays().removeRecipe();
                plugin.displays().recover(l);
                s.sendMessage("Recipe display moved to "+formatLocation(target)+".");
            }
            case "remove"->plugin.displays().removeRecipe();
            case "inspect"->s.sendMessage(plugin.displays().inspect());
            default->throw new IllegalArgumentException("Unknown recipe action");
        }return true;}
        throw new IllegalArgumentException("Unknown altar operation");
    }
    private boolean ritual(CommandSender s,String[] a) {
        String sub=a.length>1?a[1]:"status";
        if(sub.equals("status")||sub.equals("inspect")){s.sendMessage("Ritual: "+plugin.rituals().active().map(Object::toString).orElse("inactive"));return true;}
        require(s,"dragonaltar.admin.ritual");
        if(sub.equals("start")&&a.length>2){plugin.rituals().start(requiredOnline(a[2]));return true;}
        if(sub.equals("stop")||sub.equals("fail")||sub.equals("refund")){plugin.rituals().cancel(true);return true;}
        if(sub.equals("complete")){requireForcedStateAllowed();danger(s,"ritual-force-complete",List.of(),()->plugin.rituals().complete());return true;}
        if(sub.equals("reserve")&&a.length>3){plugin.souls().reserve(a[2],requiredOnline(a[3]).getUniqueId());return true;}
        if(sub.equals("release")&&a.length>2){plugin.souls().release(a[2]);return true;}
        s.sendMessage("Supported: status, inspect, start, stop, complete, fail, refund");return true;
    }
    private boolean protection(CommandSender s,String[] a) {
        String sub=a.length>1?a[1]:"status";
        if(sub.equals("bypass")) { Player p=player(s); require(s,"dragonaltar.protection.bypass"); plugin.toggleBypass(p); return true; }
        require(s,"dragonaltar.admin.protection");
        if(sub.equals("enable"))plugin.setAltarValue("protection.enabled",true);
        else if(sub.equals("disable")){danger(s,"protection-disable",List.of(),()->plugin.setAltarValue("protection.enabled",false));return true;}
        else if(sub.equals("setpos1")||sub.equals("setpos2"))plugin.saveLocation("altar.yml","protection."+sub.substring(3),player(s).getLocation());
        else if(sub.equals("visualize")){Player p=player(s);Location a1=plugin.configuredLocation("altar.yml","protection.pos1"),a2=plugin.configuredLocation("altar.yml","protection.pos2");if(a1==null||a2==null)throw new IllegalStateException("Region unconfigured");for(int i=0;i<80;i++){double t=i/79d;p.spawnParticle(Particle.END_ROD,a1.clone().multiply(1-t).add(a2.clone().toVector().multiply(t)),1,0,0,0,0);}}
        s.sendMessage("Protection enabled="+plugin.protectionEnabled()+" configured="+plugin.protectionConfigured()); return true;
    }
    private void settings(CommandSender s,String[] a){
        Player p=player(s);PlayerSettings old=plugin.players().settings(p.getUniqueId());
        if(a.length==1||(a.length>1&&a[1].equalsIgnoreCase("menu"))){plugin.settingsMenu().open(p);return;}
        if(a.length<3){plugin.messages().send(s,"settings-status","effects",old.effects().name(),"hud",Boolean.toString(old.hud()),"selector",old.selector().name(),"slowfall",Boolean.toString(old.slowFalling()));return;}
        PlayerSettings next=switch(a[1].toLowerCase()){
            case "effects"->old.withEffects(EffectMode.valueOf(a[2].toUpperCase()));
            case "hud"->old.withHud(a[2].equalsIgnoreCase("on"));
            case "selector"->old.withSelector(SelectorMode.valueOf(a[2].toUpperCase().replace('-','_')));
            case "slowfall","slow-falling"->old.withSlowFalling(a[2].equalsIgnoreCase("on"));
            default->throw new IllegalArgumentException("Use effects, hud, selector, or slowfall");
        };plugin.players().settings(p.getUniqueId(),next);plugin.dragonborn().apply(p);plugin.messages().send(s,"settings-updated");
    }
    private boolean admin(CommandSender s,String[] a) {
        if(a.length==1) {require(s,"dragonaltar.admin");plugin.openAdmin(player(s)); return true; }
        if(Set.of("ability","cooldown","energy").contains(a[1].toLowerCase()))require(s,"dragonaltar.admin.abilities");else require(s,"dragonaltar.admin.souls");
        if(a[1].equals("refunds")&&a.length>3){Player target=requiredOnline(a[3]);if(a[2].equals("inspect"))s.sendMessage("Pending refund entries: "+plugin.rituals().pendingRefundCount(target.getUniqueId()));else if(a[2].equals("give"))plugin.rituals().refundPending(target);return true;}
        if(a[1].equals("list")) { for(DragonSoul soul:plugin.souls().all()) s.sendMessage(soul.id()+": "+soul.state()+" -> "+soul.holder()); return true; }
        if(a[1].equals("grant")&&a.length>2) {Player target=requiredOnline(a[2]);String id=a.length>3?a[3]:plugin.souls().all().stream().filter(x->x.holder()==null).findFirst().orElseThrow().id();danger(s,"admin-grant",List.of(target.getUniqueId().toString(),id),()->{plugin.souls().assign(id,target.getUniqueId(),"ADMIN_GRANT");plugin.dragonborn().apply(target);});return true;}
        if(a[1].equals("inspect")&&a.length>2){Player target=requiredOnline(a[2]);s.sendMessage(plugin.souls().byHolder(target.getUniqueId()).map(Object::toString).orElse("Not Dragonborn"));return true;}
        if(a[1].equals("remove")&&a.length>2){Player target=requiredOnline(a[2]);danger(s,"admin-remove",List.of(target.getUniqueId().toString()),()->{plugin.souls().removeHolder(target.getUniqueId(),"ADMIN_REMOVE");plugin.dragonborn().remove(target);});return true;}
        if(a[1].equals("transfer")&&a.length>3){Player from=requiredOnline(a[2]),to=requiredOnline(a[3]);DragonSoul soul=plugin.souls().byHolder(from.getUniqueId()).orElseThrow(()->new IllegalArgumentException("Source holds no soul"));danger(s,"admin-transfer",List.of(from.getUniqueId().toString(),to.getUniqueId().toString()),()->{plugin.souls().assign(soul.id(),to.getUniqueId(),"ADMIN_TRANSFER");plugin.dragonborn().remove(from);plugin.dragonborn().apply(to);});return true;}
        if(a[1].equals("transfer-soul")&&a.length>3){Player to=requiredOnline(a[3]);danger(s,"admin-transfer-soul",List.of(a[2],to.getUniqueId().toString()),()->{plugin.souls().assign(a[2],to.getUniqueId(),"ADMIN_TRANSFER");plugin.dragonborn().apply(to);});return true;}
        if(a[1].equals("make-pending")&&a.length>2){danger(s,"admin-make-pending",List.of(a[2]),()->plugin.souls().pending(a[2],"ADMIN_PENDING"));return true;}
        if(a[1].equals("reincarnate")&&a.length>2){List<Player> eligible=plugin.eligibility().eligible(Bukkit.getOnlinePlayers());if(eligible.isEmpty())throw new IllegalStateException("No eligible player");Player to=eligible.get(new Random().nextInt(eligible.size()));danger(s,"admin-reincarnate",List.of(a[2],to.getUniqueId().toString()),()->{plugin.souls().assign(a[2],to.getUniqueId(),"ADMIN_REINCARNATE");plugin.dragonborn().apply(to);});return true;}
        if(a[1].equals("fix-passives")&&a.length>2){plugin.dragonborn().apply(requiredOnline(a[2]));return true;}
        if(a[1].equals("repair")&&a.length>2){Player target=requiredOnline(a[2]);if(plugin.souls().byHolder(target.getUniqueId()).isEmpty())throw new IllegalArgumentException("Player is not Dragonborn");plugin.abilities().clearCache(target);plugin.dragonborn().apply(target);plugin.dragonborn().ensureFocus(target);plugin.abilities().setEnergy(target,plugin.abilities().maxEnergy());s.sendMessage("Repaired Dragonborn state for "+target.getName()+": passives applied, Focus verified, ability cache cleared, energy filled, HUD preference preserved.");return true;}
        if(a[1].equals("ability")&&a.length>4){Player target=requiredOnline(a[3]);if(a[2].equals("select"))plugin.abilities().select(target,a[4]);else if(a[2].equals("cast")){plugin.abilities().select(target,a[4]);plugin.abilities().cast(target);}return true;}
        if(a[1].equals("energy")&&a.length>3) { Player target=requiredOnline(a[3]); if(a[2].equals("fill"))plugin.abilities().setEnergy(target,plugin.abilities().maxEnergy());else if(a[2].equals("view"))s.sendMessage("Energy: "+plugin.abilities().current(target));else if(a[2].equals("set")&&a.length>4)plugin.abilities().setEnergy(target,Integer.parseInt(a[4]));return true; }
        if(a[1].equals("cooldown")&&a.length>3){Player target=requiredOnline(a[3]);if(a[2].equals("view"))s.sendMessage(plugin.abilities().cooldowns(target).toString());else if(a[2].equals("clear"))plugin.abilities().clearCooldowns(target,a.length>4?a[4]:"all");return true;}
        throw new IllegalArgumentException("Unknown admin operation");
    }
    private boolean system(CommandSender s,String[] a) {
        require(s,"dragonaltar.admin.system"); String sub=a.length>1?a[1]:"status";
        if(sub.equals("reload")) { plugin.reloadServices();s.sendMessage("Configuration reloaded."); }
        else if(sub.equals("integrations")) s.sendMessage("Integrations: "+String.join(", ",plugin.integrations()));
        else if(sub.equals("validate")){List<String> errors=plugin.configValidator().validate();s.sendMessage(errors.isEmpty()?"Configuration valid":String.join("; ",errors));}
        else if(sub.equals("tasks"))s.sendMessage("Central runtime tasks: energy/HUD="+(plugin.configService()!=null)+", display="+(plugin.displays()!=null));
        else if(sub.equals("entities")){long tagged=Bukkit.getWorlds().stream().flatMap(w->w.getEntities().stream()).filter(e->e instanceof org.bukkit.entity.Display||e instanceof org.bukkit.entity.EnderCrystal||e instanceof org.bukkit.entity.EnderDragon).count();s.sendMessage("Relevant loaded entities: "+tagged);}
        else if(sub.equals("cleanup")){plugin.dragonEvent().clearTestDragons();Location egg=plugin.configuredLocation("altar.yml","egg-display");if(egg!=null)plugin.displays().recover(egg);s.sendMessage("Duplicate display and test-entity cleanup completed.");}
        else if(sub.equals("version"))s.sendMessage(plugin.getPluginMeta().getName()+" "+plugin.getPluginMeta().getVersion());
        else if(sub.equals("save")){plugin.flushData();s.sendMessage("Persistence queue flushed.");}
        else if(sub.equals("health")){plugin.souls().validate();s.sendMessage("Healthy: config="+plugin.configValidator().validate().isEmpty()+", event="+plugin.dragonEvent().state()+", souls="+plugin.souls().all().size());}
        else s.sendMessage("DragonAltar "+plugin.getPluginMeta().getVersion()+" | event="+plugin.dragonEvent().state()+" | souls="+plugin.souls().all().size());
        return true;
    }
    private boolean dev(CommandSender s,String[] a) {
        require(s,"dragonaltar.developer");
        if(plugin.configService().serverMode()==ServerMode.BETA&&!plugin.getConfig().getBoolean("developer.enabled-in-beta",true))throw new IllegalStateException("Developer commands are disabled by configuration");
        if(plugin.configService().serverMode()==ServerMode.PRODUCTION&&!plugin.configService().destructiveAllowed()) throw new IllegalStateException("Developer mutations are blocked in production");
        if(a.length>2&&a[1].equals("altar")){Location egg=plugin.configuredLocation("altar.yml","egg-display");switch(a[2]){case "displays"->s.sendMessage(plugin.displays().inspect());case "remove-duplicates","repair-displays"->{plugin.displays().recover(egg);s.sendMessage(plugin.displays().inspect());}default->throw new IllegalArgumentException("Unknown altar display diagnostic");}return true;}
        if(a.length>3&&a[1].equals("ritual")){Player target=requiredOnline(a[3]);switch(a[2]){case "recipe-check","recipe-plan"->s.sendMessage(plugin.rituals().plan(target).toString());case "test-elytra"->s.sendMessage(plugin.rituals().offerings().inspectElytra(target));case "give-recipe"->{for(var requirement:plugin.rituals().offerings().requirements())target.getInventory().addItem(new org.bukkit.inventory.ItemStack(requirement.material(),requirement.amount()));}case "recipe-refund"->plugin.rituals().refundPending(target);case "recipe-consume"->danger(s,"dev-recipe-consume",List.of(target.getUniqueId().toString()),()->plugin.rituals().consumeForDiagnostic(target));default->throw new IllegalArgumentException("Unknown ritual diagnostic");}return true;}
        if(a.length>2&&a[1].equals("eligibility")&&(a[2].equals("explain")||a[2].equals("check"))&&a.length>3) {
            var result=plugin.eligibility().check(requiredOnline(a[3])); s.sendMessage("Eligible: "+result.eligible()+" "+result.checks()); return true;
        }
        if(a.length>2&&a[1].equals("eligibility")&&a[2].equals("list")){s.sendMessage("Eligible: "+String.join(", ",plugin.eligibility().eligible(Bukkit.getOnlinePlayers()).stream().map(Player::getName).toList()));return true;}
        if(a.length>2&&a[1].equals("eligibility")&&a[2].equals("choose")){List<Player> pool=plugin.eligibility().eligible(Bukkit.getOnlinePlayers());s.sendMessage(pool.isEmpty()?"No eligible players":"Chosen: "+pool.get(new Random().nextInt(pool.size())).getName());return true;}
        if(a.length>2&&a[1].equals("event")&&a[2].equals("spawn-direct")){Player p=player(s);plugin.dragonEvent().spawnTestDragon(p.getLocation());s.sendMessage("Spawned isolated test dragon; it cannot awaken the altar.");return true;}
        if(a.length>2&&a[1].equals("event")&&a[2].equals("start-force")){Player p=player(s);plugin.dragonEvent().spawnTestDragon(p.getLocation());s.sendMessage("Started isolated direct test dragon; official progression unchanged.");return true;}
        if(a.length>2&&a[1].equals("event")&&a[2].equals("spawn-vanilla")){plugin.dragonEvent().startTestVanilla(plugin.configuredLocation("altar.yml","fountain"),plugin.crystalLocations().values());s.sendMessage("Started isolated vanilla test sequence; resulting dragon cannot awaken the altar.");return true;}
        if(a.length>2&&a[1].equals("event")&&a[2].equals("simulate-spawn")){Player p=player(s);p.getWorld().spawnParticle(Particle.DRAGON_BREATH,p.getLocation(),100,3,2,3,.02);s.sendMessage("Visual-only spawn simulation.");return true;}
        if(a.length>2&&a[1].equals("event")&&a[2].equals("simulate-sed-kill")){Location at=plugin.configuredLocation("altar.yml","altar-center");if(at==null)at=player(s).getLocation();plugin.animations().play("altar-awaken",at,s instanceof Player p?p:null);s.sendMessage("Visual-only ScaledEnderDragon kill simulation.");return true;}
        if(a.length>2&&a[1].equals("event")&&a[2].equals("promote-test-dragon")){Player p=player(s);org.bukkit.entity.EnderDragon dragon=p.getNearbyEntities(64,64,64).stream().filter(org.bukkit.entity.EnderDragon.class::isInstance).map(org.bukkit.entity.EnderDragon.class::cast).filter(plugin.dragonEvent()::isTestDragon).findFirst().orElseThrow(()->new IllegalStateException("No nearby test dragon"));danger(s,"promote-test-dragon",List.of(dragon.getUniqueId().toString()),()->plugin.dragonEvent().promoteTestDragon(dragon));return true;}
        if(a.length>2&&a[1].equals("event")&&a[2].equals("clear-dragons")){danger(s,"clear-test-dragons",List.of(),()->plugin.dragonEvent().clearTestDragons());return true;}
        if(a.length>2&&a[1].equals("event")&&a[2].equals("clear-crystals")){danger(s,"clear-event-crystals",List.of(),()->{plugin.dragonEvent().cleanupCrystals();plugin.dragonEvent().clearTestCrystals();});return true;}
        if(a.length>2&&a[1].equals("event")&&a[2].equals("dump")){s.sendMessage("state="+plugin.dragonEvent().state()+" session="+plugin.dragonEvent().sessionId()+" dragon="+plugin.dragonEvent().dragonId());return true;}
        if(a.length>2&&a[1].equals("event")&&a[2].equals("simulate-death")){Location at=plugin.configuredLocation("altar.yml","altar-center");if(at==null)at=player(s).getLocation();plugin.animations().play("altar-awaken",at,s instanceof Player p?p:null);s.sendMessage("Visual-only death/awakening simulation; ownership unchanged.");return true;}
        if(a.length>2&&a[1].equals("soul")&&a[2].equals("repair")) { int repaired=plugin.souls().repair();s.sendMessage("Soul repair completed; repaired="+repaired);return true; }
        if(a.length>2&&a[1].equals("soul")&&(a[2].equals("list")||a[2].equals("duplicate-check"))){plugin.souls().validate();plugin.souls().all().forEach(x->s.sendMessage(x.id()+" "+x.state()+" holder="+x.holder()));return true;}
        if(a.length>3&&a[1].equals("soul")&&a[2].equals("dump")){s.sendMessage(plugin.souls().byId(a[3]).map(x->x.id()+" "+x.state()+" holder="+x.holder()+" reserved="+x.reservedFor()+" generation="+x.generation()+" transfers="+x.transferCount()+" lineage="+x.lineage()).orElse("Unknown soul"));return true;}
        if(a.length>3&&a[1].equals("soul")&&a[2].equals("create")){plugin.souls().create(a[3]);return true;}
        if(a.length>3&&a[1].equals("soul")&&a[2].equals("delete")){danger(s,"soul-delete",List.of(a[3]),()->plugin.souls().delete(a[3]));return true;}
        if(a.length>4&&a[1].equals("soul")&&a[2].equals("assign")){Player p=requiredOnline(a[4]);danger(s,"soul-assign",List.of(a[3],p.getUniqueId().toString()),()->{plugin.souls().assign(a[3],p.getUniqueId(),"DEV_ASSIGN");plugin.dragonborn().apply(p);});return true;}
        if(a.length>3&&a[1].equals("soul")&&a[2].equals("unassign")){danger(s,"soul-unassign",List.of(a[3]),()->plugin.souls().pending(a[3],"DEV_UNASSIGN"));return true;}
        if(a.length>4&&a[1].equals("soul")&&a[2].equals("setstate")){var state=com.dragonaltar.soul.DragonSoulState.valueOf(a[4].toUpperCase());danger(s,"soul-setstate",List.of(a[3],state.name()),()->plugin.souls().setState(a[3],state));return true;}
        if(a.length>2&&a[1].equals("animation")&&a[2].equals("list")) { s.sendMessage("Animations: "+String.join(", ",plugin.animations().ids())); return true; }
        if(a.length>3&&a[1].equals("animation")&&a[2].equals("play")) { Player p=a.length>4?requiredOnline(a[4]):s instanceof Player player?player:null; Location at=p==null?plugin.configuredLocation("altar.yml","altar-center"):p.getLocation(); if(at==null)throw new IllegalStateException("No animation origin");UUID id=plugin.animations().play(a[3],at,p);if(p!=null)animationSessions.put(p.getUniqueId(),id);return true; }
        if(a.length>2&&a[1].equals("animation")&&a[2].equals("stop")){Player p=player(s);UUID id=animationSessions.remove(p.getUniqueId());if(id!=null)plugin.animations().stop(id);return true;}
        if(a.length>4&&a[1].equals("animation")&&a[2].equals("pvp-transfer")){Player victim=requiredOnline(a[3]),killer=requiredOnline(a[4]);plugin.animations().play("soul-depart",victim.getLocation(),victim);plugin.animations().play("pvp-transfer",killer.getLocation(),killer);return true;}
        if(a.length>4&&a[1].equals("animation")&&a[2].equals("natural-transfer")){Player victim=requiredOnline(a[3]),recipient=requiredOnline(a[4]);plugin.animations().play("soul-depart",victim.getLocation(),victim);plugin.animations().play("natural-transfer",recipient.getLocation(),recipient);return true;}
        if(a.length>2&&a[1].equals("animation")){Map<String,String> aliases=Map.of("altar-awaken","altar-awaken","egg-idle","egg-idle","egg-claim","egg-claim","egg-deplete","egg-deplete","soul-depart","soul-depart","soul-arrive","soul-arrive","ritual-start","ritual-start","ritual-complete","ritual-complete");String animation=aliases.get(a[2]);if(animation!=null){Player target=a.length>3?requiredOnline(a[3]):player(s);plugin.animations().play(animation,target.getLocation(),target);return true;}}
        if(a.length>3&&a[1].equals("input")){Player target=requiredOnline(a[3]);switch(a[2]){case "status"->s.sendMessage("selected="+plugin.abilities().selected(target)+" energy="+plugin.abilities().current(target));case "simulate-scroll"->plugin.abilities().cycle(target,a.length>4&&a[4].equalsIgnoreCase("up")?-1:1);case "simulate-cast"->s.sendMessage(plugin.abilities().cast(target).toString());case "simulate-swap"->plugin.abilities().cycleCategory(target);case "reset"->plugin.abilities().select(target,"wings");default->throw new IllegalArgumentException("Unknown input diagnostic");}return true;}
        if(a.length>2&&a[1].equals("data")&&a[2].equals("backup")) { plugin.backup(); s.sendMessage("Backup created."); return true; }
        if(a.length>3&&a[1].equals("data")&&a[2].equals("restore")){danger(s,"restore-backup",List.of(a[3]),()->plugin.restoreBackup(a[3]));return true;}
        if(a.length>2&&a[1].equals("data")&&a[2].equals("dump")){s.sendMessage("Event="+plugin.dragonEvent().state()+" souls="+plugin.souls().all()+" backups="+plugin.backups());return true;}
        if(a.length>2&&a[1].equals("data")&&a[2].equals("save")){plugin.flushData();s.sendMessage("Persistence queue flushed.");return true;}
        if(a.length>2&&a[1].equals("data")&&a[2].equals("validate")){plugin.souls().validate();s.sendMessage("Persistent soul data valid.");return true;}
        if(a.length>2&&a[1].equals("data")&&a[2].equals("reload")){plugin.reloadData();s.sendMessage("Persistent data reloaded.");return true;}
        if(a.length>3&&a[1].equals("data")&&a[2].equals("dump-player")){Player target=requiredOnline(a[3]);s.sendMessage("settings="+plugin.players().settings(target.getUniqueId())+" soul="+plugin.souls().byHolder(target.getUniqueId())+" energy="+plugin.abilities().current(target));return true;}
        if(a.length>3&&a[1].equals("data")&&a[2].equals("dump-soul")){s.sendMessage(plugin.souls().byId(a[3]).map(Object::toString).orElse("Unknown soul"));return true;}
        if(a.length>2&&a[1].equals("data")&&a[2].equals("clear-cache")){plugin.abilities().clearCaches();return true;}
        if(a.length>2&&a[1].equals("perf")){long displays=Bukkit.getWorlds().stream().flatMap(w->w.getEntities().stream()).filter(e->e instanceof org.bukkit.entity.Display).count();s.sendMessage("online="+Bukkit.getOnlinePlayers().size()+" entities="+Bukkit.getWorlds().stream().mapToInt(w->w.getEntities().size()).sum()+" displays="+displays);return true;}
        if(a.length>2&&a[1].equals("reset")&&a[2].equals("souls")){danger(s,"reset-souls",List.of(),plugin.souls()::reset);return true;}
        if(a.length>2&&a[1].equals("reset")&&a[2].equals("event")){danger(s,"reset-event",List.of(),plugin.dragonEvent()::resetForBeta);return true;}
        if(a.length>2&&a[1].equals("reset")&&a[2].equals("players")){danger(s,"reset-players",List.of(),()->{plugin.players().reset();plugin.abilities().clearCaches();plugin.audit().record("PLAYERS_RESET",senderId(s).toString(),"Player settings, energy, selections, cooldowns, and history cleared");});return true;}
        if(a.length>2&&a[1].equals("reset")&&a[2].equals("altar")){danger(s,"reset-altar",List.of(),plugin::resetAltarConfiguration);return true;}
        if(a.length>2&&a[1].equals("reset")&&a[2].equals("history")){danger(s,"reset-history",List.of(),()->{plugin.souls().clearHistory();plugin.players().clearHistory();});return true;}
        if(a.length>2&&a[1].equals("reset")&&a[2].equals("everything")){danger(s,"reset-everything",List.of(),()->{plugin.souls().reset();plugin.consequences().reset();plugin.players().reset();plugin.abilities().clearCaches();plugin.dragonEvent().resetForBeta();plugin.resetAltarConfiguration();});return true;}
        if(a.length>2&&a[1].equals("confirm")){confirmDanger(s,new String[]{"confirm",a[2]});return true;}
        s.sendMessage("Developer command recognized; see COMMANDS.md for production safety and supported diagnostics."); return true;
    }
    private void status(CommandSender s){String identity="No";if(s instanceof Player p)identity=plugin.souls().byHolder(p.getUniqueId()).map(soul->SoulIdentity.displayName(soul.id())).orElse("No");plugin.messages().send(s,"player-status","event",plugin.dragonEvent().state().name(),"altar",plugin.validateSetup(),"dragonborn",identity);}
    private void history(CommandSender s,String[] a){UUID filter=a.length>1?Bukkit.getOfflinePlayer(a[1]).getUniqueId():(s instanceof Player p?p.getUniqueId():null);int count=0;if(filter!=null){for(String line:plugin.players().history(filter)){String[] parts=line.split("\\|");String soul=parts.length==0?"Soul":SoulIdentity.displayName(parts[parts.length-1]);plugin.messages().send(s,"history-entry","soul",soul,"entry",SoulIdentity.replaceIds(line));count++;}}else for(DragonSoul soul:plugin.souls().all())for(String line:soul.lineage()){plugin.messages().send(s,"history-entry","soul",SoulIdentity.displayName(soul.id()),"entry",SoulIdentity.replaceIds(line));count++;}if(count==0)plugin.messages().send(s,"history-empty");}
    private void help(CommandSender s){if(s instanceof Player p)plugin.helpMenu().open(p);else plugin.messages().send(s,"help");}
    private static Location setupLocation(Player player,String[] args,int coordinateStart) {
        if(args.length==coordinateStart)return player.getLocation();
        int supplied=args.length-coordinateStart;
        if(supplied<3||supplied>5)throw new IllegalArgumentException("Use: <x> <y> <z> [yaw] [pitch]");
        try {
            double x=Double.parseDouble(args[coordinateStart]);
            double y=Double.parseDouble(args[coordinateStart+1]);
            double z=Double.parseDouble(args[coordinateStart+2]);
            float yaw=supplied>=4?Float.parseFloat(args[coordinateStart+3]):player.getLocation().getYaw();
            float pitch=supplied>=5?Float.parseFloat(args[coordinateStart+4]):player.getLocation().getPitch();
            return new Location(player.getWorld(),x,y,z,yaw,pitch);
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException("Coordinates, yaw, and pitch must be numbers");
        }
    }
    private static String formatLocation(Location location) {
        return location.getWorld().getName()+" "+location.getX()+" "+location.getY()+" "+location.getZ();
    }
    private static Player player(CommandSender s){ if(!(s instanceof Player p))throw new IllegalArgumentException("Player only");return p; }
    private static void require(CommandSender s,String permission){if(!s.hasPermission(permission))throw new IllegalArgumentException("No permission");}
    private static Player requiredOnline(String name){ Player p=Bukkit.getPlayerExact(name);if(p==null)throw new IllegalArgumentException("Player is not online");return p; }
    @Override public List<String> onTabComplete(CommandSender s,Command c,String l,String[] a) {
        if(a.length==1)return filter(List.of("status","history","refunds","abilities","focus","settings","help","event","setup","altar","ritual","admin","protection","system","dev"),a[0]);
        if(a.length==2&&a[0].equalsIgnoreCase("event"))return filter(List.of("status","preview","start","confirm-start","abort","recover","rescan","locate","dragon-info"),a[1]);
        if(a.length==2&&a[0].equalsIgnoreCase("setup"))return filter(List.of("begin","status","setaltarcenter","setritualcenter","setegg","setinteraction","setpedestal","removepedestal","pos1","pos2","setfountain","setcrystal","setarrival","validate","preview","save","cancel"),a[1]);
        if(a.length==2&&a[0].equalsIgnoreCase("altar"))return filter(List.of("status","awaken","dormancy","activate","deactivate","preview","validate","teleport","egg","recipe"),a[1]);
        if(a.length==2&&a[0].equalsIgnoreCase("ritual"))return filter(List.of("status","inspect","start","stop","complete","fail","refund","reserve","release"),a[1]);
        if(a.length==2&&a[0].equalsIgnoreCase("admin"))return filter(List.of("list","inspect","refunds","grant","remove","transfer","transfer-soul","reincarnate","make-pending","fix-passives","repair","ability","cooldown","energy"),a[1]);
        if(a.length==2&&a[0].equalsIgnoreCase("protection"))return filter(List.of("status","enable","disable","bypass","setpos1","setpos2","validate","visualize"),a[1]);
        if(a.length==2&&a[0].equalsIgnoreCase("system"))return filter(List.of("status","health","validate","save","reload","integrations","tasks","entities","cleanup","version"),a[1]);
        if(a.length==2&&a[0].equalsIgnoreCase("settings"))return filter(List.of("effects","hud","selector","slowfall"),a[1]);
        if(a.length==2&&a[0].equalsIgnoreCase("dev"))return filter(List.of("event","altar","ritual","animation","soul","eligibility","input","data","perf","reset","confirm"),a[1]);
        if(a.length==3&&a[0].equalsIgnoreCase("dev")&&a[1].equalsIgnoreCase("event"))return filter(List.of("start-force","spawn-direct","spawn-vanilla","simulate-spawn","simulate-death","simulate-sed-kill","promote-test-dragon","clear-crystals","clear-dragons","dump"),a[2]);
        if(a.length==3&&a[0].equalsIgnoreCase("dev")&&a[1].equalsIgnoreCase("animation"))return filter(List.of("list","play","stop","altar-awaken","egg-idle","egg-claim","egg-deplete","soul-depart","soul-arrive","pvp-transfer","natural-transfer","ritual-start","ritual-complete"),a[2]);
        if(a.length==3&&a[0].equalsIgnoreCase("dev")&&a[1].equalsIgnoreCase("soul"))return filter(List.of("list","dump","create","delete","setstate","assign","unassign","duplicate-check","repair"),a[2]);
        if(a.length==3&&a[0].equalsIgnoreCase("dev")&&a[1].equalsIgnoreCase("data"))return filter(List.of("dump","dump-player","dump-soul","save","reload","validate","backup","restore","clear-cache"),a[2]);
        if(a.length==3&&a[0].equalsIgnoreCase("dev")&&a[1].equalsIgnoreCase("eligibility"))return filter(List.of("check","list","choose","explain"),a[2]);
        if(a.length==3&&a[0].equalsIgnoreCase("dev")&&a[1].equalsIgnoreCase("input"))return filter(List.of("status","simulate-scroll","simulate-cast","simulate-swap","reset"),a[2]);
        if(a.length==3&&a[0].equalsIgnoreCase("dev")&&a[1].equalsIgnoreCase("perf"))return filter(List.of("status","particles","tasks","entities"),a[2]);
        if(a.length==3&&a[0].equalsIgnoreCase("dev")&&a[1].equalsIgnoreCase("reset"))return filter(List.of("event","altar","souls","players","history","everything"),a[2]);
        if(a.length==3&&a[0].equalsIgnoreCase("altar")&&a[1].equalsIgnoreCase("egg"))return filter(List.of("spawn","remove","reset","teleport-to-config","inspect","animate","count"),a[2]);
        if(a.length==3&&a[0].equalsIgnoreCase("altar")&&a[1].equalsIgnoreCase("recipe"))return filter(List.of("spawn","remove","refresh","inspect","preview","move"),a[2]);
        if(a.length==3&&a[0].equalsIgnoreCase("dev")&&a[1].equalsIgnoreCase("altar"))return filter(List.of("displays","remove-duplicates","repair-displays"),a[2]);
        if(a.length==3&&a[0].equalsIgnoreCase("dev")&&a[1].equalsIgnoreCase("ritual"))return filter(List.of("recipe-check","recipe-plan","recipe-consume","recipe-refund","give-recipe","test-elytra"),a[2]);
        if(a.length==3&&a[0].equalsIgnoreCase("admin")&&a[1].equalsIgnoreCase("ability"))return filter(List.of("cast","select"),a[2]);
        if(a.length==3&&a[0].equalsIgnoreCase("admin")&&a[1].equalsIgnoreCase("energy"))return filter(List.of("view","set","fill"),a[2]);
        if(a.length==3&&a[0].equalsIgnoreCase("admin")&&a[1].equalsIgnoreCase("cooldown"))return filter(List.of("view","clear"),a[2]);
        if(a.length==3&&a[0].equalsIgnoreCase("settings")&&a[1].equalsIgnoreCase("effects"))return filter(List.of("full","reduced","minimal"),a[2]);
        if(a.length==3&&a[0].equalsIgnoreCase("settings")&&a[1].equalsIgnoreCase("hud"))return filter(List.of("on","off"),a[2]);
        if(a.length==3&&a[0].equalsIgnoreCase("settings")&&a[1].equalsIgnoreCase("slowfall"))return filter(List.of("on","off"),a[2]);
        if(a.length==3&&a[0].equalsIgnoreCase("settings")&&a[1].equalsIgnoreCase("selector"))return filter(List.of("locked","sneak-scroll"),a[2]);
        if(a.length==3&&a[0].equalsIgnoreCase("setup")&&a[1].equalsIgnoreCase("setcrystal"))return filter(List.of("north","south","east","west"),a[2]);
        if(a.length>=3&&Set.of("start","inspect","grant","remove","transfer","fix-passives","repair").contains(a[1].toLowerCase()))return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),a[a.length-1]);
        return List.of();
    }
    private static List<String> filter(List<String> values,String prefix){String normalized=prefix.toLowerCase(Locale.ROOT);return values.stream().filter(x->x.toLowerCase(Locale.ROOT).startsWith(normalized)).toList();}
    private Duration confirmationDuration(){return Duration.ofSeconds(Math.max(1,plugin.getConfig().getLong("event.confirmation-seconds",30)));}
    private void requireForcedStateAllowed(){if(plugin.configService().serverMode()==ServerMode.PRODUCTION&&!plugin.configService().destructiveAllowed())throw new IllegalStateException("Forced state changes are blocked in production");}
    private void danger(CommandSender sender,String operation,List<String> arguments,Runnable action){UUID id=senderId(sender);Duration duration=confirmationDuration();String token=plugin.confirmations().issue(id,operation,arguments,duration);dangerous.put(id,new PendingDanger(operation,List.copyOf(arguments),action));sender.sendMessage("DANGER: destructive operation. Run within "+duration.toSeconds()+" seconds: /dragon confirm "+token);}
    private void confirmDanger(CommandSender sender,String[] args){if(args.length<2)throw new IllegalArgumentException("Confirmation token required");UUID id=senderId(sender);PendingDanger pending=dangerous.remove(id);if(pending==null||!plugin.confirmations().consume(id,args[1],pending.operation,pending.arguments))throw new IllegalArgumentException("Invalid or expired confirmation");pending.action.run();sender.sendMessage("Destructive operation completed.");}
    private static UUID senderId(CommandSender sender){return sender instanceof Player player?player.getUniqueId():UUID.nameUUIDFromBytes(("dragonaltar:"+sender.getName()).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private record PendingDanger(String operation,List<String> arguments,Runnable action){}
}
