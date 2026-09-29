package com.campusfix.domain.error;

public class ObjetoNoEncontradoException extends RecursoNoEncontradoException {

    public ObjetoNoEncontradoException(String objetoId) {
        super("El QR apunta a un objeto que no esta en el catalogo: " + objetoId);
    }
}
