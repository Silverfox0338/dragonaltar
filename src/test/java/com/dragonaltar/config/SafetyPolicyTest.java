package com.dragonaltar.config;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SafetyPolicyTest {
    @Test void betaAllowsAndProductionRequiresExplicitOverride(){assertTrue(SafetyPolicy.destructiveAllowed(ServerMode.BETA,false));assertFalse(SafetyPolicy.destructiveAllowed(ServerMode.PRODUCTION,false));assertTrue(SafetyPolicy.destructiveAllowed(ServerMode.PRODUCTION,true));}
}
