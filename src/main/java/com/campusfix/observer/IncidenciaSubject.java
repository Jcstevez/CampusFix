package com.campusfix.observer;

import com.campusfix.domain.incidencia.Incidencia;

import java.util.ArrayList;
import java.util.List;

public class IncidenciaSubject {

    private final List<IncidenciaObserver> observers = new ArrayList<>();

    public void agregarObserver(IncidenciaObserver observer) {
        observers.add(observer);
    }

    public void notificarObservers(Incidencia incidencia) {
        for (IncidenciaObserver observer : observers) {
            observer.actualizar(incidencia);
        }
    }
}