package com.campusfix.domain.incidencia;

public class IncidenciaTecnologica extends Incidencia {

    public IncidenciaTecnologica(
            String id,
            String titulo,
            String descripcion,
            String ubicacion,
            Prioridad prioridad) {

        super(id, titulo, descripcion, ubicacion, prioridad);
    }
}