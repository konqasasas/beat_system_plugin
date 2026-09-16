package dev.konqasasas.beat.debug;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DebugScenarioRunnerTest {
    @Test void allBuiltInScenariosPassAndContinueAsACompleteRun(){
        var results=new DebugScenarioRunner().run("all");
        assertTrue(results.size()>=4);
        assertTrue(results.stream().allMatch(DebugScenarioRunner.ScenarioResult::passedAll),
                ()->results.stream().flatMap(r->r.checks().stream().filter(c->!c.passed()).map(c->r.name()+"/"+c.label()+" expected="+c.expected()+" actual="+c.actual())).toList().toString());
        assertEquals(49,results.stream().mapToInt(DebugScenarioRunner.ScenarioResult::total).sum());
        assertEquals(java.util.Map.of(
                "high-boundaries",12,
                "ta-elimination",16,
                "endurance-zones",11,
                "overall-ranking",10),
                results.stream().collect(java.util.stream.Collectors.toMap(
                        DebugScenarioRunner.ScenarioResult::name,
                        DebugScenarioRunner.ScenarioResult::total)));
    }
}
