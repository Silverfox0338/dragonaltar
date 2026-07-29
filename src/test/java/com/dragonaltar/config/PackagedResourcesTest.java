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
