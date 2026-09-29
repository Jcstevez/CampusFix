package com.campusfix.domain.incidencia;

/**
 * Ciclo de vida de una incidencia.
 *
 * ABIERTA -> EN_PROCESO -> RESUELTA
 * ABIERTA / EN_PROCESO -> ESCALADA (por vencimiento del SLA) -> EN_PROCESO / RESUELTA
 */
public enum EstadoIncidencia {
    ABIERTA,
    EN_PROCESO,
    ESCALADA,
    RESUELTA
}
