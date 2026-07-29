package com.dragonaltar.integration;

import java.util.Locale;

public final class ScaledEnderDragonIntegration {
    private volatile long sedKillObservedAt;
    public void observeCommand(String command){
        String normalized=command.startsWith("/")?command.substring(1):command;normalized=normalized.trim().toLowerCase(Locale.ROOT);
        if(normalized.equals("sed kill")||normalized.startsWith("sed kill "))sedKillObservedAt=System.currentTimeMillis();
    }
    public String completionMethod(){
        if(System.currentTimeMillis()-sedKillObservedAt<=10_000){sedKillObservedAt=0;return "SED_KILL";}
        return "COMBAT";
    }
}
