package com.campusfix.strategy;

import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.IncidenciaTecnologica;
import com.campusfix.domain.incidencia.Prioridad;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EstrategiaIncidenciaTest {

    @Test
    void debeDefinirLaAccionParaUnaIncidenciaTecnologica() {

        Incidencia incidencia = new IncidenciaTecnologica(
                "INC-004",
                "Computador no enciende",
                "El computador del laboratorio no enciende",
                "Bloque C - Laboratorio 1",
                Prioridad.ALTA
        );

        EstrategiaIncidencia estrategia =
                new EstrategiaTecnologica();

        assertEquals(
                "Atender incidencia tecnológica",
                estrategia.ejecutar(incidencia)
        );
    }
    @Test
    void debeDefinirLaAccionParaUnaIncidenciaDeInfraestructura() {

        Incidencia incidencia = new IncidenciaTecnologica(
            "INC-005",
            "Puerta dañada",
            "La puerta del aula presenta daños",
            "Bloque A - Aula 201",
            Prioridad.ALTA
        );

        EstrategiaIncidencia estrategia =
            new EstrategiaInfraestructura();

        assertEquals(
            "Atender incidencia de infraestructura",
            estrategia.ejecutar(incidencia)
        );
    }
}