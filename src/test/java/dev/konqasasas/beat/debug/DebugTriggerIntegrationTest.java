package dev.konqasasas.beat.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DebugTriggerIntegrationTest {
    @Test void injectedEventsUseLiveCompetitionDomainSessions() throws Exception {
        DebugService debug=new DebugService(()->false);debug.enable();debug.session().addBots(1);
        debug.session().setTime(100);debug.trigger("high","spot",1,3);
        assertEquals(300,debug.session().require("Debug01").highPoints());
        debug.trigger("high","spot",1,3);
        assertEquals(300,debug.session().require("Debug01").highPoints(),"duplicate Spot must not add points");

        debug.session().setTime(0);debug.trigger("ta","start",null,null);
        debug.session().setTime(200);debug.trigger("ta","split",1,null);
        debug.session().setTime(500);debug.trigger("ta","goal",null,null);
        assertEquals(500,debug.session().require("Debug01").taTicks());

        debug.session().setTime(600);debug.trigger("endurance","progress",78,null);
        assertEquals(78,debug.session().require("Debug01").enduranceProgress());
    }

    @Test void liveDomainValidationRejectsInvalidInjectedSpot() throws Exception {
        DebugService debug=new DebugService(()->false);debug.enable();debug.session().addBots(1);
        assertThrows(IllegalArgumentException.class,()->debug.trigger("high","spot",1,99));
    }
}
