package com.dragonaltar.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

class YamlDataStoreTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void coalescesWaitingSnapshotsAndCompletesEveryCaller() throws Exception {
		ExecutorService writer = Executors.newSingleThreadExecutor();
		YamlDataStore store = new YamlDataStore(temporaryDirectory, quietLogger(), writer);
		store.initialize();

		CountDownLatch writerStarted = new CountDownLatch(1);
		CountDownLatch releaseWriter = new CountDownLatch(1);
		writer.submit(() -> {
			writerStarted.countDown();
			releaseWriter.await(5, TimeUnit.SECONDS);
			return null;
		});
		assertTrue(writerStarted.await(5, TimeUnit.SECONDS));

		var first = store.saveCoalesced("players.yml", yamlWithValue(1));
		var second = store.saveCoalesced("players.yml", yamlWithValue(2));
		var third = store.saveCoalesced("players.yml", yamlWithValue(3));
		releaseWriter.countDown();
		store.flush();

		assertTrue(first.isDone());
		assertTrue(second.isDone());
		assertTrue(third.isDone());
		assertEquals(3, store.load("players.yml").getInt("value"));
		store.close();
	}

	@Test
	void flushIsABarrierBeforeExternalRestore() throws Exception {
		try (YamlDataStore store = new YamlDataStore(temporaryDirectory, quietLogger())) {
			store.initialize();
			store.save("event.yml", yamlWithValue(1));
			store.save("event.yml", yamlWithValue(2));
			store.flush();

			Files.writeString(temporaryDirectory.resolve("data/event.yml"), "value: 99\n");
			assertEquals(99, store.load("event.yml").getInt("value"));
		}
	}

	@Test
	void asynchronousFailuresReachFutureAndFlush() throws Exception {
		YamlDataStore store = new YamlDataStore(temporaryDirectory, quietLogger());
		store.initialize();
		Path destination = temporaryDirectory.resolve("data/event.yml");
		Files.delete(destination);
		Files.createDirectory(destination);

		var write = store.save("event.yml", yamlWithValue(1));
		assertThrows(CompletionException.class, write::join);
		assertThrows(IllegalStateException.class, store::flush);
		store.close();
	}

	@Test
	void rejectsPathsOutsideDataDirectory() throws Exception {
		try (YamlDataStore store = new YamlDataStore(temporaryDirectory, quietLogger())) {
			store.initialize();
			assertThrows(IllegalArgumentException.class, () -> store.load("../config.yml"));
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
