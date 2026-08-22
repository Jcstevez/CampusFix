package com.campusfix.adapter;

import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.IncidenciaTecnologica;
import com.campusfix.domain.incidencia.Prioridad;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MantenimientoAdapterTest {

    @Test
    void debeEnviarUnaIncidenciaAlSistemaExterno() {

        Incidencia incidencia = new IncidenciaTecnologica(
                "INC-004",
                "Proyector no funciona",
                "El proyector del aula 302 no enciende",
                "Bloque B - Aula 302",
                Prioridad.ALTA
        );

        ExternalMaintenanceClient clienteExterno =
                new ExternalMaintenanceClient();

        MantenimientoAdapter adapter =
                new MantenimientoAdapter(clienteExterno);

        adapter.enviarIncidencia(incidencia);

        assertEquals(
                "INC-004",
                clienteExterno.getUltimoTicketId()
        );
    }

    @Test
    void debeAdaptarTodosLosDatosDeLaIncidencia() {

        Incidencia incidencia = new IncidenciaTecnologica(
                "INC-005",
                "Computador dañado",
                "El computador del laboratorio no enciende",
                "Bloque C - Laboratorio 1",
                Prioridad.ALTA
        );

        ExternalMaintenanceClient clienteExterno =
                new ExternalMaintenanceClient();

        MantenimientoAdapter adapter =
                new MantenimientoAdapter(clienteExterno);

        adapter.enviarIncidencia(incidencia);

        assertEquals("INC-005", clienteExterno.getUltimoTicketId());
        assertEquals("Computador dañado", clienteExterno.getUltimoAsunto());
        assertEquals(
                "El computador del laboratorio no enciende",
                clienteExterno.getUltimoDetalle()
        );
        assertEquals(
                "Bloque C - Laboratorio 1",
                clienteExterno.getUltimoLugar()
        );
        assertEquals("ALTA", clienteExterno.getUltimoNivel());
    }
}