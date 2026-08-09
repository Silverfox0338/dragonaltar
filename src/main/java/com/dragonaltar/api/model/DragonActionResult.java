package com.dragonaltar.api.model;

/** Result of an action that passed through DragonAltar's normal safeguards. */
public record DragonActionResult(boolean success, String message) {
	public DragonActionResult {
		message = message == null ? "" : message;
	}

	public static DragonActionResult ok() {
		return new DragonActionResult(true, "");
	}

	public static DragonActionResult failure(String message) {
		return new DragonActionResult(false, message);
	}
}
