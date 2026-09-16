package dev.konqasasas.beat.application;

import static org.junit.jupiter.api.Assertions.*;
import dev.konqasasas.beat.persistence.ResultsRepository;
import dev.konqasasas.beat.persistence.snapshot.*;
import java.nio.file.Path;import java.util.*;import org.junit.jupiter.api.Test;import org.junit.jupiter.api.io.TempDir;

class ResultEditingServiceTest {
 @TempDir Path directory;
 @Test void parsesOnlyFiveHundredthSecondUnits(){assertEquals(636,ResultEditingService.parseTimeTicks("31.80"));assertThrows(IllegalArgumentException.class,()->ResultEditingService.parseTimeTicks("31.82"));}
 @Test void editInvalidatesThenRecalculateAndConfirm()throws Exception{Memory repo=new Memory(snapshot());var service=new ResultEditingService(repo,null,new ResultAuditLog(directory.resolve("edits.log")));service.edit("Admin",ResultEditingService.Competition.HIGH,"Player","points","250",null);assertFalse(repo.state.highConfirmed());assertNull(repo.state.players().getFirst().high().rank());service.recalculate(ResultEditingService.Competition.HIGH);service.confirm("Admin",ResultEditingService.Competition.HIGH);assertTrue(repo.state.highConfirmed());assertEquals(250,repo.state.players().getFirst().high().points());service.undo("Admin");assertEquals(100,repo.state.players().getFirst().high().points());}
 private static ResultsSnapshot snapshot(){var p=new PlayerResultSnapshot(UUID.randomUUID(),"Player",true,true,true,false,false,new HighResultSnapshot(1,100,10L),new TimeAttackResultSnapshot(1,600L,20L,1L,null,null),new EnduranceResultSnapshot(1,5,30L,false,false,false),null);return new ResultsSnapshot(1,true,true,true,false,List.of(p));}
 private static final class Memory implements ResultsRepository{ResultsSnapshot state;Memory(ResultsSnapshot s){state=s;}public Optional<ResultsSnapshot>load(){return Optional.of(state);}public void save(ResultsSnapshot s){state=s;}}
}
