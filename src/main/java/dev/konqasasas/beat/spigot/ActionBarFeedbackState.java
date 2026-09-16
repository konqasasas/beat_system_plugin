package dev.konqasasas.beat.spigot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class ActionBarFeedbackState {
    private final Map<UUID, Entry> entries = new HashMap<>();

    void show(UUID playerId, String message, long untilTick) {
        entries.put(playerId, new Entry(message, untilTick));
    }

    String activeMessage(UUID playerId, long tick) {
        Entry entry = entries.get(playerId);
        if (entry == null) return null;
        if (tick < entry.untilTick()) return entry.message();
        entries.remove(playerId);
        return null;
    }

    void clear() {
        entries.clear();
    }

    private record Entry(String message, long untilTick) {
    }
}
