package com.campusfix.application.port.out;

import com.campusfix.domain.incidencia.Incidencia;

/** Puerto de salida: sistema externo de mantenimiento (tickets). */
public interface MantenimientoPuerto {

    void enviarIncidencia(Incidencia incidencia);
}
