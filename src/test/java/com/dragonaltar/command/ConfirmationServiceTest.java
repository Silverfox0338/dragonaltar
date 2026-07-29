package com.dragonaltar.command;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfirmationServiceTest {
    @Test void tokenIsBoundToSenderOperationArgumentsAndSingleUse() {
        ConfirmationService service=new ConfirmationService();UUID sender=UUID.randomUUID();
        String token=service.issue(sender,"reset",List.of("souls"),Duration.ofSeconds(5));
        assertFalse(service.consume(UUID.randomUUID(),token,"reset",List.of("souls")));
        token=service.issue(sender,"reset",List.of("souls"),Duration.ofSeconds(5));
        assertFalse(service.consume(sender,token,"reset",List.of("players")));
        token=service.issue(sender,"reset",List.of("souls"),Duration.ofSeconds(5));
        assertTrue(service.consume(sender,token,"reset",List.of("souls")));
        assertFalse(service.consume(sender,token,"reset",List.of("souls")));
    }
    @Test void expiredTokenFails() throws Exception {
        ConfirmationService service=new ConfirmationService();UUID sender=UUID.randomUUID();
        String token=service.issue(sender,"x",List.of(),Duration.ofMillis(1));Thread.sleep(5);
        assertFalse(service.consume(sender,token,"x",List.of()));
    }
}
