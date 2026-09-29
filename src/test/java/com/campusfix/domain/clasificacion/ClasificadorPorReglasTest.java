package com.campusfix.domain.clasificacion;

import com.campusfix.domain.incidencia.Prioridad;
import com.campusfix.domain.incidencia.TipoIncidencia;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Reto 2: clasificacion automatica de tipo y prioridad. */
class ClasificadorPorReglasTest {

    private final ClasificadorIncidencia clasificador = new ClasificadorPorReglas();

    private Clasificacion clasificar(String titulo, String descripcion, String ubicacion) {
        return clasificador.clasificar(titulo, descripcion, ubicacion);
    }

    // ---------- tipo: clase de equivalencia TECNOLOGIA ----------

    @ParameterizedTest
    @CsvSource({
            "Proyector no funciona, El proyector del aula no enciende, Bloque B - Aula 302",
            "Sin WiFi, No hay internet en la sala, Bloque A",
            "Computador dañado, El teclado no responde, Bloque D",
            "PROYECTOR SIN SEÑAL, ' ', Bloque B",
            "Computadores del salon no encienden, ' ', Bloque D",
            "Falla en la plataforma, No carga moodle, Bloque E"
    })
    void unReporteConPalabrasDeTecnologiaSeClasificaComoTecnologia(
            String titulo, String descripcion, String ubicacion) {

        assertEquals(TipoIncidencia.TECNOLOGIA,
                clasificar(titulo, descripcion, ubicacion).tipo());
    }

    // ---------- tipo: clase de equivalencia INFRAESTRUCTURA ----------

    @ParameterizedTest
    @CsvSource({
            "Puerta dañada, La cerradura no cierra, Bloque A - Aula 201",
            "Silla rota, Falta una silla en el salon, Bloque C",
            "Goteras en el techo, ' ', Bloque B",
            "Ventana quebrada, El vidrio esta roto, Bloque A",
            "Aire acondicionado no enfria, ' ', Bloque D"
    })
    void unReporteConPalabrasDeInfraestructuraSeClasificaComoInfraestructura(
            String titulo, String descripcion, String ubicacion) {

        assertEquals(TipoIncidencia.INFRAESTRUCTURA,
                clasificar(titulo, descripcion, ubicacion).tipo());
    }

    // ---------- tipo: casos borde ----------

    @Test
    void sinPalabrasReconocidasSeAsignaInfraestructuraPorDefecto() {
        Clasificacion c = clasificar("Algo raro pasa", "No se que es", "Bloque A");
        assertEquals(TipoIncidencia.INFRAESTRUCTURA, c.tipo());
    }

    @Test
    void conEmpateEntreCategoriasSeAsignaInfraestructura() {
        // "proyector" (tecnologia) y "luz" (infraestructura) pesan igual en el titulo
        Clasificacion c = clasificar("Luz del proyector", "", "Bloque A");
        assertEquals(TipoIncidencia.INFRAESTRUCTURA, c.tipo());
    }

    @Test
    void lasPalabrasDelTituloPesanMasQueLasDeLaDescripcion() {
        // Arrange: 1 palabra tecnologica en el titulo (x2) vs 1 de infraestructura en la descripcion
        // Act
        Clasificacion c = clasificar("Proyector", "junto a la puerta", "Bloque A");
        // Assert
        assertEquals(TipoIncidencia.TECNOLOGIA, c.tipo());
    }

    @Test
    void lasPalabrasCortasSoloCoincidenExactas() {
        // "reducido" empieza con "red" pero no es la palabra "red"
        Clasificacion c = clasificar("Espacio reducido", "en la mesa", "Bloque A");
        assertEquals(TipoIncidencia.INFRAESTRUCTURA, c.tipo());
    }

    @Test
    void laPalabraPcCortaCoincideExacta() {
        assertEquals(TipoIncidencia.TECNOLOGIA,
                clasificar("Mi pc no prende", "", "Bloque A").tipo());
    }

    // ---------- prioridad ----------

    @ParameterizedTest
    @CsvSource({
            "Fuga de gas, Huele fuerte, Bloque A",
            "Cortocircuito en el aula, Salieron chispas, Bloque B",
            "Humo saliendo del computador, ' ', Bloque C",
            "Sin luz en el pasillo, ' ', Bloque D",
            "Ascensor con persona atrapada, ' ', Bloque A",
            "Reporte urgente, La escalera esta suelta, Bloque B"
    })
    void lasPalabrasCriticasDanPrioridadAlta(
            String titulo, String descripcion, String ubicacion) {

        assertEquals(Prioridad.ALTA, clasificar(titulo, descripcion, ubicacion).prioridad());
    }

    @ParameterizedTest
    @CsvSource({
            "Pared rayada, ' ', Bloque A",
            "Silla manchada, Esta desgastada, Bloque B",
            "Sugerencia: cambiar el color, ' ', Bloque C"
    })
    void lasPalabrasMenoresDanPrioridadBaja(
            String titulo, String descripcion, String ubicacion) {

        assertEquals(Prioridad.BAJA, clasificar(titulo, descripcion, ubicacion).prioridad());
    }

    @Test
    void unaFallaComunEnUbicacionNormalEsPrioridadMedia() {
        Clasificacion c = clasificar("Proyector no funciona", "No enciende", "Bloque B - Aula 302");
        assertEquals(Prioridad.MEDIA, c.prioridad());
    }

    @ParameterizedTest
    @CsvSource({
            "Bloque C - Laboratorio 101",
            "Biblioteca - Piso 2",
            "Auditorio principal"
    })
    void unaFallaComunEnUbicacionSensibleSubeAPrioridadAlta(String ubicacion) {
        assertEquals(Prioridad.ALTA,
                clasificar("Proyector no funciona", "No enciende", ubicacion).prioridad());
    }

    @Test
    void laPrioridadCriticaGanaSobreLaMenor() {
        Clasificacion c = clasificar("Pared rayada", "Hay riesgo de caida", "Bloque A");
        assertEquals(Prioridad.ALTA, c.prioridad());
    }

    @Test
    void laUbicacionSensibleNoSubeUnaPrioridadBaja() {
        Clasificacion c = clasificar("Pared rayada", "", "Biblioteca");
        assertEquals(Prioridad.BAJA, c.prioridad());
    }

    @Test
    void laPalabraGasNoCoincideDentroDeOtraPalabra() {
        // "gastos" no es "gas": no debe disparar prioridad alta
        Clasificacion c = clasificar("Informe de gastos del aula", "", "Bloque A");
        assertEquals(Prioridad.MEDIA, c.prioridad());
    }

    // ---------- normalizacion y entradas invalidas ----------

    @Test
    void ignoraTildesYMayusculas() {
        assertEquals(Prioridad.ALTA,
                clasificar("INUNDACIÓN EN EL SÓTANO", "", "Bloque A").prioridad());
    }

    @Test
    void unTituloNuloEsInvalido() {
        assertThrows(IllegalArgumentException.class, () -> clasificar(null, "d", "u"));
    }

    @Test
    void unTituloEnBlancoEsInvalido() {
        assertThrows(IllegalArgumentException.class, () -> clasificar("   ", "d", "u"));
    }

    @Test
    void laDescripcionYLaUbicacionNulasSeToleran() {
        Clasificacion c = clasificar("Proyector roto", null, null);
        assertEquals(TipoIncidencia.TECNOLOGIA, c.tipo());
        assertEquals(Prioridad.MEDIA, c.prioridad());
    }
}
