package com.dragonaltar.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns validated backup paths and the persistence barriers around file copies.
 */
public final class PersistenceBackupService {
	private static final List<String> DATA_FILES = List.of("event.yml", "altar-state.yml", "souls.yml", "players.yml",
			"rituals.yml", "cooldowns.yml", "pending-transfers.yml", "consequences.yml");

	private final Path dataDirectory;
	private final Path backupDirectory;
	private final YamlDataStore store;
	private final Logger logger;
	private final Clock clock;

	public PersistenceBackupService(Path pluginDataDirectory, YamlDataStore store, Logger logger) {
		this(pluginDataDirectory, store, logger, Clock.systemUTC());
	}

	PersistenceBackupService(Path pluginDataDirectory, YamlDataStore store, Logger logger, Clock clock) {
		this.dataDirectory = pluginDataDirectory.resolve("data");
		this.backupDirectory = pluginDataDirectory.resolve("backups").toAbsolutePath().normalize();
		this.store = store;
		this.logger = logger;
		this.clock = clock;
	}

	public String create() {
		store.flush();
		String name = Instant.now(clock).toString().replace(':', '-');
		Path destination = backupDirectory.resolve(name);
		try {
			Files.createDirectories(destination);
			try (var stream = Files.list(dataDirectory)) {
				for (Path source : stream.filter(Files::isRegularFile).toList()) {
					Files.copy(source, destination.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
				}
			}
			return name;
		} catch (IOException exception) {
			throw new IllegalStateException("Backup failed", exception);
		}
	}

	public List<String> list() {
		try (var stream = Files.list(backupDirectory)) {
			return stream.filter(Files::isDirectory).map(path -> path.getFileName().toString()).sorted().toList();
		} catch (IOException exception) {
			logger.log(Level.WARNING, "Could not list DragonAltar backups in " + backupDirectory, exception);
			return List.of();
		}
	}

	public void restore(String name) {
		Path source = backupDirectory.resolve(name).normalize();
		if (!source.getParent().equals(backupDirectory) || !Files.isDirectory(source)) {
			throw new IllegalArgumentException("Unknown backup");
		}
		store.flush();
		try {
			for (String file : DATA_FILES) {
				Path from = source.resolve(file);
				if (Files.isRegularFile(from)) {
					Files.copy(from, dataDirectory.resolve(file), StandardCopyOption.REPLACE_EXISTING);
				}
			}
		} catch (IOException exception) {
			throw new IllegalStateException("Backup restore failed", exception);
		}
	}
}
