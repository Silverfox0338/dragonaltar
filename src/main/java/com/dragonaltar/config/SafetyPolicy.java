package com.dragonaltar.config;
public final class SafetyPolicy {
    private SafetyPolicy(){}
    public static boolean destructiveAllowed(ServerMode mode,boolean configuredOverride){return mode==ServerMode.BETA||configuredOverride;}
}
