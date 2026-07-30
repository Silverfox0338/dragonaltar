package com.dragonaltar.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PackagedResourcesTest {
    private static final List<String> CONFIGS=List.of(
            "config.yml","messages.yml","altar.yml","ritual.yml","abilities.yml","animations.yml");

    @Test
    void everyPackagedYamlLoadsStrictlyAndIsVersioned() throws Exception {
        for(String name:CONFIGS){
            YamlConfiguration yaml=load(name);
            assertTrue(yaml.getInt("config-version",0)>0,name+" must declare a positive config-version");
            assertFalse(yaml.getKeys(false).isEmpty(),name+" must not be empty");
        }
    }

    @Test
    void playerFacingResourcesContainNoLongDashes() throws Exception {
        for(String name:CONFIGS)assertNoLongDash(load(name).getValues(true),name);
    }

    @Test
    void jarContainsTheLicenseAndOwnerNotice() throws Exception {
        try(InputStream stream=PackagedResourcesTest.class.getClassLoader().getResourceAsStream("META-INF/LICENSE.md")){
            assertNotNull(stream,"LICENSE.md must be packaged");
            String text=new String(stream.readAllBytes(),StandardCharsets.UTF_8);
            assertTrue(text.contains("DragonAltar is owned by Silverfox0338."));
            assertTrue(text.contains("paid features"));
        }
    }

    @Test
    void packagedConfigsExcludeRemovedSettings() throws Exception {
        YamlConfiguration altar=load("altar.yml");
        assertEquals(4,altar.getInt("config-version"));
        assertFalse(altar.contains("world"));
        assertFalse(altar.contains("existing-structure"));
        assertFalse(altar.contains("schematic"));
        assertFalse(altar.contains("protection.enabled"));
        assertFalse(altar.contains("display.bob-height"));
        assertFalse(altar.contains("display.scale-pulse-amplitude"));
        assertTrue(altar.contains("internal-protection.required-for-event"));

        YamlConfiguration ritual=load("ritual.yml");
        assertEquals(3,ritual.getInt("config-version"));
        assertFalse(ritual.contains("elytra.accept-any-durability"));
        assertFalse(ritual.getMapList("offerings").stream().anyMatch(value->value.containsKey("durability-mode")));

        YamlConfiguration abilities=load("abilities.yml");
        assertFalse(abilities.contains("focus.soulbound"));
        assertFalse(abilities.contains("focus.non-droppable"));
        assertFalse(abilities.contains("passives.fire-damage-multiplier"));
        assertFalse(abilities.contains("abilities.dash"));
        assertFalse(abilities.contains("abilities.sight"));
        assertFalse(abilities.contains("abilities.resolve"));

        YamlConfiguration general=load("config.yml");
        assertEquals(3,general.getInt("config-version"));
        assertFalse(general.contains("forced-removal-ritual.participant-count"));

        YamlConfiguration messages=load("messages.yml");
        assertEquals(8,messages.getInt("config-version"));
        assertFalse(messages.contains("prefix"));
        assertFalse(messages.contains("egg-hologram"));
        for(String path:List.of("player-only","setup-incomplete","event-recovery-required","ritual-error","event-started"))
            assertTrue(messages.contains(path),"messages.yml must retain active key "+path);
    }

    private static void assertNoLongDash(Map<String,Object> values,String name){
        for(var entry:values.entrySet()){
            if(entry.getValue() instanceof String text)
                assertFalse(text.contains("—")||text.contains("–"),name+" contains a long dash at "+entry.getKey());
        }
    }

    private static YamlConfiguration load(String name) throws Exception {
        try(InputStream stream=PackagedResourcesTest.class.getClassLoader().getResourceAsStream(name)){
            assertNotNull(stream,name+" must be packaged");
            YamlConfiguration yaml=new YamlConfiguration();
            yaml.load(new InputStreamReader(stream,StandardCharsets.UTF_8));
            return yaml;
        }
    }
}
