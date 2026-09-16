package dev.konqasasas.beat.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SelectionServiceTest {
    private final SelectionService selections = new SelectionService();
    private final UUID player = UUID.randomUUID();

    @Test
    void normalizesHorizontalSelection() throws Exception {
        selections.setPos1(player, new SelectionPoint("beat", 8, 64, 10));
        selections.setPos2(player, new SelectionPoint("beat", 2, 64, -4));
        var region = selections.requireRegion(player);
        assertEquals(2, region.minX());
        assertEquals(8, region.maxX());
        assertEquals(-4, region.minZ());
        assertEquals(10, region.maxZ());
    }

    @Test
    void rejectsDifferentWorldAndKeepsPos1() {
        var pos1 = new SelectionPoint("beat", 1, 64, 1);
        selections.setPos1(player, pos1);
        assertThrows(SelectionException.class,
                () -> selections.setPos2(player, new SelectionPoint("other", 2, 64, 2)));
        assertEquals(pos1, selections.selection(player).pos1());
    }

    @Test
    void differingYIsAllowedUntilRegistration() throws Exception {
        selections.setPos1(player, new SelectionPoint("beat", 1, 64, 1));
        selections.setPos2(player, new SelectionPoint("beat", 2, 65, 2));
        assertThrows(SelectionException.class, () -> selections.requireRegion(player));
    }

    @Test
    void clearRemovesBothPositions() throws Exception {
        selections.setPos1(player, new SelectionPoint("beat", 1, 64, 1));
        selections.setPos2(player, new SelectionPoint("beat", 2, 64, 2));
        selections.clear(player);
        assertThrows(SelectionException.class, () -> selections.requireRegion(player));
    }

    @Test
    void clearAllRemovesEveryAdministratorsSelection() {
        UUID another = UUID.randomUUID();
        selections.setPos1(player, new SelectionPoint("beat", 1, 64, 1));
        selections.setPos1(another, new SelectionPoint("beat", 2, 64, 2));
        selections.clearAll();
        assertEquals(null, selections.selection(player).pos1());
        assertEquals(null, selections.selection(another).pos1());
    }
}
