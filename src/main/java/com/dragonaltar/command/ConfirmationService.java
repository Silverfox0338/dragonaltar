package com.dragonaltar.command;

import java.security.SecureRandom;
import java.time.*;
import java.util.*;

public final class ConfirmationService {
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final SecureRandom random = new SecureRandom();
    private final Map<UUID, Confirmation> pending = new HashMap<>();

    public synchronized String issue(UUID sender, String operation, List<String> arguments, Duration ttl) {
        StringBuilder token = new StringBuilder(6);
        for (int i = 0; i < 6; i++) token.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        pending.put(sender, new Confirmation(token.toString(), operation, List.copyOf(arguments), Instant.now().plus(ttl)));
        return token.toString();
    }
    public synchronized boolean consume(UUID sender, String token, String operation, List<String> arguments) {
        Confirmation c = pending.remove(sender);
        return c != null && Instant.now().isBefore(c.expires()) && c.token().equalsIgnoreCase(token)
                && c.operation().equals(operation) && c.arguments().equals(arguments);
    }
    public synchronized boolean cancel(UUID sender) { return pending.remove(sender) != null; }
    private record Confirmation(String token, String operation, List<String> arguments, Instant expires) {}
}
