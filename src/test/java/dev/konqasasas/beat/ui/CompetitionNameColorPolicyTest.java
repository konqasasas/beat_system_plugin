package dev.konqasasas.beat.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class CompetitionNameColorPolicyTest {
    @Test void mapsHighAndEnduranceMilestones() {
        assertEquals(CompetitionNameColor.NEUTRAL, CompetitionNameColorPolicy.high(1, false, false));
        assertEquals(CompetitionNameColor.LEADER, CompetitionNameColorPolicy.high(5, false, false));
        assertEquals(CompetitionNameColor.COMPLETE, CompetitionNameColorPolicy.high(5, true, false));
        assertEquals(CompetitionNameColor.ADVANCED,
                CompetitionNameColorPolicy.endurance(true, true, false, false));
        assertEquals(CompetitionNameColor.ELIMINATED,
                CompetitionNameColorPolicy.endurance(true, true, true, true));
    }

    @Test void mapsTimeAttackCutoffBrackets() {
        assertEquals(CompetitionNameColor.DANGER,
                CompetitionNameColorPolicy.timeAttack(30, true, false, 0, 20, 10));
        assertEquals(CompetitionNameColor.CAUTION,
                CompetitionNameColorPolicy.timeAttack(15, true, false, 0, 20, 10));
        assertEquals(CompetitionNameColor.COMPLETE,
                CompetitionNameColorPolicy.timeAttack(8, true, false, 0, 20, 10));
        assertEquals(CompetitionNameColor.DANGER,
                CompetitionNameColorPolicy.timeAttack(15, true, false, 1, 20, 10));
        assertEquals(CompetitionNameColor.ELIMINATED,
                CompetitionNameColorPolicy.timeAttack(1, true, true, 2, 20, 10));
    }
}
