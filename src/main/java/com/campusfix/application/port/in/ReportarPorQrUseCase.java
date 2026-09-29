package com.campusfix.application.port.in;

import com.campusfix.domain.incidencia.Incidencia;

/** Puerto de entrada: reportar escaneando el QR pegado en un objeto. */
public interface ReportarPorQrUseCase {

    Incidencia reportarPorContenido(String contenidoQr, String descripcion);

    Incidencia reportarPorImagen(byte[] imagenQr, String descripcion);

    /** Simula la impresion del QR de un objeto del catalogo (PNG). */
    byte[] generarQrDeObjeto(String objetoId);
}
