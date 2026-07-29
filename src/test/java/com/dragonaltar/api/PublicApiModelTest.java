package com.dragonaltar.api;

import com.dragonaltar.api.addon.DragonAltarAddon;
import com.dragonaltar.api.model.DragonEligibilityInfo;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
