package dev.konqasasas.beat.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.konqasasas.beat.map.BlockRegion;
import dev.konqasasas.beat.map.EnduranceProgressPoint;
import dev.konqasasas.beat.map.persistence.MapConfigurationService;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MapSetupServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void highSpotIsAutomaticallyNumberedAndPersisted() throws Exception {
        MapConfigurationService maps = MapConfigurationService.open(temporaryDirectory);
        MapSetupService setup = new MapSetupService(maps);
        BlockRegion first = region(0);
        BlockRegion second = region(10);

        assertEquals(1, setup.addHighSpot(3, first));
        assertEquals(2, setup.addHighSpot(3, second));

        MapConfigurationService reloaded = MapConfigurationService.open(temporaryDirectory);
        assertEquals(first, reloaded.high().courses().get(3).spots().get(1));
        assertEquals(second, reloaded.high().courses().get(3).spots().get(2));
    }

    @Test
    void enduranceGoalAlwaysUsesCurrentMaximumPlusOne() throws Exception {
        MapConfigurationService maps = MapConfigurationService.open(temporaryDirectory);
        MapSetupService setup = new MapSetupService(maps);
        setup.setEnduranceProgress(4, point(0));

        assertEquals(5, setup.setEnduranceGoal(point(10)));
        assertEquals(6, setup.setEnduranceGoal(point(20)));
        assertEquals(6, maps.endurance().goalProgress());
    }

    @Test
    void removingReferencedProgressClearsItsReferences() throws Exception {
        MapConfigurationService maps = MapConfigurationService.open(temporaryDirectory);
        MapSetupService setup = new MapSetupService(maps);
        setup.setEnduranceProgress(1, point(0));
        setup.setEnduranceZone(2, 1);

        setup.removeEnduranceProgress(1);

        assertFalse(maps.endurance().progresses().containsKey(1));
        assertNull(maps.endurance().zone2Progress());
    }

    @Test
    void enduranceProgressSupportsMultipleLocationsAndIndividualRemoval() throws Exception {
        MapConfigurationService maps = MapConfigurationService.open(temporaryDirectory);
        MapSetupService setup = new MapSetupService(maps);
        EnduranceProgressPoint first = point(0);
        EnduranceProgressPoint second = point(10);

        setup.setEnduranceProgress(1, first);
        assertEquals(2, setup.addEnduranceProgress(1, second));
        assertEquals(java.util.List.of(first, second), maps.endurance().progresses().get(1));
        assertThrows(IllegalArgumentException.class, () -> setup.addEnduranceProgress(1, second));

        setup.removeEnduranceProgress(1, 1);

        assertEquals(java.util.List.of(second), maps.endurance().progresses().get(1));
    }

    private static BlockRegion region(int offset) {
        return new BlockRegion("beat", 64, offset, offset + 2, 0, 2);
    }

    private static EnduranceProgressPoint point(int offset) {
        return new EnduranceProgressPoint("beat", offset + 0.5D, 65D, 0.5D);
    }
}
