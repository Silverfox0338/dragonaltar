package com.dragonaltar.command;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.security.SecureRandom;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfirmationServiceTest {
	@Test
	void tokenIsBoundToSenderOperationArgumentsAndSingleUse() {
		ConfirmationService service = new ConfirmationService();
		UUID sender = UUID.randomUUID();
		String token = service.issue(sender, "reset", List.of("souls"), Duration.ofSeconds(5));
		assertFalse(service.consume(UUID.randomUUID(), token, "reset", List.of("souls")));
		token = service.issue(sender, "reset", List.of("souls"), Duration.ofSeconds(5));
		assertFalse(service.consume(sender, token, "reset", List.of("players")));
		token = service.issue(sender, "reset", List.of("souls"), Duration.ofSeconds(5));
		assertTrue(service.consume(sender, token, "reset", List.of("souls")));
		assertFalse(service.consume(sender, token, "reset", List.of("souls")));
	}
	@Test
	void expiredTokenFailsWithoutTimingDependentSleep() {
		MutableClock clock = new MutableClock(Instant.parse("2026-08-08T00:00:00Z"));
		ConfirmationService service = new ConfirmationService(new SecureRandom(), clock);
		UUID sender = UUID.randomUUID();
		String token = service.issue(sender, "x", List.of(), Duration.ofSeconds(1));
		clock.advance(Duration.ofSeconds(1));
		assertFalse(service.consume(sender, token, "x", List.of()));
	}
	@Test
	void cancellationInvalidatesPendingToken() {
		ConfirmationService service = new ConfirmationService();
		UUID sender = UUID.randomUUID();
		String token = service.issue(sender, "reset", List.of("everything"), Duration.ofSeconds(30));
		assertTrue(service.cancel(sender));
		assertFalse(service.cancel(sender));
		assertFalse(service.consume(sender, token, "reset", List.of("everything")));
	}

	private static final class MutableClock extends Clock {
		private Instant instant;

		private MutableClock(Instant instant) {
			this.instant = instant;
		}

		private void advance(Duration duration) {
			instant = instant.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneId.of("UTC");
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}
}
