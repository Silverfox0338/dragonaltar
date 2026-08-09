package com.dragonaltar.command;

import java.util.List;

public record DestructiveActionPreview(String action, List<String> players, List<String> souls, String result,
		String cooldowns, String history, String undo) {
	public DestructiveActionPreview {
		players = List.copyOf(players);
		souls = List.copyOf(souls);
	}

	public String render(String token, long seconds) {
		return "DANGER — confirmation required" + "\nAction: " + action + "\nAffected player(s): " + display(players)
				+ "\nAffected soul(s): " + display(souls) + "\nResult: " + result + "\nCooldowns: " + cooldowns
				+ "\nHistory: " + history + "\nUndo: " + undo + "\nConfirm within " + seconds
				+ " seconds: /dragon confirm " + token + "\nCancel safely: /dragon cancel";
	}

	private static String display(List<String> values) {
		return values.isEmpty() ? "None" : String.join(", ", values);
	}
}
