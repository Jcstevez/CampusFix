package com.campusfix.application;

import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.IncidenciaTecnologica;
import com.campusfix.domain.incidencia.Prioridad;
import com.campusfix.factory.IncidenciaCreator;
import com.campusfix.factory.TecnologiaCreator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class IncidenciaServiceTest {

    @Test
    void debeRegistrarUnaIncidenciaTecnologica() {

        IncidenciaService service = new IncidenciaService();
        TecnologiaCreator creator = new TecnologiaCreator();

        Incidencia incidencia = service.registrarIncidencia(
                creator,
                "INC-003",
                "Computador no enciende",
                "El computador del laboratorio no enciende",
                "Bloque C - Laboratorio 1",
                Prioridad.ALTA
        );

        assertInstanceOf(IncidenciaTecnologica.class, incidencia);
    }

    @Test
    void debeRegistrarUnaIncidenciaUsandoLaAbstraccionCreator() {

        IncidenciaCreator creator = new IncidenciaCreator() {

            @Override
            public Incidencia crearIncidencia(
                    String id,
                    String titulo,
                    String descripcion,
                    String ubicacion,
                    Prioridad prioridad) {

                return new IncidenciaTecnologica(
                        id,
                        titulo,
                        descripcion,
                        ubicacion,
                        prioridad
                );
            }
        };

        IncidenciaService service = new IncidenciaService();

        Incidencia incidencia = service.registrarIncidencia(
                creator,
                "INC-004",
                "Pantalla sin señal",
                "La pantalla del laboratorio no muestra imagen",
                "Bloque C - Laboratorio 2",
                Prioridad.MEDIA
        );

        assertEquals("INC-004", incidencia.getId());
        assertEquals("Pantalla sin señal", incidencia.getTitulo());
        assertEquals(Prioridad.MEDIA, incidencia.getPrioridad());
    }
}
