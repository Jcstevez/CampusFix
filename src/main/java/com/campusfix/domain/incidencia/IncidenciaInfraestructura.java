package com.campusfix.domain.incidencia;

import java.time.Instant;

public class IncidenciaInfraestructura extends Incidencia {

    public IncidenciaInfraestructura(
            String id,
            String titulo,
            String descripcion,
            String ubicacion,
            Prioridad prioridad) {

        super(id, titulo, descripcion, ubicacion, prioridad);
    }

    public IncidenciaInfraestructura(
            String id,
            String titulo,
            String descripcion,
            String ubicacion,
            Prioridad prioridad,
            Instant creadaEn,
            EstadoIncidencia estado) {

        super(id, titulo, descripcion, ubicacion, prioridad, creadaEn, estado);
    }

    @Override
    public TipoIncidencia tipo() {
        return TipoIncidencia.INFRAESTRUCTURA;
    }
}
