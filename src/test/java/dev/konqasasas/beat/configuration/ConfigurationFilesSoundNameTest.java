package dev.konqasasas.beat.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConfigurationFilesSoundNameTest {
    @Test
    void legacyAndNamespacedSoundNamesNormalizeEqually() {
        assertEquals(
                ConfigurationFiles.normalizeSoundName("ENTITY_EXPERIENCE_ORB_PICKUP"),
                ConfigurationFiles.normalizeSoundName("minecraft:entity.experience_orb.pickup"));
    }
}
