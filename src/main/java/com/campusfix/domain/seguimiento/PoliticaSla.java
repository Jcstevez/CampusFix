package com.campusfix.domain.seguimiento;

import com.campusfix.domain.incidencia.Prioridad;

import java.time.Duration;

/** Plazo maximo sin atencion antes de escalar una incidencia, segun su prioridad. */
public record PoliticaSla(Duration alta, Duration media, Duration baja) {

    public PoliticaSla {
        if (alta == null || media == null || baja == null
                || alta.isNegative() || alta.isZero()
                || media.isNegative() || media.isZero()
                || baja.isNegative() || baja.isZero()) {
            throw new IllegalArgumentException("Los plazos deben ser positivos");
        }
    }

    public static PoliticaSla porDefecto() {
        return new PoliticaSla(
                Duration.ofHours(2),
                Duration.ofHours(24),
                Duration.ofHours(72));
    }

    public Duration plazoPara(Prioridad prioridad) {
        return switch (prioridad) {
            case ALTA -> alta;
            case MEDIA -> media;
            case BAJA -> baja;
        };
    }
}
