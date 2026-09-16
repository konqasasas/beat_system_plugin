package dev.konqasasas.beat.debug;

import dev.konqasasas.beat.domain.endurance.EnduranceRules;
import dev.konqasasas.beat.domain.endurance.EnduranceSession;
import dev.konqasasas.beat.domain.high.HighCompetitionSession;
import dev.konqasasas.beat.domain.high.HighDifficultyRules;
import dev.konqasasas.beat.domain.ta.TimeAttackSession;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Runs injected events through the same domain sessions used by live controllers. */
final class DebugCompetitionSimulator {
    private final List<UUID> participantIds;
    private final HighCompetitionSession high;
    private final TimeAttackSession timeAttack;
    private final EnduranceSession endurance;

    DebugCompetitionSimulator(List<DebugCompetitor> competitors,HighDifficultyRules highRules,EnduranceRules enduranceRules){
        Map<UUID,String> names=new LinkedHashMap<>();competitors.forEach(c->names.put(c.uuid(),c.name()));
        participantIds=List.copyOf(names.keySet());high=new HighCompetitionSession(names,highRules);
        timeAttack=new TimeAttackSession(names);endurance=new EnduranceSession(names,enduranceRules);
        participantIds.forEach(id->{high.activate(id);timeAttack.activate(id);endurance.activate(id);});
    }
    boolean matches(List<DebugCompetitor> competitors){return participantIds.equals(competitors.stream().map(DebugCompetitor::uuid).toList());}
    HighCompetitionSession high(){return high;}TimeAttackSession timeAttack(){return timeAttack;}EnduranceSession endurance(){return endurance;}
}
