package dev.konqasasas.beat.spigot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActionBarFeedbackStateTest {
    @Test
    void keepsNotificationFullyRefreshableUntilTheExactSwitchTick() {
        UUID player = UUID.randomUUID();
        ActionBarFeedbackState feedback = new ActionBarFeedbackState();
        feedback.show(player, "notification", 60L);

        assertEquals("notification", feedback.activeMessage(player, 59L));
        assertNull(feedback.activeMessage(player, 60L));
        assertNull(feedback.activeMessage(player, 61L));
    }
}
