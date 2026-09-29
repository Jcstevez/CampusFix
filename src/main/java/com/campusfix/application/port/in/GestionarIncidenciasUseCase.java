package com.campusfix.application.port.in;

import com.campusfix.domain.incidencia.EstadoIncidencia;
import com.campusfix.domain.incidencia.Incidencia;

import java.util.List;
import java.util.Optional;

/** Puerto de entrada: consultar y avanzar el estado de las incidencias. */
public interface GestionarIncidenciasUseCase {

    Incidencia consultar(String id);

    List<Incidencia> listar(Optional<EstadoIncidencia> estado);

    Incidencia atender(String id);

    Incidencia resolver(String id);
}
