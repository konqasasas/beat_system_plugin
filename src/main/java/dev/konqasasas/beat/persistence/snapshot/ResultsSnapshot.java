package dev.konqasasas.beat.persistence.snapshot;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record ResultsSnapshot(
        int schemaVersion,
        boolean highConfirmed,
        boolean timeAttackConfirmed,
        boolean enduranceConfirmed,
        boolean overallConfirmed,
        List<PlayerResultSnapshot> players) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public ResultsSnapshot {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported results schema: " + schemaVersion);
        }
        Objects.requireNonNull(players, "players");
        Set<UUID> uniqueUuids = new HashSet<>();
        for (PlayerResultSnapshot player : players) {
            Objects.requireNonNull(player, "player result");
            if (!uniqueUuids.add(player.uuid())) {
                throw new IllegalArgumentException("Duplicate result UUID: " + player.uuid());
            }
        }
        players = List.copyOf(players);
    }
}
