package com.campusfix.support;

import com.campusfix.application.port.out.IncidenciaRepositorio;
import com.campusfix.domain.incidencia.EstadoIncidencia;
import com.campusfix.domain.incidencia.Incidencia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Doble de prueba (fake) del repositorio: sin base de datos ni red. */
public class RepositorioEnMemoria implements IncidenciaRepositorio {

    private final Map<String, Incidencia> datos = new LinkedHashMap<>();
    private int llamadasAGuardar;

    @Override
    public synchronized void guardar(Incidencia incidencia) {
        llamadasAGuardar++;
        datos.put(incidencia.getId(), incidencia);
    }

    @Override
    public synchronized Optional<Incidencia> buscarPorId(String id) {
        return Optional.ofNullable(datos.get(id));
    }

    @Override
    public synchronized List<Incidencia> listarPorEstados(
            Collection<EstadoIncidencia> estados) {
        List<Incidencia> resultado = new ArrayList<>();
        for (Incidencia i : datos.values()) {
            if (estados.contains(i.getEstado())) {
                resultado.add(i);
            }
        }
        resultado.sort(Comparator.comparing(Incidencia::getCreadaEn));
        return resultado;
    }

    public synchronized int llamadasAGuardar() {
        return llamadasAGuardar;
    }

    public synchronized int total() {
        return datos.size();
    }
}
