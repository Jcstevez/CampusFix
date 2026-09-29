package com.campusfix.application.port.out;

import com.campusfix.domain.incidencia.EstadoIncidencia;
import com.campusfix.domain.incidencia.Incidencia;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Puerto de salida: persistencia de incidencias. */
public interface IncidenciaRepositorio {

    /** Inserta o actualiza (upsert por id). */
    void guardar(Incidencia incidencia);

    Optional<Incidencia> buscarPorId(String id);

    /** Ordenadas de la mas antigua a la mas reciente. */
    List<Incidencia> listarPorEstados(Collection<EstadoIncidencia> estados);
}
