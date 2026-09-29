package com.campusfix.domain.incidencia;

import java.time.Instant;

public class IncidenciaTecnologica extends Incidencia {

    public IncidenciaTecnologica(
            String id,
            String titulo,
            String descripcion,
            String ubicacion,
            Prioridad prioridad) {

        super(id, titulo, descripcion, ubicacion, prioridad);
    }

    public IncidenciaTecnologica(
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
        return TipoIncidencia.TECNOLOGIA;
    }
}
