package com.dragonaltar.api;

import com.dragonaltar.api.addon.DragonAltarAddon;
import com.dragonaltar.api.addon.DragonAddonItem;
import com.dragonaltar.api.event.DragonAddonItemEquipEvent;
import com.dragonaltar.api.event.DragonSoulTransferEvent;
import com.dragonaltar.api.model.DragonEligibilityInfo;
import org.junit.jupiter.api.Test;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicApiModelTest {
    @Test
    void eligibilityChecksAreCopied() {
        Map<String, Boolean> source = new LinkedHashMap<>();
        source.put("online", true);
        DragonEligibilityInfo info = new DragonEligibilityInfo(true, source);
        source.put("excluded", false);

        assertEquals(Map.of("online", true), info.checks());
        assertThrows(UnsupportedOperationException.class, () -> info.checks().put("changed", false));
    }

    @Test
    void addonMetadataRejectsMissingIdentity() {
        assertThrows(IllegalArgumentException.class,
                () -> new DragonAltarAddon("", "Example", "1.0.0", "Owner", ""));
        assertThrows(IllegalArgumentException.class,
                () -> new DragonAltarAddon("example", "Example", "", "Owner", ""));
    }

    @Test
    void compatibilityTransferEventCarriesTheTransferAndCanCancelIt() {
        UUID from=UUID.randomUUID(),to=UUID.randomUUID();
        DragonSoulTransferEvent event=new DragonSoulTransferEvent("soul-1",from,to);
        assertEquals("soul-1",event.soulId());
        assertEquals(from,event.from());
        assertEquals(to,event.to());
        assertFalse(event.isCancelled());
        event.setCancelled(true);
        assertTrue(event.isCancelled());
    }

    @Test
    void addonItemApiPublishesSnapshotContextAndCancellableEvent() throws Exception {
        assertTrue(Cancellable.class.isAssignableFrom(DragonAddonItemEquipEvent.class));
        assertEquals(ItemStack.class, DragonAddonItem.Context.class.getMethod("item").getReturnType());
        assertEquals(EquipmentSlot.class, DragonAddonItem.Context.class.getMethod("slot").getReturnType());
        DragonAddonItemEquipEvent.class.getConstructor(
                org.bukkit.entity.Player.class, String.class, String.class,
                EquipmentSlot.class, ItemStack.class);
        assertEquals(void.class, DragonAltarApi.class.getMethod(
                "registerItem", org.bukkit.plugin.Plugin.class, DragonAddonItem.class).getReturnType());
        assertEquals(void.class, DragonAltarApi.class.getMethod(
                "tagSoulBound", ItemStack.class, String.class).getReturnType());
    }

    @Test
    void soulBoundMarkersRequireCanonicalNamespacedItemIds() {
        assertTrue(DragonAltarApiImpl.validNamespacedId("ember-tools:frost-vestment"));
        assertTrue(DragonAltarApiImpl.validNamespacedId("a1:item_2"));
        assertFalse(DragonAltarApiImpl.validNamespacedId(null));
        assertFalse(DragonAltarApiImpl.validNamespacedId("ember-tools"));
        assertFalse(DragonAltarApiImpl.validNamespacedId("Ember:vestment"));
        assertFalse(DragonAltarApiImpl.validNamespacedId("ember:a"));
        assertFalse(DragonAltarApiImpl.validNamespacedId("ember:item:extra"));
    }

}
