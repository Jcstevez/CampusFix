package com.campusfix.domain.clasificacion;

import com.campusfix.domain.incidencia.Prioridad;
import com.campusfix.domain.incidencia.TipoIncidencia;

public record Clasificacion(TipoIncidencia tipo, Prioridad prioridad) {
}
