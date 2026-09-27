package dev.konqasasas.beat.spigot;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

final class FirstJoinTracker {
    private final Set<UUID> pending = new HashSet<>();

    void recordSpawn(UUID playerId, boolean hasPlayedBefore) {
        if (hasPlayedBefore) pending.remove(playerId);
        else pending.add(playerId);
    }

    boolean consumeJoin(UUID playerId) {
        return pending.remove(playerId);
    }
}
