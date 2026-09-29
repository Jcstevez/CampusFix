package com.campusfix.application.port.in;

/** Puerto de entrada: que ningun reporte quede en el olvido. */
public interface SeguimientoUseCase {

    /** Escala y avisa las incidencias que superaron su plazo. Devuelve cuantas. */
    int revisarPendientes();
}
