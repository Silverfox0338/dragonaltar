package com.dragonaltar.audit;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

public final class AuditService implements AutoCloseable {
    private final JavaPlugin plugin;
    private final java.util.concurrent.ExecutorService writer=java.util.concurrent.Executors.newSingleThreadExecutor(r->{Thread thread=new Thread(r,"DragonAltar-Audit");thread.setDaemon(true);return thread;});
    public AuditService(JavaPlugin plugin) { this.plugin = plugin; }
    public synchronized void record(String action, String actor, String detail) {
        String month = YearMonth.now(ZoneOffset.UTC) + ".log";
        String line = DateTimeFormatter.ISO_INSTANT.format(Instant.now()) + "\t" + action + "\t" + actor + "\t" + detail.replace('\n', ' ') + System.lineSeparator();
        writer.execute(()->{try {
            Path path = plugin.getDataFolder().toPath().resolve("logs").resolve(month);
            Files.writeString(path, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) { plugin.getLogger().log(Level.SEVERE, "Could not write audit log", e); }});
    }
    @Override public void close(){writer.shutdown();try{if(!writer.awaitTermination(5,java.util.concurrent.TimeUnit.SECONDS))writer.shutdownNow();}catch(InterruptedException e){Thread.currentThread().interrupt();writer.shutdownNow();}}
}
