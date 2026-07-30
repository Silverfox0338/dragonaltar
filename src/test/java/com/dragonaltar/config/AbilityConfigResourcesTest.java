package com.dragonaltar.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AbilityConfigResourcesTest {
    @Test
    void packagedAbilityAndMessageDefaultsAreValidAndVersioned() throws Exception {
        YamlConfiguration abilities=load("abilities.yml");
        YamlConfiguration messages=load("messages.yml");

        assertEquals(7,abilities.getInt("config-version"));
        assertEquals(100,abilities.getInt("energy.maximum"));
        assertTrue(abilities.getDouble("abilities.absolute-zero.shatter-bonus-damage")>0);
        assertTrue(abilities.getInt("abilities.infernos-wrath.maximum-mobility-ticks")
                >=abilities.getInt("abilities.infernos-wrath.initial-mobility-ticks"));
        assertEquals(100,abilities.getInt("rev-hunt.heat.maximum"));
        assertTrue(abilities.getBoolean("rev-hunt.heat-bar.enabled"));
        assertTrue(abilities.getString("rev-hunt.heat-bar.title","").contains("<heat>"));
        assertTrue(abilities.getInt("abilities.revs-rend.recast-window-ticks")>0);
        assertTrue(abilities.getInt("abilities.infernos-wrath.maximum-rampage")>0);
        assertTrue(abilities.getDouble("abilities.titans-bulwark.stored-damage-cap")>0);
        assertEquals(720,abilities.getInt("resonances.dragon-trinity.cooldown-seconds"));
        assertEquals(50,abilities.getInt("resonances.unlock-range-blocks"));
        assertTrue(abilities.getInt("abilities.absolute-zero.presentation.shell-display-count")>0);
        assertTrue(abilities.getInt("resonances.glacial-bastion.ward-charges")>0);
        assertTrue(abilities.getDouble("resonances.volcanic-aegis.retaliation-damage")>0);
        assertTrue(abilities.getInt("resonances.dragon-trinity.pulse-interval-ticks")>0);
        assertEquals(8,messages.getInt("config-version"));
        assertTrue(messages.getString("energy-hud","").contains("<status>"));
        assertTrue(messages.getString("energy-hud","").contains("<resonance_cooldown>"));
    }

    private static YamlConfiguration load(String name) throws Exception {
        try(InputStream stream=AbilityConfigResourcesTest.class.getClassLoader().getResourceAsStream(name)){
            assertNotNull(stream,name+" must be packaged as a test resource");
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream,StandardCharsets.UTF_8));
        }
    }
}
