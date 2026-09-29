package com.campusfix.application;

import com.campusfix.application.port.out.Notificador;
import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.TipoIncidencia;
import com.campusfix.observer.IncidenciaObserver;

/** Reto 1: las incidencias de tecnologia se avisan por correo al area de TI. */
public class CorreoTecnologiaObserver implements IncidenciaObserver {

    private final Notificador correo;
    private final String destinatario;

    public CorreoTecnologiaObserver(Notificador correo, String destinatario) {
        this.correo = correo;
        this.destinatario = destinatario;
    }

    @Override
    public void actualizar(Incidencia incidencia) {
        if (incidencia.tipo() != TipoIncidencia.TECNOLOGIA) {
            return;
        }
        correo.enviar(new Notificacion(
                Canal.CORREO,
                destinatario,
                "[CampusFix] Incidencia tecnologica " + incidencia.getPrioridad()
                        + ": " + incidencia.getTitulo(),
                "Ubicacion: " + incidencia.getUbicacion()
                        + "\nDescripcion: " + incidencia.getDescripcion()
                        + "\nId: " + incidencia.getId()));
    }
}
