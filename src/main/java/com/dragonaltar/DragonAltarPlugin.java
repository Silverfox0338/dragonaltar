package com.dragonaltar;

import com.dragonaltar.ability.AbilityService;
import com.dragonaltar.api.*;
import com.dragonaltar.animation.AnimationService;
import com.dragonaltar.audit.AuditService;
import com.dragonaltar.command.*;
import com.dragonaltar.config.ConfigService;
import com.dragonaltar.config.MessageService;
import com.dragonaltar.config.ConfigValidator;
import com.dragonaltar.dragonborn.DragonbornService;
import com.dragonaltar.dragonevent.DragonEventManager;
import com.dragonaltar.eligibility.EligibilityService;
import com.dragonaltar.listener.GameplayListener;
import com.dragonaltar.altar.ProtectionManager;
import com.dragonaltar.altar.AltarSetupService;
import com.dragonaltar.persistence.YamlDataStore;
import com.dragonaltar.ritual.RitualManager;
import com.dragonaltar.ritual.SoulConsequenceService;
import com.dragonaltar.integration.PlaceholderApiIntegration;
import com.dragonaltar.integration.ScaledEnderDragonIntegration;
import com.dragonaltar.player.PlayerDataService;
import com.dragonaltar.dragonborn.CombatTagService;
import com.dragonaltar.display.DisplayManager;
import com.dragonaltar.gui.AdminGui;
import com.dragonaltar.gui.AbilityMenu;
import com.dragonaltar.gui.RitualMenu;
import com.dragonaltar.gui.SettingsMenu;
import com.dragonaltar.gui.HelpMenu;
import com.dragonaltar.soul.DragonSoulService;
import com.dragonaltar.soul.SoulIdentity;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

