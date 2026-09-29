package com.campusfix.application;

import com.campusfix.application.port.out.Notificador;
import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.observer.IncidenciaObserver;

/** Reto 1: cada objeto reportado genera una alerta visible en la app. */
public class AlertaObjetoReportadoObserver implements IncidenciaObserver {

    private final Notificador alertas;

    public AlertaObjetoReportadoObserver(Notificador alertas) {
        this.alertas = alertas;
    }

    @Override
    public void actualizar(Incidencia incidencia) {
        alertas.enviar(new Notificacion(
                Canal.APP,
                "app-sabana",
                "Nuevo reporte: " + incidencia.getTitulo(),
                "Se reporto " + incidencia.tipo() + " en "
                        + incidencia.getUbicacion() + " con prioridad "
                        + incidencia.getPrioridad() + " (id "
                        + incidencia.getId() + ")."));
    }
}
