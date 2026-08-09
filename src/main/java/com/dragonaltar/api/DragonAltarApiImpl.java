package com.dragonaltar.api;

import com.dragonaltar.DragonAltarPlugin;
import com.dragonaltar.ability.AbilityCategory;
import com.dragonaltar.ability.AbilityContext;
import com.dragonaltar.ability.AbilityResult;
import com.dragonaltar.ability.DragonAbility;
import com.dragonaltar.api.addon.DragonAddonAbility;
import com.dragonaltar.api.addon.DragonAddonItem;
import com.dragonaltar.api.addon.DragonAltarAddon;
import com.dragonaltar.api.event.DragonAddonItemEquipEvent;
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
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
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
    private static final String API_VERSION = "2.1";
    private static final Pattern ADDON_ID = Pattern.compile("[a-z0-9][a-z0-9._-]{1,31}");
    private static final Pattern ABILITY_PART = Pattern.compile("[a-z0-9][a-z0-9._-]{1,47}");
    private static final Pattern ITEM_PART = Pattern.compile("[a-z0-9][a-z0-9._-]{1,47}");
    private static final long MAX_COOLDOWN = Duration.ofHours(24).toMillis();

    private final DragonAltarPlugin plugin;
    private final Map<Plugin, Registration> registrations = new IdentityHashMap<>();
    private final Map<String, RegisteredItem> items = new java.util.LinkedHashMap<>();
    private final NamespacedKey soulBoundItemKey;

    public DragonAltarApiImpl(DragonAltarPlugin plugin) {
        this.plugin = plugin;
        this.soulBoundItemKey = new NamespacedKey(plugin, "soul_bound_item");
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
    public void registerItem(Plugin owner, DragonAddonItem item) {
        requireMainThread();
        requireLivePlugin(owner);
        Objects.requireNonNull(item, "item");
        Registration registration = registrations.get(owner);
        if (registration == null)
            throw new IllegalStateException("Register the add-on before registering items");
        String id = validateItem(registration.addon, item);
        if (items.containsKey(id)) throw new IllegalStateException("Item id is already registered: " + id);
        SoulIdentity soul = identity(item.soulId());
        RegisteredItem registered = new RegisteredItem(id, item.displayName().trim(), soul, item);
        items.put(id, registered);
        registration.itemIds.add(id);
    }

    @Override
    public void tagSoulBound(ItemStack item, String itemId) {
        requireMainThread();
        Objects.requireNonNull(item, "item");
        if (item.getType().isAir()) throw new IllegalArgumentException("Cannot tag an empty item stack");
        String id = Objects.requireNonNull(itemId, "itemId");
        if (!items.containsKey(id)) throw new IllegalArgumentException("Unknown registered item: " + id);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) throw new IllegalArgumentException("Item stack does not support metadata");
        meta.getPersistentDataContainer().set(soulBoundItemKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
    }

    @Override
    public boolean isSoulBound(ItemStack item) {
        return soulBoundItemId(item).isPresent();
    }

    @Override
    public Optional<String> soulBoundItemId(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return Optional.empty();
        String id = item.getItemMeta().getPersistentDataContainer()
                .get(soulBoundItemKey, PersistentDataType.STRING);
        return validNamespacedId(id) ? Optional.of(id) : Optional.empty();
    }

    @Override
    public Collection<String> itemIds() {
        return List.copyOf(items.keySet());
    }

    @Override
    public boolean unregisterAddon(Plugin owner) {
        requireMainThread();
        if (owner == null) return false;
        Registration removed = registrations.remove(owner);
        if (removed == null) return false;
        plugin.abilities().unregisterExternal(removed.abilityIds);
        removed.itemIds.forEach(items::remove);
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeldItem(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        Denial denial = preflight(player, EquipmentSlot.HAND,
                player.getInventory().getItem(event.getNewSlot()));
        if (denial != null) {
            event.setCancelled(true);
            deny(player, denial);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Denial denial = preflight(event.getPlayer(), EquipmentSlot.HAND, event.getMainHandItem());
        if (denial == null)
            denial = preflight(event.getPlayer(), EquipmentSlot.OFF_HAND, event.getOffHandItem());
        if (denial != null) {
            event.setCancelled(true);
            deny(event.getPlayer(), denial);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        List<EquipCandidate> candidates = clickCandidates(event, player);
        for (EquipCandidate candidate : candidates) {
            Denial denial = preflight(player, candidate.slot(), candidate.item());
            if (denial == null) continue;
            event.setCancelled(true);
            deny(player, denial);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
            Inventory inventory = event.getView().getInventory(entry.getKey());
            if (inventory != player.getInventory()) continue;
            EquipmentSlot slot = equipmentSlot(player, event.getView().convertSlot(entry.getKey()));
            if (slot == null) continue;
            Denial denial = preflight(player, slot, entry.getValue());
            if (denial == null) continue;
            event.setCancelled(true);
            deny(player, denial);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getHand() == null || !event.getAction().isRightClick()) return;
        EquipmentSlot slot = wearableSlot(event.getItem());
        if (slot == null || !slot.isArmor()) return;
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
                && event.getClickedBlock() != null && event.getClickedBlock().getType().isInteractable()
                && !event.getPlayer().isSneaking()) return;
        Denial denial = preflight(event.getPlayer(), slot, event.getItem());
        if (denial != null) {
            event.setCancelled(true);
            deny(event.getPlayer(), denial);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDispenseArmor(BlockDispenseArmorEvent event) {
        if (!(event.getTargetEntity() instanceof Player player)) return;
        EquipmentSlot slot = wearableSlot(event.getItem());
        if (slot == null || !slot.isArmor()) return;
        Denial denial = preflight(player, slot, event.getItem());
        if (denial != null) {
            event.setCancelled(true);
            deny(player, denial);
        }
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

    private String validateItem(DragonAltarAddon addon, DragonAddonItem item) {
        String id = item.id();
        if (id == null || !id.startsWith(addon.id() + ":"))
            throw new IllegalArgumentException("Item id must use the " + addon.id() + ": namespace");
        String localId = id.substring(addon.id().length() + 1);
        if (!ITEM_PART.matcher(localId).matches())
            throw new IllegalArgumentException("Item name must be 2-48 lowercase letters, numbers, dots, dashes, or underscores");
        if (item.displayName() == null || item.displayName().isBlank())
            throw new IllegalArgumentException("Item display name cannot be blank");
        identity(item.soulId());
        return id;
    }

    private Denial preflight(Player player, EquipmentSlot slot, ItemStack stack) {
        Optional<String> taggedId = soulBoundItemId(stack);
        if (taggedId.isEmpty()) return null;
        RegisteredItem registered = items.get(taggedId.get());
        if (registered == null) return null;
        boolean holdsSoul = soulInfoOf(player.getUniqueId())
                .map(soul -> soul.id().equals(registered.soul().id()))
                .orElse(false);
        if (!holdsSoul) return new Denial(registered, "");
        DragonActionResult result;
        try {
            result = registered.item().canEquip(
                    new DragonAddonItem.Context(player, slot, stack, this));
        } catch (RuntimeException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Add-on item " + registered.id() + " equip check failed", exception);
            return new Denial(registered, "");
        }
        if (result == null) {
            plugin.getLogger().warning("Add-on item " + registered.id() + " returned no equip result");
            return new Denial(registered, "");
        }
        if (!result.success()) return new Denial(registered, result.message());
        DragonAddonItemEquipEvent event = new DragonAddonItemEquipEvent(
                player, registered.id(), registered.soul().id(), slot, stack);
        Bukkit.getPluginManager().callEvent(event);
        return event.isCancelled() ? new Denial(registered, "") : null;
    }

    private void deny(Player player, Denial denial) {
        if (!denial.message().isBlank()) {
            player.sendMessage(denial.message());
            return;
        }
        plugin.messages().send(player, "soul-bound-equip-denied",
                "item", denial.item().displayName(),
                "soul", denial.item().soul().displayName());
    }

    private List<EquipCandidate> clickCandidates(InventoryClickEvent event, Player player) {
        List<EquipCandidate> candidates = new ArrayList<>();
        Inventory clicked = event.getClickedInventory();
        EquipmentSlot destination = clicked == player.getInventory()
                ? equipmentSlot(player, event.getSlot()) : null;
        ItemStack incoming = switch (event.getAction()) {
            case PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR -> event.getCursor();
            case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> hotbarSource(event, player);
            default -> null;
        };
        if (destination != null && incoming != null) candidates.add(new EquipCandidate(destination, incoming));

        if ((event.getAction() == InventoryAction.HOTBAR_SWAP
                || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD)) {
            if (event.getClick() == ClickType.SWAP_OFFHAND) {
                candidates.add(new EquipCandidate(EquipmentSlot.OFF_HAND, event.getCurrentItem()));
            } else if (event.getHotbarButton() == player.getInventory().getHeldItemSlot()) {
                candidates.add(new EquipCandidate(EquipmentSlot.HAND, event.getCurrentItem()));
            }
        }

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && event.getCurrentItem() != null
                && clicked != null && destination == null) {
            EquipmentSlot automatic = wearableSlot(event.getCurrentItem());
            if (automatic != null && automatic.isArmor()
                    && isEmpty(player.getInventory().getItem(automatic)))
                candidates.add(new EquipCandidate(automatic, event.getCurrentItem()));
        }
        return candidates;
    }

    private static ItemStack hotbarSource(InventoryClickEvent event, Player player) {
        if (event.getClick() == ClickType.SWAP_OFFHAND) return player.getInventory().getItemInOffHand();
        int button = event.getHotbarButton();
        return button >= 0 && button <= 8 ? player.getInventory().getItem(button) : null;
    }

    private static EquipmentSlot equipmentSlot(Player player, int inventorySlot) {
        if (inventorySlot == player.getInventory().getHeldItemSlot()) return EquipmentSlot.HAND;
        return switch (inventorySlot) {
            case 36 -> EquipmentSlot.FEET;
            case 37 -> EquipmentSlot.LEGS;
            case 38 -> EquipmentSlot.CHEST;
            case 39 -> EquipmentSlot.HEAD;
            case 40 -> EquipmentSlot.OFF_HAND;
            default -> null;
        };
    }

    private static EquipmentSlot wearableSlot(ItemStack item) {
        if (isEmpty(item)) return null;
        EquipmentSlot slot = item.getType().getEquipmentSlot();
        return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET ? slot : null;
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    static boolean validNamespacedId(String id) {
        if (id == null) return false;
        int colon = id.indexOf(':');
        return colon > 0 && colon == id.lastIndexOf(':')
                && ADDON_ID.matcher(id.substring(0, colon)).matches()
                && ITEM_PART.matcher(id.substring(colon + 1)).matches();
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
        private final Set<String> itemIds = new LinkedHashSet<>();

        private Registration(DragonAltarAddon addon) {
            this.addon = addon;
        }
    }

    private record RegisteredItem(String id, String displayName, SoulIdentity soul, DragonAddonItem item) {}
    private record EquipCandidate(EquipmentSlot slot, ItemStack item) {}
    private record Denial(RegisteredItem item, String message) {}
}
