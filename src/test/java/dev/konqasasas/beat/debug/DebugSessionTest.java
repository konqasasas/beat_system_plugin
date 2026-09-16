package dev.konqasasas.beat.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class DebugSessionTest {
    @Test void botsHaveStableUniqueIdentityAndNeverNeedPlayerObjects(){
        DebugSession first=new DebugSession();DebugSession second=new DebugSession();
        first.addBots(30);second.addBots(30);
        assertEquals(30,first.competitors().stream().map(DebugCompetitor::uuid).distinct().count());
        assertEquals(first.competitors().get(12).uuid(),second.competitors().get(12).uuid());
    }

    @Test void seededFillIsReproducible(){
        DebugSession first=new DebugSession();DebugSession second=new DebugSession();
        first.addBots(20);second.addBots(20);first.fill("all",12345);second.fill("all",12345);
        assertEquals(snapshot(first),snapshot(second));
    }

    @Test void zeroRecordsAreTrueTiesAndRecordedTicksBreakNonzeroTies(){
        DebugSession session=new DebugSession();session.addBots(3);
        assertEquals(List.of(1,1,1),session.highRanking().stream().map(DebugSession.Standing::rank).toList());
        session.setHigh("Debug01",850,200L);session.setHigh("Debug02",850,100L);
        assertEquals("Debug02",session.highRanking().getFirst().competitor().name());
    }

    @Test void nextMovesToTenSecondsBeforeImportantBoundary(){
        DebugSession session=new DebugSession();
        assertEquals(11_800,session.next());assertEquals(17_800,session.next());
        session.setTime(36_000);assertThrows(IllegalStateException.class,session::next);
    }

    private static List<String> snapshot(DebugSession session){return session.competitors().stream().map(c->c.name()+":"+c.highPoints()+":"+c.highTick()+":"+c.taTicks()+":"+c.taReachedTick()+":"+c.enduranceProgress()+":"+c.enduranceTick()).toList();}
}
