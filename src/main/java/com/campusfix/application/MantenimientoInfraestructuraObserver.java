package com.campusfix.application;

import com.campusfix.application.port.out.MantenimientoPuerto;
import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.TipoIncidencia;
import com.campusfix.observer.IncidenciaObserver;

/** Las incidencias de infraestructura se envian como ticket al sistema de mantenimiento. */
public class MantenimientoInfraestructuraObserver implements IncidenciaObserver {

    private final MantenimientoPuerto mantenimiento;

    public MantenimientoInfraestructuraObserver(MantenimientoPuerto mantenimiento) {
        this.mantenimiento = mantenimiento;
    }

    @Override
    public void actualizar(Incidencia incidencia) {
        if (incidencia.tipo() == TipoIncidencia.INFRAESTRUCTURA) {
            mantenimiento.enviarIncidencia(incidencia);
        }
    }
}
