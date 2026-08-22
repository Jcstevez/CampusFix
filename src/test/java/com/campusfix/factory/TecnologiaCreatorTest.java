package com.campusfix.factory;

import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.IncidenciaTecnologica;
import com.campusfix.domain.incidencia.Prioridad;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TecnologiaCreatorTest {

    @Test
    void debeCrearUnaIncidenciaTecnologica() {

        TecnologiaCreator creator = new TecnologiaCreator();

        Incidencia incidencia = creator.crearIncidencia(
                "INC-001",
                "Proyector no funciona",
                "El proyector del aula 302 no enciende",
                "Bloque B - Aula 302",
                Prioridad.MEDIA
        );

        assertInstanceOf(IncidenciaTecnologica.class, incidencia);
    }

    @Test
    void debeCrearLaIncidenciaConSusDatos() {

        TecnologiaCreator creator = new TecnologiaCreator();

        Incidencia incidencia = creator.crearIncidencia(
                "INC-001",
                "Proyector no funciona",
                "El proyector del aula 302 no enciende",
                "Bloque B - Aula 302",
                Prioridad.MEDIA
        );

        assertEquals("INC-001", incidencia.getId());
        assertEquals("Proyector no funciona", incidencia.getTitulo());
        assertEquals(
                "El proyector del aula 302 no enciende",
                incidencia.getDescripcion()
        );
        assertEquals("Bloque B - Aula 302", incidencia.getUbicacion());
        assertEquals(Prioridad.MEDIA, incidencia.getPrioridad());
    }
}