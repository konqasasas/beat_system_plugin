package dev.konqasasas.beat.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DustSphereRendererTest {
    @Test
    void displaySphereIsShiftedUpByItsRadius() {
        assertEquals(20D, DustSphereRenderer.displayY(20D, -1D));
        assertEquals(20.25D, DustSphereRenderer.displayY(20D, 0D));
        assertEquals(20.5D, DustSphereRenderer.displayY(20D, 1D));
    }
}
