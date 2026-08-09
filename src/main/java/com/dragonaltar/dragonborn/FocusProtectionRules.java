package com.dragonaltar.dragonborn;

import java.util.List;
import java.util.Locale;

public final class FocusProtectionRules {
	private FocusProtectionRules() {
	}

	public static boolean blocksCommand(String command, List<String> blockedPrefixes) {
		String normalized = normalize(command);
		if (normalized.isEmpty())
			return false;
		String alias = withoutNamespace(normalized);
		for (String configured : blockedPrefixes) {
			String prefix = normalize(configured);
			if (!prefix.isEmpty() && (matches(normalized, prefix) || matches(alias, prefix)))
				return true;
		}
		return false;
	}

	private static boolean matches(String command, String prefix) {
		return command.equals(prefix) || command.startsWith(prefix + " ");
	}

	private static String withoutNamespace(String command) {
		int space = command.indexOf(' ');
		String label = space < 0 ? command : command.substring(0, space);
		int colon = label.indexOf(':');
		return colon < 0 ? command : label.substring(colon + 1) + (space < 0 ? "" : command.substring(space));
	}

	private static String normalize(String value) {
		if (value == null)
			return "";
		return value.strip().replaceFirst("^/+", "").replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}
}
