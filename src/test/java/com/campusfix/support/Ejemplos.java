package com.campusfix.support;

import com.campusfix.domain.incidencia.EstadoIncidencia;
import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.IncidenciaInfraestructura;
import com.campusfix.domain.incidencia.IncidenciaTecnologica;
import com.campusfix.domain.incidencia.Prioridad;

import java.time.Instant;

/** Constructores de datos de prueba. */
public final class Ejemplos {

    public static final Instant T0 = Instant.parse("2026-09-28T12:00:00Z");

    private Ejemplos() {
    }

    public static Incidencia tecnologica(String id, Prioridad prioridad, Instant creadaEn) {
        return new IncidenciaTecnologica(
                id, "Proyector no funciona", "No enciende",
                "Bloque B - Aula 302", prioridad, creadaEn, EstadoIncidencia.ABIERTA);
    }

    public static Incidencia infraestructura(String id, Prioridad prioridad, Instant creadaEn) {
        return new IncidenciaInfraestructura(
                id, "Puerta danada", "No cierra",
                "Bloque A - Aula 201", prioridad, creadaEn, EstadoIncidencia.ABIERTA);
    }

    public static Incidencia tecnologica(String id) {
        return tecnologica(id, Prioridad.MEDIA, T0);
    }

    public static Incidencia infraestructura(String id) {
        return infraestructura(id, Prioridad.MEDIA, T0);
    }
}
