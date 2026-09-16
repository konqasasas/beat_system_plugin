package dev.konqasasas.beat.configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

class ConfigurationResourcesTest {
    @Test
    void competitionUiMessagesAndStylesHaveDefaults() {
        Map<?, ?> messages = resource("/messages.yml");
        Map<?, ?> styles = resource("/styles.yml");
        Map<?, ?> config = resource("/config.yml");

        for (String color : new String[] {"brand", "primary", "success", "warning", "error"}) {
            assertPath(styles, "colors." + color);
        }
        assertPath(messages, "ui.actionbar.notification");
        assertPath(messages, "ui.bossbar.competition-time");
        assertPath(messages, "ui.bossbar.high-practice");
        assertPath(messages, "ui.high.status");
        assertPath(messages, "ui.time-attack.status");
        assertPath(messages, "ui.endurance.status");
        assertPath(messages, "ui.scoreboard.high-row");
        assertPath(messages, "ui.tab.time-attack-row");
        assertPath(messages, "commands.setup.help");
        assertPath(messages, "commands.debug.help");
        assertPath(messages, "setup-wand.selected");
        assertPath(messages, "player-visibility.admin-tab");
        for (String notification : new String[] {
                "high.practice-countdown", "high.practice-ending-countdown", "high.spot", "high.eliminated", "high.finished",
                "high.checkpoint-updated", "high.checkpoint-disabled",
                "high.time-limit-countdown", "high.time-limit-ended",
                "time-attack.countdown", "time-attack.personal-best", "time-attack.time-limit-countdown", "time-attack.eliminated",
                "time-attack.time-limit-ended",
                "time-attack.finished", "endurance.countdown", "endurance.goal",
                "endurance.time-limit-countdown", "endurance.time-limit-ended",
                "endurance.eliminated", "endurance.finished"
        }) {
            assertPath(messages, "notifications." + notification);
        }
        for (String commandMessage : new String[] {
                "no-permission", "unknown", "players.row", "player.updated",
                "whitelist.completed", "competition.status", "competition.help",
                "event.reset-confirm", "event.reset-completed", "event.help",
                "emergency.confirm", "emergency.help", "overall.row", "overall.help",
                "result.edited", "result.row-high", "result.row-ta", "result.row-endurance", "help"
        }) {
            assertPath(messages, "commands." + commandMessage);
        }
        for (String guiMessage : new String[] {
                "titles.main", "titles.players", "common.back", "common.next-page",
                "main.high", "main.emergency", "main-lore.high", "player.detail-lore",
                "result.high-lore", "result.announce", "result.announce-confirm-lore",
                "overall.row-lore", "whitelist.status-lore",
                "start.summary-lore", "emergency.collected", "error"
        }) {
            assertPath(messages, "gui." + guiMessage);
        }
        for (String configPath : new String[] {
                "countdowns.competition-start-ticks", "ui-update-ticks.action-bar",
                "ui-update-ticks.boss-bar", "ui-update-ticks.scoreboard", "ui-update-ticks.player-visibility",
                "confirmation-timeouts.dangerous-action-millis",
                "high-difficulty.practice-items.checkpoint.slot",
                "high-difficulty.practice-items.flight.slot"
        }) {
            assertPath(config, configPath);
        }
        for (String item : new String[] {
                "arrow", "barrier", "blaze-rod", "clock", "compass", "diamond-boots",
                "emerald", "ender-pearl", "gray-dye", "iron-bars", "iron-boots", "iron-door",
                "light-blue-concrete", "lime-concrete", "lime-dye", "nether-star", "note-block", "oak-door",
                "paper", "player-head", "red-concrete", "red-dye", "redstone-block",
                "redstone-torch", "tnt", "writable-book", "yellow-concrete", "yellow-dye"
        }) {
            assertPath(config, "gui.items." + item + ".material");
        }

        for (String competition : new String[] {"high", "high-practice", "high-prepare", "time-attack", "endurance"}) {
            assertPath(styles, "boss-bars." + competition + ".color");
            assertPath(styles, "boss-bars." + competition + ".style");
        }
        for (String sound : new String[] {
                "countdown", "high-spot", "high-practice-start", "high-practice-checkpoint",
                "high-practice-flight", "high-course", "high-goal", "ta-start", "ta-split",
                "ta-personal-best", "ta-finished", "player-visibility", "elimination", "time-limit-end",
                "endurance-progress", "endurance-zone", "endurance-goal"
        }) {
            assertPath(styles, "sounds." + sound + ".name");
            assertPath(styles, "sounds." + sound + ".volume");
            assertPath(styles, "sounds." + sound + ".pitch");
        }
        for (String row : new String[] {"high-row", "time-attack-row", "endurance-row"}) {
            assertScoreboardEntryIsVisible(messages, row);
        }
        assertNoLongHorizontalBars(messages);
        assertTrue(value(messages, "ui.actionbar.notification").toString().contains("{message}"));
        assertTrue(value(messages, "ui.time-attack.border").toString().contains("{border}"));
    }

    private static Map<?, ?> resource(String name) {
        try (InputStream stream = ConfigurationResourcesTest.class.getResourceAsStream(name)) {
            assertNotNull(stream, name);
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            return new Yaml(new SafeConstructor(options)).load(stream);
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void assertPath(Map<?, ?> root, String path) {
        assertNotNull(value(root, path), path);
    }

    private static Object value(Map<?, ?> root, String path) {
        Object value = root;
        for (String part : path.split("\\.")) {
            value = value instanceof Map<?, ?> map ? map.get(part) : null;
        }
        return value;
    }

    private static void assertScoreboardEntryIsVisible(Map<?, ?> messages, String row) {
        Object scoreboard = messages.get("ui");
        scoreboard = scoreboard instanceof Map<?, ?> map ? map.get("scoreboard") : null;
        Object value = scoreboard instanceof Map<?, ?> map ? map.get(row) : null;
        assertNotNull(value, "ui.scoreboard." + row);
        assertFalse(value.toString().startsWith("#"), "Minecraft hides scoreboard entries beginning with #");
    }

    private static void assertNoLongHorizontalBars(Object value) {
        if (value instanceof Map<?, ?> map) {
            map.values().forEach(ConfigurationResourcesTest::assertNoLongHorizontalBars);
        } else if (value instanceof Iterable<?> values) {
            values.forEach(ConfigurationResourcesTest::assertNoLongHorizontalBars);
        } else if (value != null) {
            assertFalse(value.toString().contains("――"), "Use ASCII hyphens in display text");
        }
    }
}
