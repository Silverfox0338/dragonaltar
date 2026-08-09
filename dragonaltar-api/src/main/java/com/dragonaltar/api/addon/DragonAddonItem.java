package com.dragonaltar.api.addon;

import com.dragonaltar.api.DragonAltarApi;
import com.dragonaltar.api.model.DragonActionResult;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * A soul-bound equipment definition supplied by a registered add-on.
 *
 * <p>
 * The id must use the add-on namespace, for example
 * {@code my-addon:frost-vestment}. The soul may be a canonical id or public
 * name.
 * </p>
 */
public interface DragonAddonItem {
	String id();
	String displayName();
	String soulId();

	/**
	 * Common action DragonAltar should take when this item's wearer loses its soul.
	 */
	default StripPolicy onSoulLoss() {
		return StripPolicy.NONE;
	}

	default DragonActionResult canEquip(Context context) {
		return DragonActionResult.ok();
	}

	enum StripPolicy {
		/** Leave the item equipped so the add-on can handle the loss itself. */
		NONE,
		/**
		 * Move the item to ordinary inventory space, dropping it if no safe slot
		 * exists.
		 */
		UNEQUIP,
		/** Remove the item from its slot and drop it at the player's location. */
		DROP,
		/** Permanently remove the equipped item. */
		DESTROY
	}

	record Context(Player player, EquipmentSlot slot, ItemStack item, DragonAltarApi api) {
		public Context {
			Objects.requireNonNull(player, "player");
			Objects.requireNonNull(slot, "slot");
			item = Objects.requireNonNull(item, "item").clone();
			Objects.requireNonNull(api, "api");
		}

		@Override
		public ItemStack item() {
			return item.clone();
		}
	}
}
