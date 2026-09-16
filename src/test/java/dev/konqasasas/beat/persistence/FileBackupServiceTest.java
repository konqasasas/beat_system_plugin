package dev.konqasasas.beat.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileBackupServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void backsUpOnlyExistingFilesWithoutOverwritingEarlierBackups() throws Exception {
        Path data = temporaryDirectory.resolve("data");
        Files.createDirectories(data);
        Path eventState = data.resolve("event-state.json");
        Path results = data.resolve("results.json");
        Files.writeString(eventState, "event", StandardCharsets.UTF_8);
        Files.writeString(results, "results", StandardCharsets.UTF_8);
        Clock clock = Clock.fixed(Instant.parse("2026-09-12T03:00:00Z"), ZoneOffset.UTC);
        FileBackupService backups = new FileBackupService(
                temporaryDirectory.resolve("backups"), clock);

        List<Path> first = backups.backupExisting(
                List.of(eventState, results, data.resolve("missing.json")),
                "event-reset");
        List<Path> second = backups.backupExisting(List.of(eventState), "event-reset");

        assertEquals(2, first.size());
        assertEquals(1, second.size());
        assertTrue(first.getFirst().getFileName().toString()
                .startsWith("20260912-030000-000-event-reset-"));
        assertTrue(second.getFirst().getFileName().toString().endsWith(".2"));
        assertEquals("event", Files.readString(first.getFirst(), StandardCharsets.UTF_8));
        assertEquals("event", Files.readString(second.getFirst(), StandardCharsets.UTF_8));
    }
}
