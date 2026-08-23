package com.campusfix.strategy;

import com.campusfix.domain.incidencia.Incidencia;

public class EstrategiaInfraestructura implements EstrategiaIncidencia {

    @Override
    public String ejecutar(Incidencia incidencia) {
        return "Atender incidencia de infraestructura";
    }
}