package com.dragonaltar.ability;

import com.dragonaltar.DragonAltarPlugin;
import com.dragonaltar.config.ConfigService;
import com.dragonaltar.dragonborn.DragonbornService;
import com.dragonaltar.soul.SoulIdentity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

final class AbilityResonanceCoordinator {
    private final DragonAltarPlugin plugin;
    private final DragonbornService dragonborn;
    private final ConfigService config;
    private final ResonanceState state;
    private final Consumer<Player> unlockCue;

    AbilityResonanceCoordinator(DragonAltarPlugin plugin, DragonbornService dragonborn,
                                ConfigService config, ResonanceState state, Consumer<Player> unlockCue) {
        this.plugin = plugin;
        this.dragonborn = dragonborn;
        this.config = config;
        this.state = state;
        this.unlockCue = unlockCue;
    }

    void refreshUnlocks() {
        Set<UUID> online = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!dragonborn.isDragonborn(player.getUniqueId())) {
                continue;
            }
            online.add(player.getUniqueId());
            String previous = state.unlocked(player.getUniqueId());
            Optional<DragonResonance> current = current(player);
            String next = current.map(DragonResonance::id).orElse(null);
            if (Objects.equals(previous, next)) {
                continue;
            }
            state.setUnlocked(player.getUniqueId(), next);
            if (next == null) {
                if (previous != null) {
                    plugin.messages().send(player, "resonance-lost");
                }
                continue;
            }
            DragonResonance unlocked = current.orElseThrow();
            plugin.messages().send(player, "resonance-unlocked",
                    "ability", unlocked.displayName(),
                    "partners", partnerNames(player, unlocked),
                    "range", Integer.toString((int) Math.round(Math.sqrt(rangeSquared()))));
            unlockCue.accept(player);
        }
        state.retainUnlocked(online);
    }

    Optional<DragonResonance> current(Player player) {
        if (trinityParticipants().stream().anyMatch(member -> member.getUniqueId().equals(player.getUniqueId()))) {
            return Optional.of(DragonResonance.DRAGON_TRINITY);
        }
        SoulIdentity identity = dragonborn.soul(player).orElse(null);
        if (identity == null) {
            return Optional.empty();
        }
        double rangeSquared = rangeSquared();
        return Bukkit.getOnlinePlayers().stream()
                .filter(other -> other != player && dragonborn.isDragonborn(other.getUniqueId()))
                .filter(other -> other.getWorld().equals(player.getWorld())
                        && other.getLocation().distanceSquared(player.getLocation()) <= rangeSquared)
                .min(Comparator.comparingDouble(other -> other.getLocation().distanceSquared(player.getLocation())))
                .flatMap(other -> dragonborn.soul(other)
                        .flatMap(partner -> DragonResonance.pair(identity, partner)));
    }

    List<Player> participants(Player caster, DragonResonance resonance) {
        if (resonance == DragonResonance.DRAGON_TRINITY) {
            List<Player> trio = trinityParticipants();
            return trio.stream().anyMatch(player -> player.getUniqueId().equals(caster.getUniqueId()))
                    ? trio : List.of();
        }
        SoulIdentity casterSoul = dragonborn.soul(caster).orElse(null);
        if (casterSoul == null || !resonance.souls().contains(casterSoul)) {
            return List.of();
        }
        Set<SoulIdentity> needed = new HashSet<>(resonance.souls());
        needed.remove(casterSoul);
        Optional<? extends Player> partner = Bukkit.getOnlinePlayers().stream()
                .filter(other -> other != caster && other.getWorld().equals(caster.getWorld()))
                .filter(other -> other.getLocation().distanceSquared(caster.getLocation()) <= rangeSquared())
                .filter(other -> dragonborn.soul(other).map(needed::contains).orElse(false))
                .findFirst();
        return partner.map(player -> List.of(caster, player)).orElseGet(List::of);
    }

    private List<Player> trinityParticipants() {
        EnumMap<SoulIdentity, Player> members = new EnumMap<>(SoulIdentity.class);
        for (Player player : Bukkit.getOnlinePlayers()) {
            dragonborn.soul(player).ifPresent(identity -> members.put(identity, player));
        }
        if (members.size() != SoulIdentity.values().length) {
            return List.of();
        }
        List<Player> trio = new ArrayList<>(members.values());
        double rangeSquared = rangeSquared();
        for (int first = 0; first < trio.size(); first++) {
            for (int second = first + 1; second < trio.size(); second++) {
                if (!trio.get(first).getWorld().equals(trio.get(second).getWorld())
                        || trio.get(first).getLocation().distanceSquared(trio.get(second).getLocation()) > rangeSquared) {
                    return List.of();
                }
            }
        }
        return List.copyOf(trio);
    }

    private double rangeSquared() {
        double range = config.file("abilities.yml").getDouble("resonances.unlock-range-blocks", 50);
        return range * range;
    }

    private String partnerNames(Player caster, DragonResonance resonance) {
        return participants(caster, resonance).stream()
                .filter(player -> player != caster)
                .map(Player::getName)
                .collect(Collectors.joining(", "));
    }
}
