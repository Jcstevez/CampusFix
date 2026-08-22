package com.campusfix.factory;

import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.IncidenciaInfraestructura;
import com.campusfix.domain.incidencia.Prioridad;

public class InfraestructuraCreator extends IncidenciaCreator {

    @Override
    public Incidencia crearIncidencia(
            String id,
            String titulo,
            String descripcion,
            String ubicacion,
            Prioridad prioridad) {

        return new IncidenciaInfraestructura(
                id,
                titulo,
                descripcion,
                ubicacion,
                prioridad);
    }
}
