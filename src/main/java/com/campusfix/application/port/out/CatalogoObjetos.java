package com.campusfix.application.port.out;

import com.campusfix.domain.catalogo.ObjetoCampus;

import java.util.Optional;

/** Puerto de salida: catalogo de objetos del campus que llevan QR. */
public interface CatalogoObjetos {

    Optional<ObjetoCampus> buscarPorId(String objetoId);
}
