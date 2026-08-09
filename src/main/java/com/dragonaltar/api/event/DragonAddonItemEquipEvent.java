package com.dragonaltar.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/** Fired after DragonAltar's built-in add-on item checks pass, before equip. */
public final class DragonAddonItemEquipEvent extends DragonAltarCancellableEvent {
	private static final HandlerList HANDLERS = new HandlerList();
	private final Player player;
	private final String itemId;
	private final String soulId;
	private final EquipmentSlot slot;
	private final ItemStack item;

	public DragonAddonItemEquipEvent(Player player, String itemId, String soulId, EquipmentSlot slot, ItemStack item) {
		this.player = Objects.requireNonNull(player, "player");
		this.itemId = Objects.requireNonNull(itemId, "itemId");
		this.soulId = Objects.requireNonNull(soulId, "soulId");
		this.slot = Objects.requireNonNull(slot, "slot");
		this.item = Objects.requireNonNull(item, "item").clone();
	}

	public Player player() {
		return player;
	}
	public String itemId() {
		return itemId;
	}
	public String soulId() {
		return soulId;
	}
	public EquipmentSlot slot() {
		return slot;
	}
	public ItemStack item() {
		return item.clone();
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}
	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
