package com.campusfix.domain.catalogo;

import com.campusfix.domain.error.CodigoQrInvalidoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CodigoQrTest {

    @Test
    void unQrValidoExtraeElIdentificadorDelObjeto() {
        assertEquals("PROY-B302", CodigoQr.desde("campusfix://objeto/PROY-B302").objetoId());
    }

    @Test
    void ignoraEspaciosAlrededorDelContenido() {
        assertEquals("WIFI-A2", CodigoQr.desde("  campusfix://objeto/WIFI-A2\n").objetoId());
    }

    @Test
    void elIdentificadorAceptaMinusculasYDigitos() {
        assertEquals("pc-101", CodigoQr.desde("campusfix://objeto/pc-101").objetoId());
    }

    @Test
    void elContenidoGeneradoSePuedeVolverALeer() {
        // Arrange
        CodigoQr original = new CodigoQr("LUZ-A201");
        // Act
        CodigoQr leido = CodigoQr.desde(original.contenido());
        // Assert
        assertEquals(original, leido);
    }

    @Test
    void unIdentificadorDe40CaracteresEsValido() {
        String id = "A".repeat(40);
        assertEquals(id, CodigoQr.desde("campusfix://objeto/" + id).objetoId());
    }

    @Test
    void unIdentificadorDe41CaracteresEsInvalido() {
        String contenido = "campusfix://objeto/" + "A".repeat(41);
        assertThrows(CodigoQrInvalidoException.class, () -> CodigoQr.desde(contenido));
    }

    @Test
    void unContenidoNuloEsInvalido() {
        assertThrows(CodigoQrInvalidoException.class, () -> CodigoQr.desde(null));
    }

    @Test
    void unContenidoVacioEsInvalido() {
        assertThrows(CodigoQrInvalidoException.class, () -> CodigoQr.desde("   "));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "http://ejemplo.com/objeto/ABC",
            "campusfix://otra/ABC",
            "campusfix://objeto/",
            "campusfix://objeto/AB CD",
            "campusfix://objeto/AB/CD",
            "campusfix://objeto/-ABC",
            "campusfix://objeto/AB?x=1",
            "texto cualquiera"
    })
    void loQueNoTieneElFormatoDeCampusfixEsInvalido(String contenido) {
        assertThrows(CodigoQrInvalidoException.class, () -> CodigoQr.desde(contenido));
    }

    @Test
    void elConstructorTambienValidaElIdentificador() {
        assertThrows(CodigoQrInvalidoException.class, () -> new CodigoQr("no valido!"));
    }
}
