package com.campusfix.factory;

import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.Prioridad;

public abstract class IncidenciaCreator {

    public abstract Incidencia crearIncidencia(
            String id,
            String titulo,
            String descripcion,
            String ubicacion,
            Prioridad prioridad);
}