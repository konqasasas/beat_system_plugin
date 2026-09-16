package dev.konqasasas.beat;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PluginDescriptorTest {
    @Test
    void pluginDescriptorTargetsSpigot262AndDeclaresMainClass() throws IOException {
        try (InputStream descriptor = getClass().getResourceAsStream("/plugin.yml")) {
            assertTrue(descriptor != null, "plugin.yml must be packaged as a resource");
            String yaml = new String(descriptor.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(yaml.contains("main: dev.konqasasas.beat.BeatPlugin"));
            assertTrue(yaml.contains("api-version: '26.2'"));
            assertTrue(yaml.contains("commands:"));
            assertTrue(yaml.contains("  beat:"));
        }
    }

    @Test
    void defaultRuntimeConfigurationResourcesArePackaged() {
        assertTrue(getClass().getResource("/config.yml") != null);
        assertTrue(getClass().getResource("/messages.yml") != null);
        assertTrue(getClass().getResource("/styles.yml") != null);
        assertTrue(getClass().getResource("/participants.json") != null);
        assertTrue(getClass().getResource("/admins.json") != null);
    }
}
