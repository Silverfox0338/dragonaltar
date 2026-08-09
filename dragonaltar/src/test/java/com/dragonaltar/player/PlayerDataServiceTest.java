package com.dragonaltar.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragonaltar.persistence.YamlDataStore;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

class PlayerDataServiceTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void cooldownsLoadWhenPlayersDocumentHasNoPlayerSection() throws Exception {
		Logger logger = quietLogger();
		try (YamlDataStore store = new YamlDataStore(temporaryDirectory, logger)) {
			store.initialize();
			UUID playerId = UUID.randomUUID();
			YamlConfiguration cooldowns = new YamlConfiguration();
			cooldowns.set("data-version", 1);
			cooldowns.set("players." + playerId + ".wings", 1234L);
			store.save("cooldowns.yml", cooldowns);
			store.flush();

			PlayerDataService players = new PlayerDataService(store, logger);
			players.load();

			assertEquals(Map.of("wings", 1234L), players.cooldowns(playerId));
		}
	}

	private static Logger quietLogger() {
		Logger logger = Logger.getAnonymousLogger();
		logger.setLevel(Level.OFF);
		return logger;
	}
}
