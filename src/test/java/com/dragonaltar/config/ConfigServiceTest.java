package com.dragonaltar.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigServiceTest {
    @Test void upgradesLegacyGeneralConfigWithoutOverwritingEdits() {
        YamlConfiguration legacy = new YamlConfiguration();
        legacy.set("config-version", 1);
        legacy.set("server-mode", "PRODUCTION");
        legacy.set("instability.threshold", 99);

        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("config-version", 2);
        defaults.set("server-mode", "BETA");
        defaults.set("instability.threshold", 6);
        defaults.set("instability.fracture-chance", .20);
        defaults.set("instability.teleport-min-seconds", 45);
        defaults.set("instability.teleport-max-seconds", 60);
        defaults.set("forced-removal-ritual.backfire-limbo-hours", 12);

        assertTrue(ConfigService.mergeMissing(legacy, defaults));
        assertEquals(2, legacy.getInt("config-version"));
        assertEquals("PRODUCTION", legacy.getString("server-mode"));
        assertEquals(99, legacy.getLong("instability.threshold"));
        assertEquals(.20, legacy.getDouble("instability.fracture-chance"));
        assertEquals(45, legacy.getLong("instability.teleport-min-seconds"));
        assertEquals(60, legacy.getLong("instability.teleport-max-seconds"));
        assertEquals(12, legacy.getLong("forced-removal-ritual.backfire-limbo-hours"));
        assertFalse(ConfigService.mergeMissing(legacy, defaults));
    }

    @Test
    void knownDefaultHudMigratesButAdministratorTextDoesNot() {
        String oldDefault="<light_purple>Dragon Energy:</light_purple> <energy>/<maximum> <gray>|</gray> <ability> <gray>|</gray> <yellow>Ability CD: <cooldown></yellow> <gray>|</gray> <gold>Ultimate CD: <ultimate_cooldown></gold>";
        YamlConfiguration defaults=new YamlConfiguration();
        defaults.set("energy-hud",oldDefault+"<status>");
        YamlConfiguration unchanged=new YamlConfiguration();
        unchanged.set("energy-hud",oldDefault);
        assertTrue(ConfigService.migrateAbilityHudMessage(unchanged,defaults,5));
        assertEquals(oldDefault+"<status>",unchanged.getString("energy-hud"));

        YamlConfiguration customized=new YamlConfiguration();
        customized.set("energy-hud","My custom HUD");
        assertFalse(ConfigService.migrateAbilityHudMessage(customized,defaults,5));
        assertEquals("My custom HUD",customized.getString("energy-hud"));
    }

    @Test
    void versionSixHudGainsResonanceFieldsWithoutReplacingCustomText() {
        String versionSix="<light_purple>Dragon Energy:</light_purple> <energy>/<maximum> <gray>|</gray> <ability> <gray>|</gray> <yellow>Ability CD: <cooldown></yellow> <gray>|</gray> <gold>Ultimate CD: <ultimate_cooldown></gold><status>";
        YamlConfiguration defaults=new YamlConfiguration();
        defaults.set("energy-hud",versionSix+" <resonance> <resonance_cooldown>");
        YamlConfiguration shipped=new YamlConfiguration();
        shipped.set("energy-hud",versionSix);
        assertTrue(ConfigService.migrateResonanceHudMessage(shipped,defaults,6));
        assertTrue(shipped.getString("energy-hud","").contains("<resonance_cooldown>"));

        YamlConfiguration custom=new YamlConfiguration();
        custom.set("energy-hud","Custom energy");
        assertFalse(ConfigService.migrateResonanceHudMessage(custom,defaults,6));
        assertEquals("Custom energy",custom.getString("energy-hud"));
    }

    @Test
    void resonanceV5MigratesCustomRenamedTunablesAndUpgradesOldDefaults() {
        YamlConfiguration legacy=new YamlConfiguration();
        legacy.set("resonances.thermal-convergence.damage",17d);
        legacy.set("resonances.volcanic-aegis.damage",8d);
        legacy.set("resonances.volcanic-aegis.buff-seconds",13);
        YamlConfiguration defaults=new YamlConfiguration();
        defaults.set("resonances.thermal-convergence.initial-damage",12d);
        defaults.set("resonances.volcanic-aegis.initial-damage",10d);
        defaults.set("resonances.volcanic-aegis.active-seconds",10);

        assertTrue(ConfigService.migrateResonanceV5(legacy,defaults));
        assertEquals(17d,legacy.getDouble("resonances.thermal-convergence.initial-damage"));
        assertEquals(10d,legacy.getDouble("resonances.volcanic-aegis.initial-damage"));
        assertEquals(13,legacy.getInt("resonances.volcanic-aegis.active-seconds"));
        assertFalse(legacy.contains("resonances.thermal-convergence.damage"));
    }

    @Test
    void revV6MigratesCustomTimingWithoutOverwritingNewSettings() {
        YamlConfiguration legacy=new YamlConfiguration();
        legacy.set("abilities.revs-rend.dash-strength",3d);
        legacy.set("abilities.infernos-wrath.speed-seconds",9);
        legacy.set("abilities.infernos-wrath.mark-duration-seconds",8);
        YamlConfiguration defaults=new YamlConfiguration();
        defaults.set("abilities.revs-rend.dash-strength",1.65d);
        defaults.set("abilities.infernos-wrath.initial-mobility-ticks",60);
        defaults.set("rev-hunt.mark.duration-ticks",120);

        assertTrue(ConfigService.migrateRevV6(legacy,defaults));
        assertEquals(3d,legacy.getDouble("abilities.revs-rend.dash-strength"));
        assertEquals(180,legacy.getInt("abilities.infernos-wrath.initial-mobility-ticks"));
        assertEquals(120,legacy.getInt("rev-hunt.mark.duration-ticks"));
        assertFalse(legacy.contains("abilities.infernos-wrath.speed-seconds"));
    }
}
