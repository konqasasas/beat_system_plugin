package dev.konqasasas.beat.domain.ta;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class TimeAttackSessionTest {
 @Test void requiresOutsideToInsidePlayerMovementAndKeepsFirstSplit(){UUID id=UUID.randomUUID();var s=new TimeAttackSession(Map.of(id,"P"));s.activate(id);assertFalse(s.enterStart(id,true,true,true,10));assertTrue(s.enterStart(id,false,true,true,20));assertEquals(10,s.reachSplit(id,2,30).orElseThrow());assertTrue(s.reachSplit(id,2,31).isEmpty());assertEquals(20,s.reachGoal(id,40).elapsedTicks());}
 @Test void restartDisplayDoesNotDiscardRunningData(){UUID id=UUID.randomUUID();var s=new TimeAttackSession(Map.of(id,"P"));s.activate(id);s.enterStart(id,false,true,true,10);s.markRestarted(id);assertEquals(0,s.displayedElapsed(id,30));assertTrue(s.running(id));}
 @Test void cutoffEliminatesNoRecordEvenWhenPlacesRemain(){UUID a=UUID.randomUUID(),b=UUID.randomUUID();var s=new TimeAttackSession(Map.of(a,"A",b,"B"));s.activate(a);s.activate(b);s.enterStart(a,false,true,true,1);s.reachGoal(a,20);assertEquals(List.of(b),s.eliminateToTop(10));}
 @Test void disqualifiedRankSeatDoesNotConsumeSurvivorCount(){UUID dq=UUID.randomUUID(),valid=UUID.randomUUID(),slow=UUID.randomUUID();var s=new TimeAttackSession(new LinkedHashMap<>(Map.of(dq,"DQ",valid,"Valid",slow,"Slow")));for(UUID id:List.of(dq,valid,slow))s.activate(id);goal(s,dq,400);goal(s,valid,500);goal(s,slow,600);s.competitor(dq).setDisqualified(true);s.eliminateToTop(1);assertFalse(s.active(dq));assertTrue(s.active(valid));assertFalse(s.active(slow));assertEquals(1,s.rank(dq));assertEquals(2,s.rank(valid));}
 private static void goal(TimeAttackSession s,UUID id,long tick){s.enterStart(id,false,true,true,0);s.reachGoal(id,tick);}
}
