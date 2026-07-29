package com.dragonaltar.eligibility;
public record EligibilitySnapshot(boolean online,boolean gameMode,boolean playtime,boolean requiredPermission,
                                  boolean notExcluded,boolean notDragonborn,boolean notAfk,boolean notVanished,
                                  boolean alive,boolean joinGrace) {}
