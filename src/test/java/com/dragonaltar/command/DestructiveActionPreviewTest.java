package com.dragonaltar.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DestructiveActionPreviewTest {
    @Test
    void rendersEveryRequiredSafetyFieldAndCommands() {
        String rendered = new DestructiveActionPreview(
                "force remove", List.of("PlayerA"), List.of("Rev"),
                "Rev enters dormant/pending reincarnation.", "Unchanged",
                "A removal event is appended.", "Can be reassigned."
        ).render("ABC234", 30);

        assertTrue(rendered.contains("Affected player(s): PlayerA"));
        assertTrue(rendered.contains("Affected soul(s): Rev"));
        assertTrue(rendered.contains("Result: Rev enters dormant/pending reincarnation."));
        assertTrue(rendered.contains("Cooldowns: Unchanged"));
        assertTrue(rendered.contains("History: A removal event is appended."));
        assertTrue(rendered.contains("Undo: Can be reassigned."));
        assertTrue(rendered.contains("/dragon confirm ABC234"));
        assertTrue(rendered.contains("/dragon cancel"));
    }
}
