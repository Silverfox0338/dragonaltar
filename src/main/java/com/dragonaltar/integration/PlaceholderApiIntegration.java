package com.dragonaltar.integration;

import com.dragonaltar.DragonAltarPlugin;
import com.dragonaltar.soul.SoulIdentity;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlaceholderApiIntegration extends PlaceholderExpansion {
	private final DragonAltarPlugin plugin;
	public PlaceholderApiIntegration(DragonAltarPlugin plugin) {
		this.plugin = plugin;
	}
	@Override
	public @NotNull String getIdentifier() {
		return "dragonaltar";
	}
	@Override
	public @NotNull String getAuthor() {
		return "DragonAltar";
	}
	@Override
	public @NotNull String getVersion() {
		return plugin.getPluginMeta().getVersion();
	}
	@Override
	public boolean persist() {
		return true;
	}
	@Override
	public @Nullable String onRequest(OfflinePlayer offline, String params) {
		Player p = offline == null ? null : offline.getPlayer();
		return switch (params.toLowerCase()) {
			case "event_state" -> plugin.dragonEvent().state().name();
			case "event_completed" -> Boolean.toString(plugin.dragonEvent().state().name().equals("COMPLETED"));
			case "altar_state" -> plugin.dragonEvent().altarState().name();
			case "souls_unclaimed" -> Long.toString(plugin.souls().unclaimedCount());
			case "souls_pending" -> Long.toString(
					plugin.souls().all().stream().filter(s -> s.state().name().equals("TRANSFER_PENDING")).count());
			case "dragonborn_count" ->
				Long.toString(plugin.souls().all().stream().filter(s -> s.holder() != null).count());
			case "is_dragonborn" -> Boolean.toString(p != null && plugin.dragonborn().isDragonborn(p.getUniqueId()));
			case "soul_id", "soul_name" -> p == null
					? ""
					: plugin.souls().byHolder(p.getUniqueId()).map(s -> SoulIdentity.displayName(s.id())).orElse("");
			case "soul_internal_id" ->
				p == null ? "" : plugin.souls().byHolder(p.getUniqueId()).map(s -> s.id()).orElse("");
			case "energy" -> p == null || !plugin.dragonborn().isDragonborn(p.getUniqueId())
					? "0"
					: Integer.toString(plugin.abilities().current(p));
			case "energy_max" -> Integer.toString(plugin.abilities().maxEnergy());
			case "selected_ability" -> p == null ? "" : plugin.abilities().selected(p);
			case "selected_ability_cooldown" ->
				p == null ? "0" : Long.toString(plugin.abilities().cooldownSeconds(p, plugin.abilities().selected(p)));
			case "combat_tagged" -> Boolean.toString(p != null && plugin.combatTags().tagged(p.getUniqueId()));
			case "combat_seconds" -> p == null ? "0" : Integer.toString(plugin.combatTags().seconds(p.getUniqueId()));
			default -> null;
		};
	}
}
