package com.campusfix.strategy;

import com.campusfix.domain.incidencia.Incidencia;

public interface EstrategiaIncidencia {

    String ejecutar(Incidencia incidencia);
}