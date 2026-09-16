package dev.konqasasas.beat.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.konqasasas.beat.domain.WhitelistMode;
import dev.konqasasas.beat.domain.state.TournamentState;
import dev.konqasasas.beat.persistence.snapshot.EventStateSnapshot;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonEventStateRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void missingStateIsEmptyAndSavedStateRoundTrips() throws Exception {
        Path file = temporaryDirectory.resolve("data/event-state.json");
        JsonEventStateRepository repository = new JsonEventStateRepository(file);
        assertFalse(repository.load().isPresent());
        UUID uuid = UUID.nameUUIDFromBytes("PlayerA".getBytes(StandardCharsets.UTF_8));
        EventStateSnapshot expected = new EventStateSnapshot(
                EventStateSnapshot.CURRENT_SCHEMA_VERSION,
                TournamentState.TA_READY,
                WhitelistMode.ALL,
                Map.of(uuid, "PlayerA"));

        repository.save(expected);

        assertEquals(expected, repository.load().orElseThrow());
        try (Stream<Path> files = Files.list(file.getParent())) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    void invalidJsonFailsWithoutChangingTheFile() throws Exception {
        Path file = temporaryDirectory.resolve("event-state.json");
        String invalidJson = "{ invalid";
        Files.writeString(file, invalidJson, StandardCharsets.UTF_8);
        JsonEventStateRepository repository = new JsonEventStateRepository(file);

        assertThrows(PersistenceException.class, repository::load);

        assertEquals(invalidJson, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void unsupportedSchemaIsRejected() throws Exception {
        Path file = temporaryDirectory.resolve("event-state.json");
        Files.writeString(file, """
                {
                  "schemaVersion": 99,
                  "tournamentState": "WAITING",
                  "whitelistMode": "ADMIN_ONLY",
                  "tournamentNames": {}
                }
                """, StandardCharsets.UTF_8);

        JsonEventStateRepository repository = new JsonEventStateRepository(file);

        assertThrows(PersistenceException.class, repository::load);
    }
}
