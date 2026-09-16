package dev.konqasasas.beat.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.konqasasas.beat.domain.WhitelistMode;
import dev.konqasasas.beat.domain.state.TournamentState;
import dev.konqasasas.beat.persistence.*;
import dev.konqasasas.beat.persistence.snapshot.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OverallServiceTest {
    @Test void calculateThenConfirmPersistsAndTransitionsState() throws Exception {
        MemoryResults results = new MemoryResults(new ResultsSnapshot(1,true,true,true,false,List.of(player())));
        MemoryEvents events = new MemoryEvents(event(TournamentState.ENDURANCE_FINISHED));
        OverallService service = new OverallService(results,new EventStateService(events,events.state));
        var calculated=service.calculate();
        assertEquals(6,calculated.players().getFirst().overall().scoreProduct());
        assertEquals(TournamentState.OVERALL_READY,events.state.tournamentState());
        assertTrue(service.confirm().overallConfirmed());
        assertEquals(TournamentState.OVERALL_CONFIRMED,events.state.tournamentState());
        assertThrows(IllegalStateException.class,service::calculate);
    }
    private static PlayerResultSnapshot player(){return new PlayerResultSnapshot(UUID.randomUUID(),"Player",true,true,true,false,false,new HighResultSnapshot(1,100,10L),new TimeAttackResultSnapshot(2,100L,10L,1L,null,null),new EnduranceResultSnapshot(3,10,10L,false,false,false),null);}
    private static EventStateSnapshot event(TournamentState s){return new EventStateSnapshot(1,s,WhitelistMode.ADMIN_ONLY,Map.of());}
    private static final class MemoryResults implements ResultsRepository{ResultsSnapshot state;MemoryResults(ResultsSnapshot s){state=s;}public Optional<ResultsSnapshot>load(){return Optional.of(state);}public void save(ResultsSnapshot s){state=s;}}
    private static final class MemoryEvents implements EventStateRepository{EventStateSnapshot state;MemoryEvents(EventStateSnapshot s){state=s;}public Optional<EventStateSnapshot>load(){return Optional.of(state);}public void save(EventStateSnapshot s){state=s;}}
}
