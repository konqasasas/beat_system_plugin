package dev.konqasasas.beat.debug;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DebugServiceTest {
    @Test void refusesEnableWhenFormalResultsExist(){
        DebugService service=new DebugService(()->true);
        assertThrows(IllegalStateException.class,service::enable);
        assertFalse(service.enabled());
    }

    @Test void disablingDropsAllVolatileDebugData() throws Exception {
        DebugService service=new DebugService(()->false);service.enable();service.session().addBots(29);
        service.disable();
        assertFalse(service.enabled());assertThrows(IllegalStateException.class,service::session);
        service.enable();assertTrue(service.session().competitors().isEmpty());
    }
}