public final class DragonAltarPlugin extends JavaPlugin {
    private ConfigService configs; private ConfigValidator validator; private MessageService messages; private YamlDataStore store; private AuditService audit; private DragonSoulService souls;
    private DragonbornService dragonborn; private AbilityService abilities; private AnimationService animations; private EligibilityService eligibility; private PlayerDataService players; private CombatTagService combatTags;
    private ScaledEnderDragonIntegration scaledDragon;
    private DragonEventManager dragonEvent; private RitualManager rituals; private SoulConsequenceService consequences; private com.dragonaltar.ritual.DragonbornRemovalRitual removalRitual; private DisplayManager displays; private AltarSetupService setup; private AdminGui adminGui; private AbilityMenu abilityMenu;private RitualMenu ritualMenu;private SettingsMenu settingsMenu;private HelpMenu helpMenu; private ConfirmationService confirmations; private final Set<UUID> bypass = new HashSet<>();private boolean runtimeTasksStarted;
    @Override public void onEnable() {
        try {
            configs=new ConfigService(this); configs.load();validator=new ConfigValidator(configs);messages=new MessageService(configs); store=new YamlDataStore(this); store.initialize(); audit=new AuditService(this);
            List<String> configErrors=validator.validate();if(!configErrors.isEmpty())throw new IllegalStateException("Configuration validation failed: "+String.join("; ",configErrors));
            souls=new DragonSoulService(store,audit); souls.load(); players=new PlayerDataService(store);players.load();dragonborn=new DragonbornService(this,souls,configs,players);
            abilities=new AbilityService(this,dragonborn,configs,players); eligibility=new EligibilityService(configs,souls);combatTags=new CombatTagService(getConfig().getLong("transfer.combat-tag-seconds",15));
            animations=new AnimationService(this,configs);animations.load();
            dragonEvent=new DragonEventManager(this,store,souls,audit); dragonEvent.load(); confirmations=new ConfirmationService();scaledDragon=new ScaledEnderDragonIntegration();
            consequences=new SoulConsequenceService(this,store); consequences.load();
            rituals=new RitualManager(this,store); rituals.recover();
            removalRitual=new com.dragonaltar.ritual.DragonbornRemovalRitual(this);
            displays=new DisplayManager(this,souls);
            setup=new AltarSetupService(this,Duration.ofMinutes(15));
            adminGui=new AdminGui(this);
            abilityMenu=new AbilityMenu(this,abilities);
            ritualMenu=new RitualMenu(this);
            settingsMenu=new SettingsMenu(this);helpMenu=new HelpMenu();
            DragonCommand command=new DragonCommand(this); Objects.requireNonNull(getCommand("dragon")).setExecutor(command); getCommand("dragon").setTabCompleter(command);
            getServer().getPluginManager().registerEvents(new GameplayListener(this,dragonEvent,souls,dragonborn,abilities,eligibility,combatTags),this);
            getServer().getPluginManager().registerEvents(new ProtectionManager(this),this);
            getServer().getPluginManager().registerEvents(adminGui,this);
            getServer().getPluginManager().registerEvents(abilityMenu,this);
            getServer().getPluginManager().registerEvents(ritualMenu,this);
            getServer().getPluginManager().registerEvents(removalRitual,this);
            getServer().getPluginManager().registerEvents(consequences,this);
            getServer().getPluginManager().registerEvents(settingsMenu,this);
            getServer().getPluginManager().registerEvents(helpMenu,this);
            getServer().getServicesManager().register(DragonAltarApi.class,new DragonAltarApiImpl(this),this,org.bukkit.plugin.ServicePriority.Normal);
            if(getServer().getPluginManager().isPluginEnabled("PlaceholderAPI"))new PlaceholderApiIntegration(this).register();
            if(validateSetup().equals("Valid")||souls.all().stream().anyMatch(s->s.holder()!=null))ensureRuntimeTasks();integrations().forEach(x->getLogger().info(x+" detected."));
            if(integrations().contains("ScaledEnderDragon")) getLogger().warning("DragonAltar uses vanilla respawning so ScaledEnderDragon can scale it. Remove dragon egg rewards from its rewards.yml.");
            if(!validateSetup().equals("Valid"))
                getLogger().warning("DragonAltar is installed but has not been configured. Run /dragon setup begin; the Ancient Dragon Event cannot start until validation passes.");
            Bukkit.getOnlinePlayers().forEach(p->{dragonborn.apply(p);rituals.refundPending(p);});
            Bukkit.getScheduler().runTask(this,this::recoverPendingTransfers);
        } catch(Exception e) { getLogger().log(java.util.logging.Level.SEVERE,"DragonAltar failed to enable",e); getServer().getPluginManager().disablePlugin(this); }
    }
    @Override public void onDisable(){
        if(removalRitual!=null)removalRitual.stop();
        if(rituals!=null)rituals.stop();
        if(animations!=null)animations.stopAll();
        if(abilities!=null)abilities.stop();
        if(displays!=null)displays.stop();
        if(consequences!=null)consequences.stop();
        bypass.clear();
        if(store!=null)store.close();
        if(audit!=null)audit.close();
        runtimeTasksStarted=false;
    }
    public void reloadServices(){
        configs.reload();
        List<String> errors=validator.validate();
        if(!errors.isEmpty()){
            getLogger().severe("Reloaded configuration is unsafe: "+String.join("; ",errors));
            getServer().getPluginManager().disablePlugin(this);
            throw new IllegalStateException("Configuration validation failed; DragonAltar was disabled. Review the console.");
        }
        if(animations!=null)animations.load();
        if(displays!=null)displays.reload();
    }
    public void ensureRuntimeTasks(){if(runtimeTasksStarted)return;abilities.start();displays.start(()->configuredLocation("altar.yml","egg-display"));runtimeTasksStarted=true;}
    public ConfigService configService(){return configs;} public DragonSoulService souls(){return souls;} public DragonbornService dragonborn(){return dragonborn;}
    public MessageService messages(){return messages;}
    public ConfigValidator configValidator(){return validator;}
    public AbilityService abilities(){return abilities;} public EligibilityService eligibility(){return eligibility;} public DragonEventManager dragonEvent(){return dragonEvent;}
    public AnimationService animations(){return animations;}
    public AuditService audit(){return audit;}
    public PlayerDataService players(){return players;} public CombatTagService combatTags(){return combatTags;}
    public ScaledEnderDragonIntegration scaledDragon(){return scaledDragon;}
    public ConfirmationService confirmations(){return confirmations;}
    public RitualManager rituals(){return rituals;}
    public SoulConsequenceService consequences(){return consequences;}
    public DisplayManager displays(){return displays;}
    public AltarSetupService setup(){return setup;}
    public AbilityMenu abilityMenu(){return abilityMenu;}
    public RitualMenu ritualMenu(){return ritualMenu;}
    public SettingsMenu settingsMenu(){return settingsMenu;}
    public HelpMenu helpMenu(){return helpMenu;}
    public Set<String> integrations(){Set<String>s=new LinkedHashSet<>();for(String n:List.of("ScaledEnderDragon","PlaceholderAPI","WorldEdit"))if(getServer().getPluginManager().isPluginEnabled(n))s.add(n);return s;}
    public Location configuredLocation(String file,String path){
        ConfigurationSection s=configs.file(file).getConfigurationSection(path);if(s==null)return null;World w=null;String uuid=s.getString("world-uuid");
        if(uuid!=null)try{w=Bukkit.getWorld(UUID.fromString(uuid));}catch(IllegalArgumentException ignored){}
        if(w==null)w=Bukkit.getWorld(s.getString("world",""));
        if(w==null)return null;return new Location(w,s.getDouble("x"),s.getDouble("y"),s.getDouble("z"),(float)s.getDouble("yaw"),(float)s.getDouble("pitch"));
    }
    public void saveLocation(String file,String path,Location l){
        YamlConfiguration y=configs.file(file);String p=path+".";y.set(p+"world-uuid",l.getWorld().getUID().toString());y.set(p+"world",l.getWorld().getName());y.set(p+"x",l.getX());y.set(p+"y",l.getY());y.set(p+"z",l.getZ());y.set(p+"yaw",l.getYaw());y.set(p+"pitch",l.getPitch());
        try{y.save(new File(getDataFolder(),file));configs.load();audit.record(path.startsWith("protection.")?"PROTECTION_CHANGE":"SETUP_CHANGE","SYSTEM",path);}catch(IOException e){throw new IllegalStateException("Could not save setup",e);}
    }
    public Map<String,Location> crystalLocations(){Map<String,Location>m=new LinkedHashMap<>();for(String d:List.of("north","south","east","west")){Location l=configuredLocation("altar.yml","crystals."+d);if(l!=null)m.put(d,l);}return m;}
    public List<Location> pedestalLocations(){ConfigurationSection section=configs.file("altar.yml").getConfigurationSection("pedestals");if(section==null)return List.of();return section.getKeys(false).stream().map(id->configuredLocation("altar.yml","pedestals."+id)).filter(Objects::nonNull).toList();}
    public boolean protectionConfigured(){return configuredLocation("altar.yml","protection.pos1")!=null&&configuredLocation("altar.yml","protection.pos2")!=null;}
    public boolean protectionEnabled(){return configs.file("altar.yml").getBoolean("internal-protection.enabled",configs.file("altar.yml").getBoolean("protection.enabled",false));}
    public void setAltarValue(String path,Object value){YamlConfiguration y=configs.file("altar.yml");y.set(path,value);try{y.save(new File(getDataFolder(),"altar.yml"));configs.load();audit.record(path.startsWith("protection.")?"PROTECTION_CHANGE":"ALTAR_CONFIG","SYSTEM",path+"="+value);}catch(IOException e){throw new IllegalStateException("Could not save altar configuration",e);}}
    public String validateSetup(){
        List<String> errors=new ArrayList<>();Map<String,Location> required=new LinkedHashMap<>();for(String path:List.of("egg-display","interaction","ritual-center","arrival","fountain")){Location location=configuredLocation("altar.yml",path);if(location==null&&path.equals("ritual-center"))location=configuredLocation("altar.yml","altar-center");if(location==null)errors.add("missing "+path);else required.put(path,location);}
        Location altar=required.get("ritual-center");for(String path:List.of("egg-display","interaction","arrival"))if(altar!=null&&required.containsKey(path)&&!altar.getWorld().equals(required.get(path).getWorld()))errors.add(path+" is not in ritual world");
        Location egg=required.get("egg-display");if(egg!=null&&egg.getBlock().getType().isSolid())errors.add("egg display is inside a solid block");
        Location fountain=required.get("fountain");if(fountain!=null&&fountain.getWorld().getEnvironment()!=World.Environment.THE_END)errors.add("fountain world is not The End");
        if(fountain!=null&&!fountain.getWorld().getName().equals(configs.general().getString("event.end-world","world_the_end")))errors.add("fountain is not in configured event.end-world");
        if(fountain!=null){boolean bedrock=false;for(int x=-6;x<=6&&!bedrock;x++)for(int y=-3;y<=4&&!bedrock;y++)for(int z=-6;z<=6;z++)if(fountain.getBlock().getRelative(x,y,z).getType()==Material.BEDROCK){bedrock=true;break;}if(!bedrock)errors.add("no exit-fountain bedrock found near fountain");}
        Map<String,Location> crystals=crystalLocations();for(String direction:List.of("north","south","east","west"))if(!crystals.containsKey(direction))errors.add("missing "+direction+" crystal");else if(fountain!=null){Location crystal=crystals.get(direction);if(!fountain.getWorld().equals(crystal.getWorld()))errors.add(direction+" crystal is not in fountain world");else{double dx=crystal.getX()-fountain.getX(),dz=crystal.getZ()-fountain.getZ(),distance=Math.hypot(dx,dz);if(distance<2||distance>6||Math.abs(crystal.getY()-fountain.getY())>4)errors.add(direction+" crystal is outside the fountain cardinal ring");boolean cardinal=switch(direction){case "north"->dz<0&&Math.abs(dz)>Math.abs(dx);case "south"->dz>0&&Math.abs(dz)>Math.abs(dx);case "east"->dx>0&&Math.abs(dx)>Math.abs(dz);default->dx<0&&Math.abs(dx)>Math.abs(dz);};if(!cardinal)errors.add(direction+" crystal is not in its named cardinal direction");}}
        if(!configs.file("ritual.yml").getString("offering-mode","INVENTORY_CONSUME").equals("INVENTORY_CONSUME")&&pedestalLocations().isEmpty())errors.add("at least one pedestal is required by the offering mode");
        if(configs.file("ritual.yml").getDouble("ritual-radius",8.0)<=0)errors.add("ritual radius must be positive");
        if(displays!=null&&displays.duplicateCount()>0)errors.add("duplicate plugin-owned display entities");
        return errors.isEmpty()?"Valid":"Invalid: "+String.join(", ",errors);
    }
    public String validationReport(){
        String result=validateSetup();return "DragonAltar Setup Validation\n\nRequired:\n"+(configuredLocation("altar.yml","egg-display")!=null?"✓":"✗")+" Egg display location\n"+(configuredLocation("altar.yml","interaction")!=null?"✓":"✗")+" Interaction location\n"+((configuredLocation("altar.yml","ritual-center")!=null||configuredLocation("altar.yml","altar-center")!=null)?"✓":"✗")+" Ritual center\n"+(configuredLocation("altar.yml","arrival")!=null?"✓":"✗")+" Arrival location\n"+(configuredLocation("altar.yml","fountain")!=null?"✓":"✗")+" End fountain\n"+(crystalLocations().size()==4?"✓":"✗")+" Crystal positions\n\nOptional:\n- Schematic system: "+(configs.file("altar.yml").getBoolean("schematic.enabled",false)?"Enabled":"Disabled")+"\n- Internal protection: "+(protectionEnabled()?"Enabled":"Disabled")+"\n\nResult: "+(result.equals("Valid")?"READY":result);
    }
    public String setupStatus(){return validateSetup();}
    public void toggleBypass(Player p){if(!bypass.add(p.getUniqueId()))bypass.remove(p.getUniqueId());messages.send(p,"protection-bypass","enabled",Boolean.toString(bypass.contains(p.getUniqueId())));}
    public boolean hasProtectionBypass(Player p){return bypass.contains(p.getUniqueId());}
    public void openAdmin(Player p){adminGui.open(p);}
    public void backup(){store.flush();try{Path dir=getDataFolder().toPath().resolve("backups").resolve(Instant.now().toString().replace(':','-'));Files.createDirectories(dir);try(var stream=Files.list(getDataFolder().toPath().resolve("data"))){for(Path p:stream.toList())Files.copy(p,dir.resolve(p.getFileName()),StandardCopyOption.REPLACE_EXISTING);}audit.record("BACKUP_CREATE","SYSTEM",dir.getFileName().toString());}catch(IOException e){throw new IllegalStateException("Backup failed",e);}}
    public List<String> backups(){Path root=getDataFolder().toPath().resolve("backups");try(var stream=Files.list(root)){return stream.filter(Files::isDirectory).map(p->p.getFileName().toString()).sorted().toList();}catch(IOException e){return List.of();}}
    public void restoreBackup(String name){
        Path root=getDataFolder().toPath().resolve("backups").toAbsolutePath().normalize(),source=root.resolve(name).normalize();
        if(!source.getParent().equals(root)||!Files.isDirectory(source))throw new IllegalArgumentException("Unknown backup");
        store.flush();try{for(String file:List.of("event.yml","altar-state.yml","souls.yml","players.yml","rituals.yml","cooldowns.yml","pending-transfers.yml","consequences.yml")){Path from=source.resolve(file);if(Files.isRegularFile(from))Files.copy(from,getDataFolder().toPath().resolve("data").resolve(file),StandardCopyOption.REPLACE_EXISTING);}}
        catch(IOException e){throw new IllegalStateException("Backup restore failed",e);}
        audit.record("BACKUP_RESTORE","ADMIN",name);souls.load();players.load();abilities.clearCaches();dragonEvent.load();consequences.load();rituals.recover();Bukkit.getOnlinePlayers().forEach(p->{dragonborn.apply(p);rituals.refundPending(p);});Bukkit.getScheduler().runTask(this,this::recoverPendingTransfers);
    }
    public void flushData(){store.flush();}
    public void reloadData(){store.flush();souls.load();players.load();abilities.clearCaches();dragonEvent.load();consequences.load();rituals.recover();Bukkit.getOnlinePlayers().forEach(p->{dragonborn.apply(p);rituals.refundPending(p);});Bukkit.getScheduler().runTask(this,this::recoverPendingTransfers);}
    private void recoverPendingTransfers(){
        List<Player> candidates=new ArrayList<>(eligibility.eligible(Bukkit.getOnlinePlayers()));Collections.shuffle(candidates);
        for(com.dragonaltar.soul.DragonSoul soul:souls.all()){
            if(soul.state()!=com.dragonaltar.soul.DragonSoulState.TRANSFER_PENDING)continue;
            Player recipient=candidates.stream().filter(p->eligibility.check(p).eligible()).findFirst().orElse(null);
            if(recipient==null){Bukkit.broadcast(messages.component("no-eligible-recipient"));break;}
            souls.assign(soul.id(),recipient.getUniqueId(),"STARTUP_PENDING_RECOVERY");dragonborn.apply(recipient);
            animations.play("soul-arrive",recipient.getLocation(),recipient);animations.play("natural-transfer",recipient.getLocation(),recipient);
            Bukkit.broadcast(messages.component("reincarnation","player",recipient.getName(),"soul",SoulIdentity.displayName(soul.id())));
        }
    }
    public void resetAltarConfiguration(){YamlConfiguration y=configs.file("altar.yml");for(String path:List.of("world","altar-center","egg-display","interaction","arrival","fountain","crystals","pedestals","protection.pos1","protection.pos2"))y.set(path,null);try{y.save(new File(getDataFolder(),"altar.yml"));configs.load();dragonEvent.setAltarState(com.dragonaltar.altar.AltarState.UNCONFIGURED);displays.remove();audit.record("ALTAR_RESET","ADMIN","Altar locations cleared");}catch(IOException e){throw new IllegalStateException("Could not reset altar",e);}}
}
