package com.dragonaltar.command;

import com.dragonaltar.DragonAltarPlugin;
import org.bukkit.command.CommandSender;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Owns destructive-command confirmation state, stale-state rejection,
 * execution, and audit outcomes.
 */
final class DestructiveCommandCoordinator {
	private final DragonAltarPlugin plugin;
	private final Function<CommandSender, UUID> senderIds;
	private final BiFunction<String, List<String>, DestructiveActionPreview> previews;
	private final BiFunction<String, List<String>, String> fingerprints;
	private final Map<UUID, PendingAction> pending = new HashMap<>();

	DestructiveCommandCoordinator(DragonAltarPlugin plugin, Function<CommandSender, UUID> senderIds,
			BiFunction<String, List<String>, DestructiveActionPreview> previews,
			BiFunction<String, List<String>, String> fingerprints) {
		this.plugin = plugin;
		this.senderIds = senderIds;
		this.previews = previews;
		this.fingerprints = fingerprints;
	}

	void request(CommandSender sender, String operation, List<String> arguments, Duration duration, Runnable action) {
		UUID id = senderIds.apply(sender);
		List<String> boundArguments = List.copyOf(arguments);
		String token = plugin.confirmations().issue(id, operation, boundArguments, duration);
		DestructiveActionPreview preview = previews.apply(operation, boundArguments);
		pending.put(id, new PendingAction(operation, boundArguments, preview,
				fingerprints.apply(operation, boundArguments), action));
		plugin.audit().record("DESTRUCTIVE_CONFIRMATION_ISSUED", id.toString(),
				"admin=" + sender.getName() + " operation=" + operation + " arguments=" + boundArguments + " expires="
						+ duration.toSeconds() + "s");
		sender.sendMessage(preview.render(token, duration.toSeconds()));
	}

	void confirm(CommandSender sender, String token) {
		UUID id = senderIds.apply(sender);
		PendingAction action = pending.remove(id);
		if (action == null || !plugin.confirmations().consume(id, token, action.operation(), action.arguments())) {
			plugin.audit().record("DESTRUCTIVE_CONFIRMATION_REJECTED", id.toString(),
					"admin=" + sender.getName() + " invalid-or-expired");
			throw new IllegalArgumentException("Invalid or expired confirmation; no changes were made");
		}
		if (!action.stateFingerprint().equals(fingerprints.apply(action.operation(), action.arguments()))) {
			plugin.audit().record("DESTRUCTIVE_CONFIRMATION_REJECTED", id.toString(),
					"admin=" + sender.getName() + " operation=" + action.operation() + " state-changed-after-preview");
			throw new IllegalStateException(
					"Relevant server state changed after the preview; no changes were made. Review and run the original command again");
		}
		try {
			action.action().run();
			plugin.audit().record("DESTRUCTIVE_ACTION_CONFIRMED", id.toString(),
					"confirmed-by=" + sender.getName() + " operation=" + action.operation() + " arguments="
							+ action.arguments() + " result=" + action.preview().result());
			sender.sendMessage("Confirmed by " + sender.getName() + ": " + action.preview().action() + " completed.");
		} catch (RuntimeException exception) {
			plugin.audit().record("DESTRUCTIVE_ACTION_FAILED", id.toString(), "confirmed-by=" + sender.getName()
					+ " operation=" + action.operation() + " error=" + String.valueOf(exception.getMessage()));
			throw exception;
		}
	}

	void cancel(CommandSender sender) {
		UUID id = senderIds.apply(sender);
		PendingAction action = pending.remove(id);
		boolean token = plugin.confirmations().cancel(id);
		if (action == null && !token) {
			sender.sendMessage("No destructive action is awaiting confirmation.");
			return;
		}
		plugin.audit().record("DESTRUCTIVE_CONFIRMATION_CANCELLED", id.toString(),
				"admin=" + sender.getName() + " operation=" + (action == null ? "unknown" : action.operation()));
		sender.sendMessage("Pending destructive action cancelled. No changes were made.");
	}

	private record PendingAction(String operation, List<String> arguments, DestructiveActionPreview preview,
			String stateFingerprint, Runnable action) {
	}
}
