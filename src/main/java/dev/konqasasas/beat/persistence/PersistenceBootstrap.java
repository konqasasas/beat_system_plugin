package dev.konqasasas.beat.persistence;

import dev.konqasasas.beat.domain.WhitelistMode;
import dev.konqasasas.beat.domain.state.TournamentState;
import dev.konqasasas.beat.persistence.snapshot.EventStateSnapshot;
import dev.konqasasas.beat.persistence.snapshot.ResultsSnapshot;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PersistenceBootstrap {
    private final Path pluginDirectory;

    public PersistenceBootstrap(Path pluginDirectory) {
        this.pluginDirectory = Objects.requireNonNull(pluginDirectory, "pluginDirectory")
                .toAbsolutePath()
                .normalize();
    }

    public PersistenceContext initialize() throws PersistenceException {
        Path dataDirectory = pluginDirectory.resolve("data");
        Path backupDirectory = pluginDirectory.resolve("backups");
        createDirectories(dataDirectory, backupDirectory);

        EventStateRepository eventStates = new JsonEventStateRepository(
                dataDirectory.resolve("event-state.json"));
        ResultsRepository results = new JsonResultsRepository(
                dataDirectory.resolve("results.json"));

        var loadedEventState = eventStates.load();
        var loadedResults = results.load();
        EventStateSnapshot eventState = loadedEventState.orElseGet(() -> new EventStateSnapshot(
                EventStateSnapshot.CURRENT_SCHEMA_VERSION,
                TournamentState.WAITING,
                WhitelistMode.ADMIN_ONLY,
                Map.of()));
        ResultsSnapshot resultState = loadedResults.orElseGet(() -> new ResultsSnapshot(
                ResultsSnapshot.CURRENT_SCHEMA_VERSION,
                false,
                false,
                false,
                false,
                List.of()));

        if (loadedEventState.isEmpty()) {
            eventStates.save(eventState);
        }
        if (loadedResults.isEmpty()) {
            results.save(resultState);
        }

        return new PersistenceContext(
                eventStates,
                results,
                new FileBackupService(backupDirectory),
                eventState,
                resultState);
    }

    private void createDirectories(Path dataDirectory, Path backupDirectory)
            throws PersistenceException {
        try {
            Files.createDirectories(pluginDirectory);
            Files.createDirectories(dataDirectory);
            Files.createDirectories(backupDirectory);
            Files.createDirectories(pluginDirectory.resolve("maps"));
            Files.createDirectories(pluginDirectory.resolve("logs"));
        } catch (IOException exception) {
            throw new PersistenceException(
                    "Failed to initialize plugin directories at " + pluginDirectory, exception);
        }
    }
}
