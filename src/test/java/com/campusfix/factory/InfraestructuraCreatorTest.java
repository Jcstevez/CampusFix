package com.campusfix.factory;

import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.IncidenciaInfraestructura;
import com.campusfix.domain.incidencia.Prioridad;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class InfraestructuraCreatorTest {

    @Test
    void debeCrearUnaIncidenciaDeInfraestructura() {

        InfraestructuraCreator creator = new InfraestructuraCreator();

        Incidencia incidencia = creator.crearIncidencia(
                "INC-002",
                "Puerta dañada",
                "La puerta del aula presenta daños",
                "Bloque A - Aula 201",
                Prioridad.ALTA
        );

        assertInstanceOf(IncidenciaInfraestructura.class, incidencia);
    }

    @Test
    void debeCrearLaIncidenciaDeInfraestructuraConSusDatos() {

        InfraestructuraCreator creator = new InfraestructuraCreator();

        Incidencia incidencia = creator.crearIncidencia(
                "INC-002",
                "Puerta dañada",
                "La puerta del aula presenta daños",
                "Bloque A - Aula 201",
                Prioridad.ALTA
        );

        assertEquals("INC-002", incidencia.getId());
        assertEquals("Puerta dañada", incidencia.getTitulo());
        assertEquals(
                "La puerta del aula presenta daños",
                incidencia.getDescripcion()
        );
        assertEquals("Bloque A - Aula 201", incidencia.getUbicacion());
        assertEquals(Prioridad.ALTA, incidencia.getPrioridad());
    }
}
