package com.campusfix.application.port.out;

/** Puerto de salida: lee el texto de una imagen con un codigo QR. */
public interface DecodificadorQr {

    /** @throws com.campusfix.domain.error.QrNoLegibleException si no hay QR legible */
    String decodificar(byte[] imagen);
}
