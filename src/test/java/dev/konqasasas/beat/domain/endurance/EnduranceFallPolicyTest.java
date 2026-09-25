package dev.konqasasas.beat.domain.endurance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnduranceFallPolicyTest {
    @Test void fallDestinationDependsOnWhetherThePlayerHasFinished(){
        EnduranceRecord record=new EnduranceRecord(new EnduranceRules(40,70,100));
        record.reachProgress(78,100);
        assertTrue(EnduranceFallPolicy.shouldRestart(record,-1,0));
        assertFalse(EnduranceFallPolicy.shouldReturnToGoal(record,-1,0));
        record.reachProgress(100,200);
        assertFalse(EnduranceFallPolicy.shouldRestart(record,-1,0));
        assertTrue(EnduranceFallPolicy.shouldReturnToGoal(record,-1,0));
        assertFalse(EnduranceFallPolicy.shouldRestart(record,1,0));
        assertFalse(EnduranceFallPolicy.shouldReturnToGoal(record,1,0));
    }
}
