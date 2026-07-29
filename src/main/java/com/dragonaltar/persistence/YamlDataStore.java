package com.dragonaltar.persistence;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public final class YamlDataStore implements AutoCloseable {
    private final JavaPlugin plugin;
    private final Path dataDirectory;
    private final AtomicReference<Throwable> writeFailure = new AtomicReference<>();
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "DragonAltar-Persistence"); t.setDaemon(true); return t;
    });

    public YamlDataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataDirectory = plugin.getDataFolder().toPath().resolve("data");
    }

    public void initialize() throws IOException {
        Files.createDirectories(dataDirectory);
        Files.createDirectories(plugin.getDataFolder().toPath().resolve("backups"));
        Files.createDirectories(plugin.getDataFolder().toPath().resolve("logs"));
        Files.createDirectories(plugin.getDataFolder().toPath().resolve("schematics"));
        for (String name : new String[]{"event.yml", "altar-state.yml", "souls.yml", "players.yml",
                "rituals.yml", "cooldowns.yml", "pending-transfers.yml", "consequences.yml"}) {
            Path path = dataDirectory.resolve(name);
            if (Files.notExists(path)) {
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.set("data-version", 1);
                saveAtomic(path, yaml.saveToString());
            }
        }
    }

    public synchronized YamlConfiguration load(String name) {
        return YamlConfiguration.loadConfiguration(dataDirectory.resolve(name).toFile());
    }

    public CompletableFuture<Void> save(String name, YamlConfiguration yaml) {
        String snapshot = yaml.saveToString();
        return CompletableFuture.runAsync(() -> {
            try { saveAtomic(dataDirectory.resolve(name), snapshot); }
            catch (IOException e) {
                writeFailure.compareAndSet(null,e);
                plugin.getLogger().log(Level.SEVERE, "Could not save " + name + "; persistent state may be stale", e);
                throw new CompletionException(e);
            }
        }, writer);
    }

    public void saveNow(String name, YamlConfiguration yaml) throws IOException {
        saveAtomic(dataDirectory.resolve(name), yaml.saveToString());
    }
    public void flush(){
        try{writer.submit(()->{}).get(10,TimeUnit.SECONDS);}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("Persistence flush interrupted",e);}
        catch(ExecutionException|TimeoutException e){throw new IllegalStateException("Persistence flush failed",e);}
        Throwable failure=writeFailure.getAndSet(null);
        if(failure!=null)throw new IllegalStateException("A queued persistence write failed; review the preceding console error",failure);
    }

    private static void saveAtomic(Path destination, String value) throws IOException {
        Path temp = destination.resolveSibling(destination.getFileName() + ".tmp");
        Files.writeString(temp, value, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        try {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override public void close() {
        writer.shutdown();
        try { if (!writer.awaitTermination(5, TimeUnit.SECONDS)) writer.shutdownNow(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); writer.shutdownNow(); }
        Throwable failure=writeFailure.get();
        if(failure!=null)plugin.getLogger().log(Level.SEVERE,"DragonAltar disabled after a persistence write failure",failure);
    }
}
