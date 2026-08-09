package com.dragonaltar.integration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ScaledEnderDragonIntegrationTest {
	@Test
	void recognizesOnlySedKillAndConsumesAdministrativeMarker() {
		ScaledEnderDragonIntegration integration = new ScaledEnderDragonIntegration();
		integration.observeCommand("/sed info");
		assertEquals("COMBAT", integration.completionMethod());
		integration.observeCommand("sed kill");
		assertEquals("SED_KILL", integration.completionMethod());
		assertEquals("COMBAT", integration.completionMethod());
	}
}
