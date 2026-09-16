package dev.konqasasas.beat.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.konqasasas.beat.persistence.snapshot.EnduranceResultSnapshot;
import dev.konqasasas.beat.persistence.snapshot.HighResultSnapshot;
import dev.konqasasas.beat.persistence.snapshot.OverallResultSnapshot;
import dev.konqasasas.beat.persistence.snapshot.PlayerResultSnapshot;
import dev.konqasasas.beat.persistence.snapshot.ResultsSnapshot;
import dev.konqasasas.beat.persistence.snapshot.TimeAttackResultSnapshot;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonResultsRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void completeResultSchemaRoundTripsIncludingNullableTaValues() throws Exception {
        UUID uuid = UUID.nameUUIDFromBytes("PlayerA".getBytes(StandardCharsets.UTF_8));
        PlayerResultSnapshot player = new PlayerResultSnapshot(
                uuid,
                "PlayerA",
                true,
                true,
                true,
                false,
                false,
                new HighResultSnapshot(2, 900, 20_000L),
                new TimeAttackResultSnapshot(1, 580L, 10_000L, 7L, null, 400L),
                new EnduranceResultSnapshot(3, 88, 30_000L, true, true, true),
                new OverallResultSnapshot(1, 6, 2, 1, 3, true));
        ResultsSnapshot expected = new ResultsSnapshot(
                ResultsSnapshot.CURRENT_SCHEMA_VERSION,
                true,
                true,
                true,
                true,
                List.of(player));
        Path file = temporaryDirectory.resolve("data/results.json");
        JsonResultsRepository repository = new JsonResultsRepository(file);

        repository.save(expected);

        assertEquals(expected, repository.load().orElseThrow());
        String json = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"pbRecordSequence\": 7"));
        assertTrue(json.contains("\"pbSplit1Ticks\": null"));
        assertTrue(json.contains("\"participatedTa\": true"));
    }
}
