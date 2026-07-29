package com.dragonaltar.gui;

import com.dragonaltar.DragonAltarPlugin;
import com.dragonaltar.soul.DragonSoul;
import com.dragonaltar.soul.DragonSoulState;
import com.dragonaltar.soul.SoulHistoryEntry;
import com.dragonaltar.soul.SoulIdentity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class SoulHistoryMenu implements Listener {
    private static final int PAGE_SIZE = 45;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
            .withLocale(Locale.US).withZone(ZoneId.systemDefault());
    private final DragonAltarPlugin plugin;

    public SoulHistoryMenu(DragonAltarPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        MenuHolder holder = new MenuHolder(Screen.OVERVIEW, null, 0);
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Component.text("Dragon Soul History", NamedTextColor.DARK_PURPLE));
        holder.inventory = inventory;
        fill(inventory, Material.BLACK_STAINED_GLASS_PANE);
        int[] slots = {11, 13, 15};
        List<DragonSoul> souls = plugin.souls().all();
        for (int i = 0; i < souls.size() && i < slots.length; i++)
            inventory.setItem(slots[i], soulItem(souls.get(i)));
        inventory.setItem(4, item(Material.DRAGON_EGG, "Soul Archive", NamedTextColor.LIGHT_PURPLE, List.of(
                line("Every Dragon Soul remembers its journey.", NamedTextColor.GRAY),
                line("Select a soul to inspect its full timeline.", NamedTextColor.GRAY))));
        inventory.setItem(22, item(Material.CLOCK, "Refresh", NamedTextColor.YELLOW, List.of(
                line("Updates holders, status, and limbo timers.", NamedTextColor.GRAY))));
        inventory.setItem(26, item(Material.BARRIER, "Close", NamedTextColor.RED, List.of()));
        player.openInventory(inventory);
    }

    private void openTimeline(Player player, String soulId, int requestedPage) {
        DragonSoul soul = plugin.souls().byId(soulId).orElse(null);
        if (soul == null) { open(player); return; }
        List<SoulHistoryEntry> events = entries(soul);
        int pages = Math.max(1, (events.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        MenuHolder holder = new MenuHolder(Screen.TIMELINE, soulId, page);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                Component.text(SoulIdentity.displayName(soulId) + " History", NamedTextColor.DARK_PURPLE));
        holder.inventory = inventory;
        if (events.isEmpty()) inventory.setItem(22, item(Material.WRITABLE_BOOK, "No Events Yet", NamedTextColor.GRAY,
                List.of(line("This soul has not begun its journey.", NamedTextColor.DARK_GRAY))));
        for (int index = page * PAGE_SIZE; index < Math.min(events.size(), (page + 1) * PAGE_SIZE); index++)
            inventory.setItem(index - page * PAGE_SIZE, eventItem(events.get(index), index + 1));
        fillRange(inventory, 45, 54, Material.BLACK_STAINED_GLASS_PANE);
        inventory.setItem(45, item(Material.ARROW, "All Souls", NamedTextColor.YELLOW,
                List.of(line("Return to the soul overview.", NamedTextColor.GRAY))));
        if (page > 0) inventory.setItem(48, item(Material.SPECTRAL_ARROW, "Newer Events", NamedTextColor.AQUA,
                List.of(line("Page " + page + " of " + pages, NamedTextColor.GRAY))));
        inventory.setItem(49, timelineSummary(soul, page, pages, events.size()));
        if (page + 1 < pages) inventory.setItem(50, item(Material.ARROW, "Older Events", NamedTextColor.AQUA,
                List.of(line("Page " + (page + 2) + " of " + pages, NamedTextColor.GRAY))));
        inventory.setItem(53, item(Material.BARRIER, "Close", NamedTextColor.RED, List.of()));
        player.openInventory(inventory);
    }

    private ItemStack soulItem(DragonSoul soul) {
        SoulIdentity identity = SoulIdentity.fromId(soul.id());
        List<Component> lore = new ArrayList<>();
        lore.add(label("Status", displayState(soul.state()), stateColor(soul.state())));
        if (displayState(soul.state()).equals("Dormant"))
            lore.add(line("  " + dormantDetail(soul.state()), NamedTextColor.DARK_GRAY));
        lore.add(label("Current holder", playerName(soul.holder()), NamedTextColor.WHITE));
        if (soul.state() == DragonSoulState.MOTHER_SOUL_LIMBO) {
            plugin.consequences().limboReleaseAt(soul.id()).ifPresent(release -> {
                lore.add(label("Entered limbo", latestTime(soul), NamedTextColor.LIGHT_PURPLE));
                lore.add(label(release.isAfter(Instant.now()) ? "Returns" : "Return status",
                        release.isAfter(Instant.now()) ? DATE.format(release) : "Ready — awaiting an eligible player",
                        NamedTextColor.LIGHT_PURPLE));
                if (release.isAfter(Instant.now())) lore.add(line("  " + relative(release), NamedTextColor.GRAY));
            });
        }
        List<String> previous = previousHolders(soul);
        lore.add(Component.empty());
        lore.add(line("Previous holders", NamedTextColor.GRAY));
        if (previous.isEmpty()) lore.add(line("  None yet", NamedTextColor.DARK_GRAY));
        else previous.stream().limit(4).forEach(name -> lore.add(line("  • " + name, NamedTextColor.WHITE)));
        if (previous.size() > 4) lore.add(line("  +" + (previous.size() - 4) + " more", NamedTextColor.DARK_GRAY));
        latest(soul).ifPresent(event -> {
            lore.add(Component.empty());
            lore.add(label("Latest", event.transferType(), NamedTextColor.GOLD));
            lore.add(line(event.description(), NamedTextColor.GRAY));
            lore.add(line(relative(event.timestamp()), NamedTextColor.DARK_GRAY));
        });
        lore.add(Component.empty());
        lore.add(line("Click for the full timeline →", NamedTextColor.YELLOW));
        return item(material(identity), identity.displayName(), soulColor(identity), lore);
    }

    private ItemStack eventItem(SoulHistoryEntry event, int number) {
        List<Component> lore = new ArrayList<>();
        lore.add(line(event.description(), NamedTextColor.GRAY));
        lore.add(Component.empty());
        if (event.fromPlayer() != null) lore.add(label("Previous holder", playerName(event.fromPlayer()), NamedTextColor.WHITE));
        if (event.toPlayer() != null) {
            String role = event.transferType().equals("PvP") || event.transferType().equals("Fractured claim")
                    ? "Killer / new holder" : "New holder";
            lore.add(label(role, playerName(event.toPlayer()), NamedTextColor.WHITE));
        }
        if (!event.callers().isEmpty())
            lore.add(label(event.callers().size() == 1 ? "Caller" : "Callers",
                    String.join(", ", event.callers().stream().map(this::playerName).toList()), NamedTextColor.LIGHT_PURPLE));
        if (event.killer() != null && !event.killer().equals(event.toPlayer()))
            lore.add(label("Killer", playerName(event.killer()), NamedTextColor.RED));
        lore.add(label("When", DATE.format(event.timestamp()), NamedTextColor.AQUA));
        lore.add(line(relative(event.timestamp()), NamedTextColor.DARK_GRAY));
        return item(eventMaterial(event), event.transferType() + " · Event " + number, eventColor(event), lore);
    }

    private ItemStack timelineSummary(DragonSoul soul, int page, int pages, int events) {
        List<Component> lore = new ArrayList<>();
        lore.add(label("Status", displayState(soul.state()), stateColor(soul.state())));
        lore.add(label("Holder", playerName(soul.holder()), NamedTextColor.WHITE));
        lore.add(label("Recorded events", Integer.toString(events), NamedTextColor.AQUA));
        lore.add(label("Page", (page + 1) + " / " + pages, NamedTextColor.GRAY));
        if (soul.state() == DragonSoulState.MOTHER_SOUL_LIMBO)
            plugin.consequences().limboReleaseAt(soul.id()).ifPresent(at ->
                    lore.add(label(at.isAfter(Instant.now()) ? "Returns" : "Return status",
                            at.isAfter(Instant.now()) ? DATE.format(at) + " (" + relative(at) + ")"
                                    : "Ready — awaiting an eligible player", NamedTextColor.LIGHT_PURPLE)));
        return item(material(SoulIdentity.fromId(soul.id())), SoulIdentity.displayName(soul.id()), NamedTextColor.GOLD, lore);
    }

    @EventHandler
    public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getRawSlot();
        if (holder.screen == Screen.OVERVIEW) {
            if (slot == 22) { open(player); sound(player); return; }
            if (slot == 26) { player.closeInventory(); return; }
            int soulIndex = slot == 11 ? 0 : slot == 13 ? 1 : slot == 15 ? 2 : -1;
            List<DragonSoul> souls = plugin.souls().all();
            if (soulIndex >= 0 && soulIndex < souls.size()) {
                openTimeline(player, souls.get(soulIndex).id(), 0);
                sound(player);
            }
            return;
        }
        if (slot == 45) { open(player); sound(player); }
        else if (slot == 48 && holder.page > 0) { openTimeline(player, holder.soulId, holder.page - 1); sound(player); }
        else if (slot == 50 && plugin.souls().byId(holder.soulId)
                .map(soul -> entries(soul).size() > (holder.page + 1) * PAGE_SIZE).orElse(false)) {
            openTimeline(player, holder.soulId, holder.page + 1); sound(player);
        }
        else if (slot == 53) player.closeInventory();
    }

    @EventHandler
    public void drag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof MenuHolder) event.setCancelled(true);
    }

    private List<SoulHistoryEntry> entries(DragonSoul soul) {
        List<SoulHistoryEntry> result = new ArrayList<>();
        for (String raw : soul.lineage()) {
            SoulHistoryEntry entry = SoulHistoryEntry.parse(raw);
            if (entry != null) result.add(entry);
        }
        result.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));
        return result;
    }

    private java.util.Optional<SoulHistoryEntry> latest(DragonSoul soul) {
        return entries(soul).stream().findFirst();
    }

    private List<String> previousHolders(DragonSoul soul) {
        Set<UUID> holders = new LinkedHashSet<>();
        List<SoulHistoryEntry> chronological = entries(soul);
        for (SoulHistoryEntry event : chronological) {
            if (event.fromPlayer() != null && !event.fromPlayer().equals(soul.holder())) holders.add(event.fromPlayer());
            if (event.toPlayer() != null && !event.toPlayer().equals(soul.holder())) holders.add(event.toPlayer());
        }
        return holders.stream().map(this::playerName).toList();
    }

    private String latestTime(DragonSoul soul) {
        return latest(soul).map(entry -> DATE.format(entry.timestamp())).orElse("Unknown");
    }

    private String playerName(UUID id) {
        if (id == null) return "None";
        OfflinePlayer player = Bukkit.getOfflinePlayer(id);
        return player.getName() == null ? "Unknown player" : player.getName();
    }

    private static String displayState(DragonSoulState state) {
        return switch (state) {
            case HELD, ADMIN_HELD -> "Held";
            case MOTHER_SOUL_LIMBO -> "Limbo";
            case FRACTURED -> "Fractured";
            default -> "Dormant";
        };
    }

    private static String dormantDetail(DragonSoulState state) {
        return switch (state) {
            case UNCLAIMED -> "Awaiting a ritual";
            case RITUAL_RESERVED -> "Reserved for an active ritual";
            case TRANSFER_PENDING, TRANSFER_ANIMATING -> "Choosing its next holder";
            case DISABLED -> "Silenced until restored";
            case UNCREATED -> "Not yet awakened";
            default -> "Currently without a holder";
        };
    }

    private static NamedTextColor stateColor(DragonSoulState state) {
        return switch (state) {
            case HELD, ADMIN_HELD -> NamedTextColor.GREEN;
            case MOTHER_SOUL_LIMBO -> NamedTextColor.LIGHT_PURPLE;
            case FRACTURED -> NamedTextColor.RED;
            default -> NamedTextColor.GRAY;
        };
    }

    private static Material material(SoulIdentity identity) {
        return switch (identity) {
            case AKUMA -> Material.BLUE_ICE;
            case REV -> Material.BLAZE_POWDER;
            case LAMARI -> Material.MOSS_BLOCK;
        };
    }

    private static NamedTextColor soulColor(SoulIdentity identity) {
        return switch (identity) {
            case AKUMA -> NamedTextColor.AQUA;
            case REV -> NamedTextColor.RED;
            case LAMARI -> NamedTextColor.GREEN;
        };
    }

    private static Material eventMaterial(SoulHistoryEntry event) {
        return switch (event.transferType()) {
            case "PvP" -> Material.IRON_SWORD;
            case "Ritual" -> Material.ENCHANTING_TABLE;
            case "Mother Soul" -> Material.ECHO_SHARD;
            case "Fracture", "Fractured claim" -> Material.WITHER_SKELETON_SKULL;
            case "Reincarnation" -> Material.TOTEM_OF_UNDYING;
            case "Soul transfer" -> Material.ENDER_PEARL;
            default -> Material.PAPER;
        };
    }

    private static NamedTextColor eventColor(SoulHistoryEntry event) {
        return switch (event.transferType()) {
            case "PvP", "Fracture", "Fractured claim" -> NamedTextColor.RED;
            case "Mother Soul" -> NamedTextColor.LIGHT_PURPLE;
            case "Ritual", "Reincarnation" -> NamedTextColor.GOLD;
            default -> NamedTextColor.AQUA;
        };
    }

    private static String relative(Instant instant) {
        long seconds = Duration.between(Instant.now(), instant).getSeconds();
        boolean future = seconds > 0;
        long absolute = Math.abs(seconds);
        String amount;
        if (absolute < 60) amount = absolute + "s";
        else if (absolute < 3600) amount = absolute / 60 + "m";
        else if (absolute < 86400) amount = absolute / 3600 + "h " + (absolute % 3600) / 60 + "m";
        else amount = absolute / 86400 + "d " + (absolute % 86400) / 3600 + "h";
        return future ? "in " + amount : amount + " ago";
    }

    private static Component label(String label, String value, NamedTextColor valueColor) {
        return Component.text(label + ": ", NamedTextColor.GRAY).append(Component.text(value, valueColor));
    }

    private static Component line(String value, NamedTextColor color) { return Component.text(value, color); }

    private static ItemStack item(Material material, String name, NamedTextColor color, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(component -> component.decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return item;
    }

    private static void fill(Inventory inventory, Material material) { fillRange(inventory, 0, inventory.getSize(), material); }
    private static void fillRange(Inventory inventory, int from, int to, Material material) {
        ItemStack pane = item(material, " ", NamedTextColor.BLACK, List.of());
        for (int slot = from; slot < to; slot++) inventory.setItem(slot, pane);
    }
    private static void sound(Player player) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, .6f, 1.2f); }

    private enum Screen { OVERVIEW, TIMELINE }
    private static final class MenuHolder implements InventoryHolder {
        private final Screen screen;
        private final String soulId;
        private final int page;
        private Inventory inventory;
        private MenuHolder(Screen screen, String soulId, int page) {
            this.screen = screen; this.soulId = soulId; this.page = page;
        }
        @Override public Inventory getInventory() { return inventory; }
    }
}
