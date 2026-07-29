package com.dragonaltar.ability;
public record AbilityResult(boolean success, String message) {
    public static AbilityResult ok() { return new AbilityResult(true, ""); }
    public static AbilityResult fail(String message) { return new AbilityResult(false, message); }
}
