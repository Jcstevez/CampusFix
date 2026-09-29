package com.campusfix.domain.incidencia;

import com.campusfix.support.Ejemplos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncidenciaTest {

    private static Incidencia conTitulo(String titulo) {
        return new IncidenciaTecnologica("INC-1", titulo, "desc", "Bloque A", Prioridad.MEDIA);
    }

    // ---------- creacion: clases de equivalencia ----------

    @Test
    @DisplayName("Una incidencia nueva nace ABIERTA y guarda sus datos")
    void unaIncidenciaNuevaNaceAbierta() {
        // Arrange + Act
        Incidencia i = new IncidenciaTecnologica(
                "INC-1", "  Proyector  ", "No enciende", "Bloque B", Prioridad.ALTA);

        // Assert
        assertEquals(EstadoIncidencia.ABIERTA, i.getEstado());
        assertEquals("Proyector", i.getTitulo());
        assertEquals(TipoIncidencia.TECNOLOGIA, i.tipo());
    }

    @Test
    void laInfraestructuraReportaSuTipo() {
        assertEquals(TipoIncidencia.INFRAESTRUCTURA, Ejemplos.infraestructura("X").tipo());
    }

    @Test
    void unTituloNuloEsInvalido() {
        assertThrows(IllegalArgumentException.class, () -> conTitulo(null));
    }

    @Test
    void unTituloVacioEsInvalido() {
        assertThrows(IllegalArgumentException.class, () -> conTitulo(""));
    }

    @Test
    void unTituloSoloConEspaciosEsInvalido() {
        assertThrows(IllegalArgumentException.class, () -> conTitulo("   "));
    }

    @Test
    void unaUbicacionVaciaEsInvalida() {
        assertThrows(IllegalArgumentException.class, () ->
                new IncidenciaTecnologica("INC-1", "t", "d", " ", Prioridad.MEDIA));
    }

    @Test
    void unaPrioridadNulaEsInvalida() {
        assertThrows(IllegalArgumentException.class, () ->
                new IncidenciaTecnologica("INC-1", "t", "d", "u", null));
    }

    @Test
    void unaDescripcionNulaSeConvierteEnTextoVacio() {
        Incidencia i = new IncidenciaTecnologica("INC-1", "t", null, "u", Prioridad.BAJA);
        assertEquals("", i.getDescripcion());
    }

    // ---------- valores limite de longitud ----------

    @Test
    void unTituloDeExactamente200CaracteresEsValido() {
        assertEquals(200, conTitulo("a".repeat(200)).getTitulo().length());
    }

    @Test
    void unTituloDe201CaracteresEsInvalido() {
        assertThrows(IllegalArgumentException.class, () -> conTitulo("a".repeat(201)));
    }

    @Test
    void unaDescripcionDe2000CaracteresEsValida() {
        Incidencia i = new IncidenciaTecnologica(
                "INC-1", "t", "d".repeat(2000), "u", Prioridad.MEDIA);
        assertEquals(2000, i.getDescripcion().length());
    }

    @Test
    void unaDescripcionDe2001CaracteresEsInvalida() {
        assertThrows(IllegalArgumentException.class, () ->
                new IncidenciaTecnologica("INC-1", "t", "d".repeat(2001), "u", Prioridad.MEDIA));
    }

    // ---------- transiciones de estado ----------

    @Test
    void resolverUnaIncidenciaAbiertaLaDejaResuelta() {
        Incidencia i = Ejemplos.tecnologica("A");
        i.resolver();
        assertEquals(EstadoIncidencia.RESUELTA, i.getEstado());
    }

    @Test
    void resolverDosVecesEsUnEstadoIlegal() {
        Incidencia i = Ejemplos.tecnologica("A");
        i.resolver();
        assertThrows(IllegalStateException.class, i::resolver);
    }

    @Test
    void escalarUnaIncidenciaAbiertaLaDejaEscalada() {
        Incidencia i = Ejemplos.tecnologica("A");
        i.escalar();
        assertEquals(EstadoIncidencia.ESCALADA, i.getEstado());
    }

    @Test
    void noSePuedeEscalarUnaIncidenciaResuelta() {
        Incidencia i = Ejemplos.tecnologica("A");
        i.resolver();
        assertThrows(IllegalStateException.class, i::escalar);
    }

    @Test
    void noSePuedeEscalarDosVeces() {
        Incidencia i = Ejemplos.tecnologica("A");
        i.escalar();
        assertThrows(IllegalStateException.class, i::escalar);
    }

    @Test
    void atenderUnaIncidenciaAbiertaLaPasaAEnProceso() {
        Incidencia i = Ejemplos.tecnologica("A");
        i.iniciarAtencion();
        assertEquals(EstadoIncidencia.EN_PROCESO, i.getEstado());
    }

    @Test
    void atenderUnaIncidenciaEscaladaLaPasaAEnProceso() {
        Incidencia i = Ejemplos.tecnologica("A");
        i.escalar();
        i.iniciarAtencion();
        assertEquals(EstadoIncidencia.EN_PROCESO, i.getEstado());
    }

    @Test
    void noSePuedeAtenderUnaIncidenciaYaEnProceso() {
        Incidencia i = Ejemplos.tecnologica("A");
        i.iniciarAtencion();
        assertThrows(IllegalStateException.class, i::iniciarAtencion);
    }

    @Test
    void noSePuedeAtenderUnaIncidenciaResuelta() {
        Incidencia i = Ejemplos.tecnologica("A");
        i.resolver();
        assertThrows(IllegalStateException.class, i::iniciarAtencion);
    }

    // ---------- vencimiento: valores limite del plazo ----------

    private static final Duration PLAZO = Duration.ofHours(2);

    @Test
    void unMilisegundoAntesDelPlazoNoEstaVencida() {
        Incidencia i = Ejemplos.tecnologica("A");
        Instant ahora = Ejemplos.T0.plus(PLAZO).minusMillis(1);
        assertFalse(i.estaVencida(PLAZO, ahora));
    }

    @Test
    void justoEnElPlazoEstaVencida() {
        Incidencia i = Ejemplos.tecnologica("A");
        assertTrue(i.estaVencida(PLAZO, Ejemplos.T0.plus(PLAZO)));
    }

    @Test
    void despuesDelPlazoEstaVencida() {
        Incidencia i = Ejemplos.tecnologica("A");
        assertTrue(i.estaVencida(PLAZO, Ejemplos.T0.plus(PLAZO).plusSeconds(1)));
    }

    @Test
    void unaIncidenciaEnProcesoTambienPuedeVencer() {
        Incidencia i = Ejemplos.tecnologica("A");
        i.iniciarAtencion();
        assertTrue(i.estaVencida(PLAZO, Ejemplos.T0.plus(PLAZO)));
    }

    @Test
    void unaIncidenciaResueltaNuncaEstaVencida() {
        Incidencia i = Ejemplos.tecnologica("A");
        i.resolver();
        assertFalse(i.estaVencida(PLAZO, Ejemplos.T0.plus(Duration.ofDays(30))));
    }

    @Test
    void unaIncidenciaYaEscaladaNoVuelveAVencer() {
        Incidencia i = Ejemplos.tecnologica("A");
        i.escalar();
        assertFalse(i.estaVencida(PLAZO, Ejemplos.T0.plus(Duration.ofDays(30))));
    }
}
