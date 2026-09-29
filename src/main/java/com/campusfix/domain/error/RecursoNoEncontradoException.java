package com.campusfix.domain.error;

/** Base para "no existe el recurso pedido" (se traduce a 404 en la frontera HTTP). */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
