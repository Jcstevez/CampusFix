package com.campusfix.application.port.in;

import com.campusfix.domain.incidencia.Incidencia;

/** Puerto de entrada: reportar una incidencia describiendola con texto. */
public interface ReportarIncidenciaUseCase {

    record ReporteManual(String titulo, String descripcion, String ubicacion) {
    }

    /** Clasifica automaticamente (tipo y prioridad), guarda y avisa. */
    Incidencia reportar(ReporteManual reporte);
}
