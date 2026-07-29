package com.dragonaltar.api;

import com.dragonaltar.DragonAltarPlugin;
import com.dragonaltar.ability.AbilityCategory;
import com.dragonaltar.ability.AbilityContext;
import com.dragonaltar.ability.AbilityResult;
import com.dragonaltar.ability.DragonAbility;
import com.dragonaltar.api.addon.DragonAddonAbility;
import com.dragonaltar.api.addon.DragonAltarAddon;
import com.dragonaltar.api.model.DragonAbilityInfo;
import com.dragonaltar.api.model.DragonActionResult;
import com.dragonaltar.api.model.DragonEligibilityInfo;
import com.dragonaltar.api.model.DragonEventInfo;
import com.dragonaltar.api.model.DragonRitualInfo;
import com.dragonaltar.api.model.DragonSoulInfo;
import com.dragonaltar.eligibility.EligibilityService;
import com.dragonaltar.ritual.RitualSession;
import com.dragonaltar.soul.DragonSoul;
import com.dragonaltar.soul.DragonSoulState;
import com.dragonaltar.soul.SoulHistoryEntry;
import com.dragonaltar.soul.SoulIdentity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class DragonAltarApiImpl implements DragonAltarApi, Listener {
    private static final String API_VERSION = "2.0";
    private static final Pattern ADDON_ID = Pattern.compile("[a-z0-9][a-z0-9._-]{1,31}");
    private static final Pattern ABILITY_PART = Pattern.compile("[a-z0-9][a-z0-9._-]{1,47}");
    private static final long MAX_COOLDOWN = Duration.ofHours(24).toMillis();

    private final DragonAltarPlugin plugin;
    private final Map<Plugin, Registration> registrations = new IdentityHashMap<>();

    public DragonAltarApiImpl(DragonAltarPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String apiVersion() {
        return API_VERSION;
    }

    @Override
    public DragonEventInfo event() {
        return new DragonEventInfo(
                plugin.dragonEvent().state().name(),
                plugin.dragonEvent().altarState().name(),
                plugin.dragonEvent().sessionId(),
                plugin.dragonEvent().dragonId());
    }

    @Override
    public Optional<DragonRitualInfo> activeRitual() {
        return plugin.rituals().active().map(this::ritualInfo);
    }

    @Override
    public Collection<DragonSoulInfo> souls() {
        return plugin.souls().all().stream().map(this::soulInfo).toList();
    }

    @Override
    public Optional<DragonSoulInfo> soulInfo(String idOrName) {
        return resolveSoul(idOrName).map(this::soulInfo);
    }

    @Override
    public Optional<DragonSoulInfo> soulInfoOf(UUID player) {
        Objects.requireNonNull(player, "player");
        if (privatePlayer(player)) return Optional.empty();
        return plugin.souls().byHolder(player).filter(value -> !privateCustody(value)).map(this::soulInfo);
    }

    @Override
    public DragonEligibilityInfo eligibilityInfo(Player player) {
        EligibilityService.Result result = plugin.eligibility().check(Objects.requireNonNull(player, "player"));
        return new DragonEligibilityInfo(result.eligible(), result.checks());
    }

    @Override
    public Collection<String> availableAbilityIds(Player player) {
        Objects.requireNonNull(player, "player");
        if (privatePlayer(player.getUniqueId())) return List.of();
        return plugin.abilities().abilities(player).stream().map(DragonAbility::id).toList();
    }

    @Override
    public Optional<String> selectedAbility(Player player) {
        Objects.requireNonNull(player, "player");
        if (privatePlayer(player.getUniqueId())
                || !plugin.dragonborn().isDragonborn(player.getUniqueId())) return Optional.empty();
        return Optional.of(plugin.abilities().selected(player));
    }

    @Override
    public long cooldownSeconds(Player player, String abilityId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(abilityId, "abilityId");
        if (privatePlayer(player.getUniqueId())) return 0;
        return plugin.abilities().cooldownSeconds(player, abilityId);
    }

    @Override
    public DragonActionResult cast(Player player) {
        requireMainThread();
        if (privatePlayer(player.getUniqueId()))
            return DragonActionResult.failure("Private staff custody is unavailable to add-ons");
        AbilityResult result = plugin.abilities().cast(Objects.requireNonNull(player, "player"));
        return new DragonActionResult(result.success(), result.message());
    }

    @Override
    public boolean openSoulHistory(Player player) {
        requireMainThread();
        Objects.requireNonNull(player, "player");
        if (!player.isOnline() || !player.hasPermission("dragonaltar.use")) return false;
        plugin.soulHistoryMenu().open(player);
        return true;
    }

    @Override
    public void registerAddon(Plugin owner, DragonAltarAddon addon) {
        requireMainThread();
        requireLivePlugin(owner);
        Objects.requireNonNull(addon, "addon");
        String id = addon.id().toLowerCase(Locale.ROOT);
        if (!ADDON_ID.matcher(id).matches())
            throw new IllegalArgumentException("Add-on id must be 2-32 lowercase letters, numbers, dots, dashes, or underscores");
        if (id.equals("dragonaltar"))
            throw new IllegalArgumentException("The dragonaltar namespace is reserved");
        if (!id.equals(addon.id()))
            throw new IllegalArgumentException("Add-on id must already be lowercase");
        if (registrations.containsKey(owner))
            throw new IllegalStateException(owner.getName() + " is already registered");
        if (registrations.values().stream().anyMatch(value -> value.addon.id().equals(id)))
            throw new IllegalStateException("Add-on id is already registered: " + id);
        registrations.put(owner, new Registration(addon));
        plugin.getLogger().info("Registered DragonAltar add-on " + addon.name() + " " + addon.version()
                + " from " + owner.getName() + ".");
    }

    @Override
    public void registerAbility(Plugin owner, DragonAddonAbility ability) {
        requireMainThread();
        requireLivePlugin(owner);
        Objects.requireNonNull(ability, "ability");
        Registration registration = registrations.get(owner);
        if (registration == null)
            throw new IllegalStateException("Register the add-on before registering abilities");
        validateAbility(registration.addon, ability);
        DragonAbility bridge = bridge(ability);
        plugin.abilities().registerExternal(bridge);
        registration.abilityIds.add(ability.id());
    }

    @Override
    public boolean unregisterAddon(Plugin owner) {
        requireMainThread();
        if (owner == null) return false;
        Registration removed = registrations.remove(owner);
        if (removed == null) return false;
        plugin.abilities().unregisterExternal(removed.abilityIds);
        return true;
    }

    @Override
    public Collection<DragonAltarAddon> addons() {
        return registrations.values().stream().map(value -> value.addon).toList();
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        unregisterAddon(event.getPlugin());
    }

    @Override
    @Deprecated
    public com.dragonaltar.dragonevent.DragonEventState eventState() {
        return plugin.dragonEvent().state();
    }

    @Override
    public String altarState() {
        return plugin.dragonEvent().altarState().name();
    }

    @Override
    public Collection<UUID> dragonborn() {
        return plugin.souls().all().stream()
                .map(DragonSoul::holder)
                .filter(Objects::nonNull)
                .filter(id -> !privatePlayer(id))
                .toList();
    }

    @Override
    @Deprecated
    public Optional<DragonSoul> soul(String id) {
        return Optional.empty();
    }

    @Override
    @Deprecated
    public Optional<DragonSoul> soulOf(UUID player) {
        return Optional.empty();
    }

    @Override
    @Deprecated
    public EligibilityService.Result eligibility(Player player) {
        return plugin.eligibility().check(player);
    }

    @Override
    public Collection<String> abilityIds() {
        return plugin.abilities().abilities().stream().map(DragonAbility::id).toList();
    }

    @Override
    public Optional<DragonAbilityInfo> ability(String id) {
        return plugin.abilities().abilities().stream()
                .filter(value -> value.id().equalsIgnoreCase(id))
                .findFirst()
                .map(value -> new DragonAbilityInfo(
                        value.id(),
                        PlainTextComponentSerializer.plainText().serialize(value.displayName()),
                        value.category().name(),
                        value.energyCost(),
                        value.cooldownMillis()));
    }

    @Override
    public int energy(Player player) {
        return !privatePlayer(player.getUniqueId())
                && plugin.dragonborn().isDragonborn(player.getUniqueId()) ? plugin.abilities().current(player) : 0;
    }

    @Override
    public int maximumEnergy() {
        return plugin.abilities().maxEnergy();
    }

    @Override
    public boolean selectAbility(Player player, String abilityId) {
        requireMainThread();
        if (privatePlayer(player.getUniqueId())) return false;
        if (!player.hasPermission("dragonaltar.use")
                || ability(abilityId).isEmpty()
                || !plugin.dragonborn().isDragonborn(player.getUniqueId())) return false;
        plugin.abilities().select(player, abilityId);
        return plugin.abilities().selected(player).equalsIgnoreCase(abilityId);
    }

    @Override
    @Deprecated
    public AbilityResult castSelectedAbility(Player player) {
        return plugin.abilities().cast(player);
    }

    private DragonRitualInfo ritualInfo(RitualSession session) {
        return new DragonRitualInfo(
                session.playerId(),
                session.soulId(),
                SoulIdentity.displayName(session.soulId()),
                session.phase().name(),
                session.sessionId());
    }

    private Optional<DragonSoul> resolveSoul(String idOrName) {
        if (idOrName == null || idOrName.isBlank()) return Optional.empty();
        Optional<DragonSoul> byId = plugin.souls().byId(idOrName);
        if (byId.isPresent()) return byId;
        return plugin.souls().all().stream()
                .filter(soul -> SoulIdentity.displayName(soul.id()).equalsIgnoreCase(idOrName))
                .findFirst();
    }

    private DragonSoulInfo soulInfo(DragonSoul soul) {
        boolean privateCustody = privateCustody(soul);
        UUID holder = privateCustody ? null : soul.holder();
        String status = privateCustody ? "DORMANT" : publicStatus(soul.state());
        return new DragonSoulInfo(
                soul.id(),
                SoulIdentity.displayName(soul.id()),
                status,
                holder,
                soul.createdAt(),
                status.equals("LIMBO") ? plugin.consequences().limboReleaseAt(soul.id()).orElse(null) : null);
    }

    private boolean privateCustody(DragonSoul soul) {
        return soul.state() == DragonSoulState.ADMIN_HELD
                || soul.holder() != null && (isAdministrator(soul.holder())
                || soul.lineage().stream()
                .map(SoulHistoryEntry::parse)
                .filter(Objects::nonNull)
                .anyMatch(entry -> entry.privatePlayers().contains(soul.holder())));
    }

    private boolean privatePlayer(UUID id) {
        if (isAdministrator(id)) return true;
        return plugin.souls().byHolder(id).map(this::privateCustody).orElse(false);
    }

    private boolean isAdministrator(UUID id) {
        if (id == null) return false;
        Player online = Bukkit.getPlayer(id);
        if (online != null && (online.hasPermission("dragonaltar.admin")
                || online.hasPermission("dragonaltar.admin.souls")
                || online.hasPermission("dragonaltar.developer"))) return true;
        return Bukkit.getOfflinePlayer(id).isOp();
    }

    private static String publicStatus(DragonSoulState state) {
        return switch (state) {
            case HELD -> "HELD";
            case MOTHER_SOUL_LIMBO -> "LIMBO";
            case FRACTURED -> "FRACTURED";
            default -> "DORMANT";
        };
    }

    private void validateAbility(DragonAltarAddon addon, DragonAddonAbility ability) {
        if (ability.id() == null || !ability.id().startsWith(addon.id() + ":"))
            throw new IllegalArgumentException("Ability id must use the " + addon.id() + ": namespace");
        String localId = ability.id().substring(addon.id().length() + 1);
        if (!ABILITY_PART.matcher(localId).matches())
            throw new IllegalArgumentException("Ability name must be 2-48 lowercase letters, numbers, dots, dashes, or underscores");
        if (ability.displayName() == null || ability.displayName().isBlank())
            throw new IllegalArgumentException("Ability display name cannot be blank");
        Objects.requireNonNull(ability.category(), "ability category");
        if (ability.energyCost() < 0 || ability.energyCost() > plugin.abilities().maxEnergy())
            throw new IllegalArgumentException("Ability energy cost must be between 0 and maximum Dragon Energy");
        if (ability.cooldownMillis() < 0 || ability.cooldownMillis() > MAX_COOLDOWN)
            throw new IllegalArgumentException("Ability cooldown must be between 0 and 24 hours");
        Set<String> supported = Objects.requireNonNull(ability.supportedSouls(), "supported souls");
        if (supported.isEmpty()) throw new IllegalArgumentException("Ability must support at least one soul");
        supported.forEach(this::identity);
    }

    private DragonAbility bridge(DragonAddonAbility addonAbility) {
        Set<SoulIdentity> souls = addonAbility.supportedSouls().stream()
                .map(this::identity)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new DragonAbility() {
            @Override public String id() { return addonAbility.id(); }
            @Override public Component displayName() { return Component.text(addonAbility.displayName()); }
            @Override public AbilityCategory category() { return AbilityCategory.valueOf(addonAbility.category().name()); }
            @Override public int energyCost() { return addonAbility.energyCost(); }
            @Override public long cooldownMillis() { return addonAbility.cooldownMillis(); }
            @Override public boolean ultimate() { return addonAbility.ultimate(); }
            @Override public Set<SoulIdentity> souls() { return souls; }

            @Override
            public AbilityResult canUse(AbilityContext context) {
                return invoke(addonAbility, context, false);
            }

            @Override
            public AbilityResult activate(AbilityContext context) {
                return invoke(addonAbility, context, true);
            }
        };
    }

    private AbilityResult invoke(DragonAddonAbility ability, AbilityContext context, boolean activate) {
        try {
            DragonAddonAbility.Context publicContext = new DragonAddonAbility.Context(context.player(), this);
            DragonActionResult result = activate ? ability.activate(publicContext) : ability.canUse(publicContext);
            if (result == null) return AbilityResult.fail("Add-on ability returned no result");
            return new AbilityResult(result.success(), result.message());
        } catch (RuntimeException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Add-on ability " + ability.id() + " failed", exception);
            return AbilityResult.fail("Add-on ability failed");
        }
    }

    private SoulIdentity identity(String value) {
        for (SoulIdentity identity : SoulIdentity.values())
            if (identity.id().equalsIgnoreCase(value) || identity.displayName().equalsIgnoreCase(value))
                return identity;
        throw new IllegalArgumentException("Unknown soul: " + value);
    }

    private static void requireLivePlugin(Plugin owner) {
        Objects.requireNonNull(owner, "owner");
        if (!owner.isEnabled()) throw new IllegalStateException(owner.getName() + " is not enabled");
    }

    private static void requireMainThread() {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("DragonAltar add-ons must be registered on the server thread");
    }

    private static final class Registration {
        private final DragonAltarAddon addon;
        private final Set<String> abilityIds = new LinkedHashSet<>();

        private Registration(DragonAltarAddon addon) {
            this.addon = addon;
        }
    }
}
