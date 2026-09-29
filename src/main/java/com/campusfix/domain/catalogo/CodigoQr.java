package com.campusfix.domain.catalogo;

import com.campusfix.domain.error.CodigoQrInvalidoException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contenido de los QR de CampusFix: {@code campusfix://objeto/<ID>}.
 * El QR solo lleva el identificador; el resto se resuelve en el catalogo.
 */
public record CodigoQr(String objetoId) {

    public static final String PREFIJO = "campusfix://objeto/";

    private static final Pattern FORMATO =
            Pattern.compile("^campusfix://objeto/([A-Za-z0-9][A-Za-z0-9-]{0,39})$");

    public CodigoQr {
        if (objetoId == null || !objetoId.matches("[A-Za-z0-9][A-Za-z0-9-]{0,39}")) {
            throw new CodigoQrInvalidoException("Identificador de objeto invalido");
        }
    }

    public static CodigoQr desde(String contenido) {
        if (contenido == null || contenido.isBlank()) {
            throw new CodigoQrInvalidoException("El contenido del QR esta vacio");
        }
        Matcher m = FORMATO.matcher(contenido.strip());
        if (!m.matches()) {
            throw new CodigoQrInvalidoException(
                    "El QR no es de CampusFix (se esperaba " + PREFIJO + "<ID>)");
        }
        return new CodigoQr(m.group(1));
    }

    public String contenido() {
        return PREFIJO + objetoId;
    }
}
