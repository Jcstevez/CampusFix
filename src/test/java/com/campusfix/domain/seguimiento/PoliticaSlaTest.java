package com.campusfix.domain.seguimiento;

import com.campusfix.domain.incidencia.Prioridad;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PoliticaSlaTest {

    @Test
    void laPoliticaPorDefectoUsa2_24y72Horas() {
        PoliticaSla p = PoliticaSla.porDefecto();
        assertEquals(Duration.ofHours(2), p.plazoPara(Prioridad.ALTA));
        assertEquals(Duration.ofHours(24), p.plazoPara(Prioridad.MEDIA));
        assertEquals(Duration.ofHours(72), p.plazoPara(Prioridad.BAJA));
    }

    @Test
    void unPlazoCeroEsInvalido() {
        assertThrows(IllegalArgumentException.class, () ->
                new PoliticaSla(Duration.ZERO, Duration.ofHours(1), Duration.ofHours(1)));
    }

    @Test
    void unPlazoNegativoEsInvalido() {
        assertThrows(IllegalArgumentException.class, () ->
                new PoliticaSla(Duration.ofHours(1), Duration.ofHours(-1), Duration.ofHours(1)));
    }

    @Test
    void unPlazoNuloEsInvalido() {
        assertThrows(IllegalArgumentException.class, () ->
                new PoliticaSla(Duration.ofHours(1), Duration.ofHours(1), null));
    }
}
