package dev.konqasasas.beat.map.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.konqasasas.beat.map.MapFixtures;
import dev.konqasasas.beat.persistence.PersistenceException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlMapConfigurationRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void allCompetitionMapsRoundTripThroughHumanReadableYaml() throws Exception {
        YamlMapConfigurationRepository repository =
                new YamlMapConfigurationRepository(temporaryDirectory);

        repository.saveHigh(MapFixtures.validHigh());
        repository.saveTimeAttack(MapFixtures.validTimeAttack());
        repository.saveEndurance(MapFixtures.validEndurance());

        assertEquals(MapFixtures.validHigh(), repository.loadHigh().orElseThrow());
        assertEquals(MapFixtures.validTimeAttack(), repository.loadTimeAttack().orElseThrow());
        assertEquals(MapFixtures.validEndurance(), repository.loadEndurance().orElseThrow());
        String yaml = Files.readString(
                temporaryDirectory.resolve("time-attack.yml"), StandardCharsets.UTF_8);
        assertTrue(yaml.contains("schema-version: 1"));
        assertTrue(yaml.contains("min-x:"));
        try (Stream<Path> files = Files.list(temporaryDirectory)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    void malformedYamlIsRejectedWithoutBeingReplaced() throws Exception {
        Path file = temporaryDirectory.resolve("time-attack.yml");
        String malformed = "schema-version: [";
        Files.writeString(file, malformed, StandardCharsets.UTF_8);
        YamlMapConfigurationRepository repository =
                new YamlMapConfigurationRepository(temporaryDirectory);

        assertThrows(PersistenceException.class, repository::loadTimeAttack);

        assertEquals(malformed, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void firstOpenCreatesAllThreeMapFiles() throws Exception {
        MapConfigurationService service = MapConfigurationService.open(temporaryDirectory);

        assertTrue(service.high().courses().isEmpty());
        assertTrue(Files.isRegularFile(temporaryDirectory.resolve("high-difficulty.yml")));
        assertTrue(Files.isRegularFile(temporaryDirectory.resolve("time-attack.yml")));
        assertTrue(Files.isRegularFile(temporaryDirectory.resolve("endurance.yml")));
    }

    @Test
    void failedReloadKeepsAllLastKnownGoodMaps() throws Exception {
        MapConfigurationService service = MapConfigurationService.open(temporaryDirectory);
        service.saveHigh(MapFixtures.validHigh());
        service.saveTimeAttack(MapFixtures.validTimeAttack());
        service.saveEndurance(MapFixtures.validEndurance());
        Files.writeString(
                temporaryDirectory.resolve("time-attack.yml"),
                "invalid: [",
                StandardCharsets.UTF_8);

        assertThrows(PersistenceException.class, service::reload);

        assertEquals(MapFixtures.validHigh(), service.high());
        assertEquals(MapFixtures.validTimeAttack(), service.timeAttack());
        assertEquals(MapFixtures.validEndurance(), service.endurance());
    }
}
