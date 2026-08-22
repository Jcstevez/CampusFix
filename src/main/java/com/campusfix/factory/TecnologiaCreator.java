package com.campusfix.factory;

import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.IncidenciaTecnologica;
import com.campusfix.domain.incidencia.Prioridad;

public class TecnologiaCreator extends IncidenciaCreator {

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
                prioridad);
    }
}