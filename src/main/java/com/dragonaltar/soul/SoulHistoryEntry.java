package com.dragonaltar.soul;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * A tolerant view of the original pipe-delimited lineage format. Keeping the
 * persisted representation compatible means existing servers gain the GUI
 * without a data migration.
 */
public record SoulHistoryEntry(Instant timestamp, String from, String to, String reason, List<UUID> callers,
                               UUID killer, UUID adminActor) {
    public static SoulHistoryEntry parse(String value) {
        if (value == null) return null;
        String[] parts = value.split("\\|", 4);
        if (parts.length < 4) return null;
        try {
            String rawReason = parts[3];
            String[] reasonParts = rawReason.split(";");
            String reason = reasonParts[0];
            List<UUID> callers = new ArrayList<>();
            UUID killer = null;
            UUID adminActor = null;
            for (int i = 1; i < reasonParts.length; i++) {
                if (reasonParts[i].startsWith("callers=")) for (String caller : reasonParts[i].substring(8).split(",")) {
                    try { callers.add(UUID.fromString(caller)); }
                    catch (IllegalArgumentException ignored) {}
                }
                if (reasonParts[i].startsWith("killer=")) {
                    try { killer = UUID.fromString(reasonParts[i].substring(7)); }
                    catch (IllegalArgumentException ignored) {}
                }
                if (reasonParts[i].startsWith("admin=")) {
                    try { adminActor = UUID.fromString(reasonParts[i].substring(6)); }
                    catch (IllegalArgumentException ignored) {}
                }
            }
            return new SoulHistoryEntry(Instant.parse(parts[0]), parts[1], parts[2], reason,
                    List.copyOf(callers), killer, adminActor);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public UUID fromPlayer() { return player(from); }
    public UUID toPlayer() { return player(to); }

    public String transferType() {
        String normalized = reason.toUpperCase(Locale.ROOT);
        if (normalized.contains("PVP") || normalized.contains("COMBAT_LOG")) return "PvP";
        if (normalized.contains("FRACTURED")) return normalized.contains("KILL") ? "Fractured claim" : "Fracture";
        if (normalized.contains("MOTHER_SOUL") || normalized.contains("FORCED_REMOVAL")
                || normalized.contains("CALLER_BACKFIRE")) return "Mother Soul";
        if (normalized.contains("ADMIN_PENDING") || normalized.contains("ADMIN_REMOVE")
                || normalized.contains("DEV_UNASSIGN") || normalized.contains("DEV_SETSTATE")) return "Soul event";
        if (normalized.contains("ADMIN_REINCARNATE")) return "Reincarnation";
        if ((normalized.contains("ADMIN") && normalized.contains("TRANSFER"))
                || normalized.contains("DEV_ASSIGN")) return "Soul transfer";
        if (normalized.contains("RITUAL")) return "Ritual";
        if (normalized.contains("REINCARN") || normalized.contains("PENDING")
                || normalized.contains("NATURAL_DEATH")) return "Reincarnation";
        if (normalized.contains("ADMIN") || normalized.contains("DEV_")) return "Soul event";
        if (normalized.contains("REPAIR") || normalized.contains("STARTUP")) return "Recovery";
        return "Soul event";
    }

    public String description() {
        return switch (reason.toUpperCase(Locale.ROOT)) {
            case "INITIAL_RITUAL" -> "Claimed through the Dragonborn ritual";
            case "PVP_INHERITANCE" -> "Inherited through a PvP killing blow";
            case "COMBAT_LOG" -> "Transferred after combat logging";
            case "NATURAL_DEATH" -> "Released after its holder died";
            case "NATURAL_REINCARNATION", "JOIN_PENDING", "STARTUP_PENDING_RECOVERY" -> "Chose a new holder through reincarnation";
            case "FORCED_REMOVAL_RITUAL" -> "Transferred by the Mother Soul ritual";
            case "FORCED_REMOVAL_RITUAL_INSTABILITY" -> "Fractured during a Mother Soul ritual";
            case "DRAGONBORN_CALLER_BACKFIRE" -> "Taken into limbo by Mother Soul backfire";
            case "MOTHER_SOUL_LIMBO_RELEASE" -> "Returned from limbo to a new holder";
            case "FRACTURED_SOUL_KILL" -> "Claimed by defeating its fractured form";
            case "DRAGONBORN_KILLER_DORMANT" -> "Became dormant after its holder was slain";
            case "ADMIN_GRANT", "DEV_ASSIGN" -> "Chose a new holder";
            case "ADMIN_TRANSFER", "ADMIN_FORCE_TRANSFER" -> "Passed to a new holder";
            case "ADMIN_REINCARNATE" -> "Chose a new holder through reincarnation";
            case "ADMIN_REMOVE", "ADMIN_FORCE_REMOVE", "ADMIN_PENDING", "DEV_UNASSIGN" -> "Entered dormancy";
            case "DEV_SETSTATE" -> "Soul state changed";
            default -> readable(reason);
        };
    }

    private static UUID player(String value) {
        try { return value == null ? null : UUID.fromString(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private static String readable(String value) {
        if (value == null || value.isBlank()) return "Soul state changed";
        String[] words = value.toLowerCase(Locale.ROOT).split("_");
        String result = String.join(" ", words);
        return Character.toUpperCase(result.charAt(0)) + result.substring(1);
    }
}
