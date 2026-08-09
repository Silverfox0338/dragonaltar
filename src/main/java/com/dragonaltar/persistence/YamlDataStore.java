package com.dragonaltar.persistence;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serializes immutable YAML snapshots to disk without accessing Bukkit state
 * from the writer thread. Calls to {@link #flush()} form an ordering barrier
 * for every write submitted before the call.
 */
public final class YamlDataStore implements AutoCloseable {
	private static final List<String> DATA_FILES = List.of("event.yml", "altar-state.yml", "souls.yml", "players.yml",
			"rituals.yml", "cooldowns.yml", "pending-transfers.yml", "consequences.yml");
	private static final long FLUSH_TIMEOUT_SECONDS = 10;

	private final Path pluginDataDirectory;
	private final Path dataDirectory;
	private final Logger logger;
	private final ExecutorService writer;
	private final AtomicBoolean closed = new AtomicBoolean();
	private final AtomicReference<Throwable> writeFailure = new AtomicReference<>();
	private final Object pendingLock = new Object();
	private final Map<String, PendingWrite> pendingWrites = new HashMap<>();

	public YamlDataStore(JavaPlugin plugin) {
		this(plugin.getDataFolder().toPath(), plugin.getLogger());
	}

	/** Visible for persistence contract tests and non-Bukkit tooling. */
	public YamlDataStore(Path pluginDataDirectory, Logger logger) {
		this(pluginDataDirectory, logger, newWriter());
	}

	YamlDataStore(Path pluginDataDirectory, Logger logger, ExecutorService writer) {
		this.pluginDataDirectory = Objects.requireNonNull(pluginDataDirectory, "pluginDataDirectory");
		this.dataDirectory = pluginDataDirectory.resolve("data");
		this.logger = Objects.requireNonNull(logger, "logger");
		this.writer = Objects.requireNonNull(writer, "writer");
	}

	public void initialize() throws IOException {
		Files.createDirectories(dataDirectory);
		Files.createDirectories(pluginDataDirectory.resolve("backups"));
		Files.createDirectories(pluginDataDirectory.resolve("logs"));
		for (String name : DATA_FILES) {
			Path path = resolveDataFile(name);
			if (Files.notExists(path)) {
				YamlConfiguration yaml = new YamlConfiguration();
				yaml.set("data-version", 1);
				saveAtomic(path, yaml.saveToString());
			}
		}
	}

	public synchronized YamlConfiguration load(String name) {
		return YamlConfiguration.loadConfiguration(resolveDataFile(name).toFile());
	}

	/**
	 * Queues every supplied snapshot. Use this for crash-sensitive state
	 * transitions.
	 */
	public CompletableFuture<Void> save(String name, YamlConfiguration yaml) {
		resolveDataFile(name);
		return submit(name, yaml.saveToString());
	}

	/**
	 * Coalesces snapshots that have not started writing yet. Every returned future
	 * completes only after a snapshot at least as new as its caller's snapshot has
	 * reached disk.
	 */
	public CompletableFuture<Void> saveCoalesced(String name, YamlConfiguration yaml) {
		ensureOpen();
		resolveDataFile(name);
		String snapshot = yaml.saveToString();
		CompletableFuture<Void> completion = new CompletableFuture<>();
		synchronized (pendingLock) {
			PendingWrite pending = pendingWrites.get(name);
			if (pending != null) {
				pending.snapshot = snapshot;
				pending.completions.add(completion);
				return completion;
			}
			pendingWrites.put(name, new PendingWrite(snapshot, completion));
			try {
				writer.execute(() -> drainCoalesced(name));
			} catch (RejectedExecutionException exception) {
				pendingWrites.remove(name);
				completion.completeExceptionally(exception);
				throw exception;
			}
		}
		return completion;
	}

	public void flush() {
		ensureOpen();
		try {
			writer.submit(() -> {
			}).get(FLUSH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Persistence flush interrupted", exception);
		} catch (ExecutionException | TimeoutException exception) {
			throw new IllegalStateException("Persistence flush failed", exception);
		}
		throwRecordedFailure();
	}

	private CompletableFuture<Void> submit(String name, String snapshot) {
		ensureOpen();
		return CompletableFuture.runAsync(() -> write(name, snapshot), writer);
	}

	private void drainCoalesced(String name) {
		PendingWrite pending;
		synchronized (pendingLock) {
			pending = pendingWrites.remove(name);
		}
		if (pending == null) {
			return;
		}
		try {
			write(name, pending.snapshot);
			pending.completions.forEach(completion -> completion.complete(null));
		} catch (RuntimeException exception) {
			pending.completions.forEach(completion -> completion.completeExceptionally(exception));
		}
	}

	private void write(String name, String snapshot) {
		try {
			saveAtomic(resolveDataFile(name), snapshot);
		} catch (IOException exception) {
			writeFailure.compareAndSet(null, exception);
			logger.log(Level.SEVERE, "Could not save " + name + "; persistent state may be stale", exception);
			throw new CompletionException(exception);
		}
	}

	private Path resolveDataFile(String name) {
		Objects.requireNonNull(name, "name");
		Path resolved = dataDirectory.resolve(name).normalize();
		if (!resolved.getParent().equals(dataDirectory) || !name.endsWith(".yml")) {
			throw new IllegalArgumentException("Invalid persistence file name: " + name);
		}
		return resolved;
	}

	private static void saveAtomic(Path destination, String value) throws IOException {
		Path temp = destination.resolveSibling(destination.getFileName() + ".tmp");
		Files.writeString(temp, value, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		try {
			Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException exception) {
			Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private void ensureOpen() {
		if (closed.get()) {
			throw new IllegalStateException("Persistence is already closed");
		}
	}

	private void throwRecordedFailure() {
		Throwable failure = writeFailure.getAndSet(null);
		if (failure != null) {
			throw new IllegalStateException("A queued persistence write failed; review the preceding console error",
					failure);
		}
	}

	@Override
	public void close() {
		if (closed.get()) {
			return;
		}
		try {
			flush();
		} catch (IllegalStateException exception) {
			logger.log(Level.SEVERE, "DragonAltar persistence did not flush cleanly during shutdown", exception);
		} finally {
			closed.set(true);
			writer.shutdown();
		}
		try {
			if (!writer.awaitTermination(5, TimeUnit.SECONDS)) {
				List<Runnable> abandoned = writer.shutdownNow();
				if (!abandoned.isEmpty()) {
					logger.severe("Persistence shutdown abandoned " + abandoned.size() + " queued write task(s)");
				}
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			writer.shutdownNow();
		}
		Throwable failure = writeFailure.get();
		if (failure != null) {
			logger.log(Level.SEVERE, "DragonAltar disabled after a persistence write failure", failure);
		}
	}

	private static ExecutorService newWriter() {
		return Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, "DragonAltar-Persistence");
			thread.setDaemon(true);
			return thread;
		});
	}

	private static final class PendingWrite {
		private String snapshot;
		private final List<CompletableFuture<Void>> completions = new ArrayList<>();

		private PendingWrite(String snapshot, CompletableFuture<Void> completion) {
			this.snapshot = snapshot;
			completions.add(completion);
		}
	}
}
