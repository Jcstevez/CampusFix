package com.campusfix.application;

import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.Prioridad;
import com.campusfix.factory.IncidenciaCreator;

public class IncidenciaService {

    public Incidencia registrarIncidencia(
            IncidenciaCreator creator,
            String id,
            String titulo,
            String descripcion,
            String ubicacion,
            Prioridad prioridad) {

        return creator.crearIncidencia(
                id,
                titulo,
                descripcion,
                ubicacion,
                prioridad
        );
    }
}
