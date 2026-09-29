package com.campusfix.domain.error;

/** Falla de infraestructura al persistir o leer (envuelve la causa tecnica). */
public class RepositorioException extends RuntimeException {

    public RepositorioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
