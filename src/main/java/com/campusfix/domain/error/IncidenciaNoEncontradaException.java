package com.campusfix.domain.error;

public class IncidenciaNoEncontradaException extends RecursoNoEncontradoException {

    public IncidenciaNoEncontradaException(String id) {
        super("No existe la incidencia " + id);
    }
}
