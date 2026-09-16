package dev.konqasasas.beat.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.konqasasas.beat.domain.WhitelistMode;
import dev.konqasasas.beat.domain.state.TournamentState;
import dev.konqasasas.beat.persistence.snapshot.EventStateSnapshot;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PersistenceBootstrapTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void firstStartCreatesLayoutAndSafeDefaultState() throws Exception {
        Path pluginDirectory = temporaryDirectory.resolve("BEAT");

        PersistenceContext context = new PersistenceBootstrap(pluginDirectory).initialize();

        assertEquals(TournamentState.WAITING, context.eventState().tournamentState());
        assertEquals(WhitelistMode.ADMIN_ONLY, context.eventState().whitelistMode());
        assertFalse(context.resultState().overallConfirmed());
        assertTrue(Files.isRegularFile(pluginDirectory.resolve("data/event-state.json")));
        assertTrue(Files.isRegularFile(pluginDirectory.resolve("data/results.json")));
        assertTrue(Files.isDirectory(pluginDirectory.resolve("maps")));
        assertTrue(Files.isDirectory(pluginDirectory.resolve("backups")));
        assertTrue(Files.isDirectory(pluginDirectory.resolve("logs")));
    }

    @Test
    void laterStartRestoresPreviouslySavedState() throws Exception {
        Path pluginDirectory = temporaryDirectory.resolve("BEAT");
        PersistenceContext firstStart = new PersistenceBootstrap(pluginDirectory).initialize();
        firstStart.eventStates().save(new EventStateSnapshot(
                EventStateSnapshot.CURRENT_SCHEMA_VERSION,
                TournamentState.TA_READY,
                WhitelistMode.ALL,
                Map.of()));

        PersistenceContext restored = new PersistenceBootstrap(pluginDirectory).initialize();

        assertEquals(TournamentState.TA_READY, restored.eventState().tournamentState());
        assertEquals(WhitelistMode.ALL, restored.eventState().whitelistMode());
    }
}
