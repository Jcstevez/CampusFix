package com.campusfix.domain.error;

/** La imagen recibida no contiene un QR que se pueda decodificar. */
public class QrNoLegibleException extends IllegalArgumentException {

    public QrNoLegibleException(String mensaje) {
        super(mensaje);
    }
}
