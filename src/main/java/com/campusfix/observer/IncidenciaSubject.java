package com.campusfix.observer;

import com.campusfix.domain.incidencia.Incidencia;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Publica los eventos de incidencias a sus observadores.
 *
 * Un observador que falla no debe impedir que los demas se enteren ni romper
 * el registro del reporte: la falla se registra y se continua.
 */
public class IncidenciaSubject {

    private static final System.Logger LOG =
            System.getLogger(IncidenciaSubject.class.getName());

    private final List<IncidenciaObserver> observers = new CopyOnWriteArrayList<>();

    public void agregarObserver(IncidenciaObserver observer) {
        observers.add(observer);
    }

    public void notificarObservers(Incidencia incidencia) {
        for (IncidenciaObserver observer : observers) {
            try {
                observer.actualizar(incidencia);
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING,
                        "Observer " + observer.getClass().getSimpleName()
                                + " fallo con la incidencia " + incidencia.getId(),
                        e);
            }
        }
    }
}
