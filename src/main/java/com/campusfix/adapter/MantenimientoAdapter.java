package com.campusfix.adapter;

import com.campusfix.domain.incidencia.Incidencia;

public class MantenimientoAdapter {

    private final ExternalMaintenanceClient clienteExterno;

    public MantenimientoAdapter(
            ExternalMaintenanceClient clienteExterno) {

        this.clienteExterno = clienteExterno;
    }

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
