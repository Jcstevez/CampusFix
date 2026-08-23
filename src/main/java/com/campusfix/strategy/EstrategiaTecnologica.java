package com.campusfix.strategy;

import com.campusfix.domain.incidencia.Incidencia;

public class EstrategiaTecnologica implements EstrategiaIncidencia {

    @Override
    public String ejecutar(Incidencia incidencia) {
        return "Atender incidencia tecnológica";
    }
}