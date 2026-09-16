package dev.konqasasas.beat.setup;

import dev.konqasasas.beat.map.BlockRegion;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SelectionService {
    private final Map<UUID, SelectionSnapshot> selections = new HashMap<>();

    public synchronized SelectionSnapshot setPos1(UUID playerId, SelectionPoint point) {
        SelectionSnapshot updated = new SelectionSnapshot(point, null);
        selections.put(playerId, updated);
        return updated;
    }

    public synchronized SelectionSnapshot setPos2(UUID playerId, SelectionPoint point)
            throws SelectionException {
        SelectionSnapshot current = selections.getOrDefault(playerId, new SelectionSnapshot(null, null));
        if (current.pos1() != null && !current.pos1().world().equals(point.world())) {
            throw new SelectionException("Pos1とは別のワールドをPos2に設定できません。Pos1は保持されています。");
        }
        SelectionSnapshot updated = new SelectionSnapshot(current.pos1(), point);
        selections.put(playerId, updated);
        return updated;
    }

    public synchronized SelectionSnapshot selection(UUID playerId) {
        return selections.getOrDefault(playerId, new SelectionSnapshot(null, null));
    }

    public synchronized void clear(UUID playerId) {
        selections.remove(playerId);
    }

    public synchronized void clearAll() {
        selections.clear();
    }

    public synchronized BlockRegion requireRegion(UUID playerId) throws SelectionException {
        SelectionSnapshot selection = selection(playerId);
        if (selection.pos1() == null || selection.pos2() == null) {
            throw new SelectionException("Pos1とPos2を両方設定してください。");
        }
        SelectionPoint first = selection.pos1();
        SelectionPoint second = selection.pos2();
        if (!first.world().equals(second.world())) {
            throw new SelectionException("Pos1とPos2のワールドが一致しません。");
        }
        if (first.y() != second.y()) {
            throw new SelectionException("水平な範囲だけ登録できます。Pos1とPos2のYを同じにしてください。");
        }
        return new BlockRegion(first.world(), first.y(), first.x(), second.x(), first.z(), second.z());
    }
}
