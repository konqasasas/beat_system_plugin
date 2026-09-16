package dev.konqasasas.beat.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MenuPaginationTest {
    @Test void fiftyParticipantsUseTwoPagesWithoutDroppingRows(){
        assertEquals(2,MenuPagination.pages(50));
        assertEquals(0,MenuPagination.from(0));assertEquals(45,MenuPagination.to(0,50));
        assertEquals(45,MenuPagination.from(1));assertEquals(50,MenuPagination.to(1,50));
    }
    @Test void emptyAndOutOfRangePagesAreClamped(){
        assertEquals(1,MenuPagination.pages(0));assertEquals(0,MenuPagination.clamp(99,0));
        assertEquals(1,MenuPagination.clamp(99,50));assertEquals(0,MenuPagination.clamp(-1,50));
        assertThrows(IllegalArgumentException.class,()->MenuPagination.pages(-1));
    }
}
