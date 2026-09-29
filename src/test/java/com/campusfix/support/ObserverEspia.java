package com.campusfix.support;

import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.observer.IncidenciaObserver;

import java.util.ArrayList;
import java.util.List;

/** Doble de prueba (spy) de un observer. */
public class ObserverEspia implements IncidenciaObserver {

    private final List<Incidencia> recibidas = new ArrayList<>();
    private final boolean fallar;

    public ObserverEspia() {
        this(false);
    }

    private ObserverEspia(boolean fallar) {
        this.fallar = fallar;
    }

    public static ObserverEspia queFalla() {
        return new ObserverEspia(true);
    }

    @Override
    public void actualizar(Incidencia incidencia) {
        if (fallar) {
            throw new IllegalStateException("Fallo simulado del observer");
        }
        recibidas.add(incidencia);
    }

    public List<Incidencia> recibidas() {
        return recibidas;
    }
}
