package com.dragonaltar.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PluginMetadataTest {
	@Test
	void filteredPluginMetadataMatchesReleaseContract() throws Exception {
		try (var stream = PluginMetadataTest.class.getClassLoader().getResourceAsStream("plugin.yml")) {
			assertNotNull(stream);
			YamlConfiguration yaml = new YamlConfiguration();
			yaml.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
			assertEquals("DragonAltar", yaml.getString("name"));
			assertEquals("1.4.21", yaml.getString("version"));
			assertEquals("1.21", yaml.getString("api-version"));
			assertEquals("com.dragonaltar.DragonAltarPlugin", yaml.getString("main"));
			assertEquals(Set.of("PlaceholderAPI", "ScaledEnderDragon"), Set.copyOf(yaml.getStringList("softdepend")));
			assertTrue(yaml.isConfigurationSection("commands.dragon"));
			assertEquals(Set.of("dragonaltar"), Set.copyOf(yaml.getStringList("commands.dragon.aliases")));
			assertTrue(yaml.isConfigurationSection("permissions.dragonaltar.admin"));
			assertTrue(yaml.isConfigurationSection("permissions.dragonaltar.use"));
		}
	}
}
