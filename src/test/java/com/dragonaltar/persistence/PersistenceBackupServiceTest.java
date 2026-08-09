package com.dragonaltar.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.logging.Level;
import java.util.logging.Logger;

class PersistenceBackupServiceTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void createAndRestoreAreSeparatedByPersistenceBarriers() throws Exception {
		try (YamlDataStore store = new YamlDataStore(temporaryDirectory, quietLogger())) {
			store.initialize();
			store.save("players.yml", yamlWithValue(7));
			PersistenceBackupService backups = new PersistenceBackupService(temporaryDirectory, store, quietLogger(),
					Clock.fixed(Instant.parse("2026-08-08T12:34:56Z"), ZoneOffset.UTC));

			String name = backups.create();
			assertEquals("2026-08-08T12-34-56Z", name);
			store.save("players.yml", yamlWithValue(99));
			store.flush();

			backups.restore(name);
			assertEquals(7, store.load("players.yml").getInt("value"));
		}
	}

	@Test
	void restoreRejectsTraversalAndUnknownDirectories() throws Exception {
		try (YamlDataStore store = new YamlDataStore(temporaryDirectory, quietLogger())) {
			store.initialize();
			PersistenceBackupService backups = new PersistenceBackupService(temporaryDirectory, store, quietLogger());
			assertThrows(IllegalArgumentException.class, () -> backups.restore("../data"));
			assertThrows(IllegalArgumentException.class, () -> backups.restore("missing"));
		}
	}

	private static YamlConfiguration yamlWithValue(int value) {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("value", value);
		return yaml;
	}

	private static Logger quietLogger() {
		Logger logger = Logger.getAnonymousLogger();
		logger.setLevel(Level.OFF);
		return logger;
	}
}
