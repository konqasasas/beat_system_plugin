package dev.konqasasas.beat.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CompetitionSettingsTest {
    @Test
    void testPresetHalvesCompetitionTimesButKeepsCountdownAndSurvivors() {
        CompetitionSettings production = CompetitionSettings.production();
        CompetitionSettings test = CompetitionSettings.testPreset();

        assertEquals(production.startCountdownTicks(), test.startCountdownTicks());
        assertEquals(production.highPracticeTicks() / 2, test.highPracticeTicks());
        assertEquals(production.highPrepareTicks() / 2, test.highPrepareTicks());
        assertEquals(production.highRunningTicks() / 2, test.highRunningTicks());
        assertEquals(production.timeAttackRunningTicks() / 2, test.timeAttackRunningTicks());
        assertEquals(production.enduranceRunningTicks() / 2, test.enduranceRunningTicks());
        assertEquals(production.timeAttackSurvivorCounts(), test.timeAttackSurvivorCounts());
    }

    @Test
    void rejectsUnorderedEliminationTimes() {
        assertThrows(IllegalArgumentException.class,
                () -> CompetitionSettings.production().with("high-elimination-2", 10_000));
    }

    @Test
    void changesOneSettingWithoutMutatingOriginal() {
        CompetitionSettings original = CompetitionSettings.production();
        CompetitionSettings changed = original.with("ta-survivors-1", 18);
        assertEquals(20, original.timeAttackSurvivorCounts().getFirst());
        assertEquals(18, changed.timeAttackSurvivorCounts().getFirst());
    }
}
