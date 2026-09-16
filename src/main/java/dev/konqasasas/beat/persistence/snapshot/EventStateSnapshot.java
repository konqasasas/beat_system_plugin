package dev.konqasasas.beat.persistence.snapshot;

import dev.konqasasas.beat.domain.WhitelistMode;
import dev.konqasasas.beat.domain.state.TournamentState;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record EventStateSnapshot(
        int schemaVersion,
        TournamentState tournamentState,
        WhitelistMode whitelistMode,
        Map<UUID, String> tournamentNames) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public EventStateSnapshot {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported event-state schema: " + schemaVersion);
        }
        Objects.requireNonNull(tournamentState, "tournamentState");
        Objects.requireNonNull(whitelistMode, "whitelistMode");
        Objects.requireNonNull(tournamentNames, "tournamentNames");
        LinkedHashMap<UUID, String> copy = new LinkedHashMap<>();
        tournamentNames.forEach((uuid, name) -> {
            Objects.requireNonNull(uuid, "tournamentNames UUID");
            Objects.requireNonNull(name, "tournamentNames name");
            if (name.isBlank()) {
                throw new IllegalArgumentException("Tournament names must not be blank");
            }
            copy.put(uuid, name);
        });
        tournamentNames = Map.copyOf(copy);
    }
}
