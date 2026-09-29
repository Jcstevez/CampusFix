package com.campusfix.domain.error;

public class CodigoQrInvalidoException extends IllegalArgumentException {

    public CodigoQrInvalidoException(String mensaje) {
        super(mensaje);
    }
}
