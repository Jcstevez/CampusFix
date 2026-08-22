package com.campusfix.domain.incidencia;

public class IncidenciaInfraestructura extends Incidencia {

    public IncidenciaInfraestructura(
            String id,
            String titulo,
            String descripcion,
            String ubicacion,
            Prioridad prioridad) {

        super(id, titulo, descripcion, ubicacion, prioridad);
    }
}
