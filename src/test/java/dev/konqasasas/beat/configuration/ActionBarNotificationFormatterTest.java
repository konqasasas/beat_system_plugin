package dev.konqasasas.beat.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ActionBarNotificationFormatterTest {
    @Test
    void sharedLabelsUseTheSameColorsAcrossNotifications() {
        assertEquals(
                "\u00a7bSplit\u00a7r 1 12.30\u00a77 ｜ \u00a7r\u00a7aPB\u00a7r 11.90",
                ActionBarNotificationFormatter.colorize("Split 1 12.30 ｜ PB 11.90"));
        assertEquals(
                "\u00a7eTime\u00a7r 12.30\u00a77 ｜ \u00a7r\u00a7aPB\u00a7r 11.90\u00a77 ｜ \u00a7r+0.40",
                ActionBarNotificationFormatter.colorize("Time 12.30 ｜ PB 11.90 ｜ +0.40"));
        assertEquals(
                "\u00a7eProgress\u00a7r 014 到達\u00a77 ｜ \u00a7r\u00a7b#02\u00a7r",
                ActionBarNotificationFormatter.colorize("Progress 014 到達 ｜ #02"));
    }

    @Test
    void existingLegacyColorFormattingTakesPriority() {
        assertEquals("\u00a7dPB 10.00", ActionBarNotificationFormatter.colorize("\u00a7dPB 10.00"));
        assertEquals("&dPB 10.00", ActionBarNotificationFormatter.colorize("&dPB 10.00"));
    }
}
