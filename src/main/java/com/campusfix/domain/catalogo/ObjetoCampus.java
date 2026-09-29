package com.campusfix.domain.catalogo;

import com.campusfix.domain.incidencia.TipoIncidencia;

/**
 * Objeto fisico del campus identificado por un QR pegado en el (proyector,
 * puerta, punto WiFi...). Al escanearlo ya se conoce que es y donde esta.
 */
public record ObjetoCampus(
        String id,
        String nombre,
        String ubicacion,
        TipoIncidencia tipo) {

    public ObjetoCampus {
        if (id == null || id.isBlank()
                || nombre == null || nombre.isBlank()
                || ubicacion == null || ubicacion.isBlank()
                || tipo == null) {
            throw new IllegalArgumentException(
                    "id, nombre, ubicacion y tipo son obligatorios");
        }
    }
}
