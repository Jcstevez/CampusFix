package com.campusfix.adapter.out.mantenimiento;

import com.campusfix.application.port.out.MantenimientoPuerto;
import com.campusfix.domain.incidencia.Incidencia;

public class MantenimientoAdapter implements MantenimientoPuerto {

    private final ExternalMaintenanceClient clienteExterno;

    public MantenimientoAdapter(
            ExternalMaintenanceClient clienteExterno) {

        this.clienteExterno = clienteExterno;
    }

    @Override
    public void enviarIncidencia(Incidencia incidencia) {

        clienteExterno.crearTicket(
                incidencia.getId(),
                incidencia.getTitulo(),
                incidencia.getDescripcion(),
                incidencia.getUbicacion(),
                incidencia.getPrioridad().name()
        );
    }
}
