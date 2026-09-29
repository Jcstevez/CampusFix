package com.campusfix.application;

import com.campusfix.application.port.out.CatalogoObjetos;
import com.campusfix.application.port.out.DecodificadorQr;
import com.campusfix.application.port.out.GeneradorQr;
import com.campusfix.domain.catalogo.ObjetoCampus;
import com.campusfix.domain.clasificacion.ClasificadorPorReglas;
import com.campusfix.domain.error.CodigoQrInvalidoException;
import com.campusfix.domain.error.ObjetoNoEncontradoException;
import com.campusfix.domain.error.QrNoLegibleException;
import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.Prioridad;
import com.campusfix.domain.incidencia.TipoIncidencia;
import com.campusfix.observer.IncidenciaSubject;
import com.campusfix.support.ObserverEspia;
import com.campusfix.support.RepositorioEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Reto 3: reportar escaneando el QR de un objeto. */
class ReporteQrServiceTest {

    private static final ObjetoCampus PROYECTOR = new ObjetoCampus(
            "PROY-B302", "Proyector", "Bloque B - Aula 302", TipoIncidencia.TECNOLOGIA);
    private static final ObjetoCampus PUERTA = new ObjetoCampus(
            "PUERTA-A201", "Puerta del aula", "Bloque A - Aula 201", TipoIncidencia.INFRAESTRUCTURA);

    private RepositorioEnMemoria repositorio;
    private ObserverEspia espia;
    private String textoQuePideGenerar;
    private byte[] imagenQueRecibioElDecodificador;
    private ReporteQrService service;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioEnMemoria();
        IncidenciaSubject eventos = new IncidenciaSubject();
        espia = new ObserverEspia();
        eventos.agregarObserver(espia);

        // stubs de los puertos de salida
        Map<String, ObjetoCampus> datos = Map.of(
                PROYECTOR.id(), PROYECTOR, PUERTA.id(), PUERTA);
        CatalogoObjetos catalogo = id -> Optional.ofNullable(datos.get(id));

        DecodificadorQr decodificador = imagen -> {
            imagenQueRecibioElDecodificador = imagen;
            if (imagen.length == 0) {
                throw new QrNoLegibleException("vacia");
            }
            return "campusfix://objeto/PROY-B302";
        };
        GeneradorQr generador = contenido -> {
            textoQuePideGenerar = contenido;
            return new byte[]{1, 2, 3};
        };

        ClasificadorPorReglas clasificador = new ClasificadorPorReglas();
        IncidenciaService incidencias = new IncidenciaService(
                repositorio, clasificador, eventos, () -> "INC-QR-1");
        service = new ReporteQrService(incidencias, catalogo, decodificador, generador, clasificador);
    }

    @Test
    void escanearElQrDeUnProyectorCreaUnaIncidenciaTecnologicaConSuUbicacion() {
        // Act
        Incidencia i = service.reportarPorContenido(
                "campusfix://objeto/PROY-B302", "No enciende");

        // Assert
        assertEquals(TipoIncidencia.TECNOLOGIA, i.tipo());
        assertEquals("Reporte: Proyector", i.getTitulo());
        assertEquals("Bloque B - Aula 302", i.getUbicacion());
        assertEquals("No enciende", i.getDescripcion());
        assertEquals(Prioridad.MEDIA, i.getPrioridad());
    }

    @Test
    void escanearElQrDeUnaPuertaCreaUnaIncidenciaDeInfraestructura() {
        Incidencia i = service.reportarPorContenido("campusfix://objeto/PUERTA-A201", "No cierra");
        assertEquals(TipoIncidencia.INFRAESTRUCTURA, i.tipo());
    }

    @Test
    void bastaConEscanearNoHaceFaltaDescripcion() {
        Incidencia i = service.reportarPorContenido("campusfix://objeto/PROY-B302", null);
        assertEquals("", i.getDescripcion());
    }

    @Test
    void laPrioridadSaleDeLaDescripcionDelUsuario() {
        Incidencia i = service.reportarPorContenido(
                "campusfix://objeto/PROY-B302", "Sale humo del aparato");
        assertEquals(Prioridad.ALTA, i.getPrioridad());
    }

    @Test
    void elReporteQuedaGuardadoYSeAvisaALosObservers() {
        Incidencia i = service.reportarPorContenido("campusfix://objeto/PROY-B302", "x");

        assertEquals(Optional.of(i), repositorio.buscarPorId("INC-QR-1"));
        assertEquals(1, espia.recibidas().size());
    }

    @Test
    void unQrQueNoEsDeCampusfixSeRechazaSinGuardarNada() {
        assertThrows(CodigoQrInvalidoException.class,
                () -> service.reportarPorContenido("http://otra-cosa.com", "x"));
        assertEquals(0, repositorio.total());
    }

    @Test
    void unQrDeUnObjetoQueNoExisteSeRechaza() {
        assertThrows(ObjetoNoEncontradoException.class,
                () -> service.reportarPorContenido("campusfix://objeto/NO-EXISTE", "x"));
        assertEquals(0, repositorio.total());
    }

    @Test
    void alReportarPorImagenSeDecodificaYSeCreaLaIncidencia() {
        // Arrange
        byte[] imagen = {9, 9, 9};

        // Act
        Incidencia i = service.reportarPorImagen(imagen, "No prende");

        // Assert
        assertSame(imagen, imagenQueRecibioElDecodificador);
        assertEquals("Bloque B - Aula 302", i.getUbicacion());
    }

    @Test
    void siLaImagenNoTieneUnQrLegibleSePropagaElError() {
        assertThrows(QrNoLegibleException.class,
                () -> service.reportarPorImagen(new byte[0], "x"));
        assertEquals(0, repositorio.total());
    }

    @Test
    void generarElQrDeUnObjetoPideAlGeneradorElContenidoCorrecto() {
        byte[] png = service.generarQrDeObjeto("PROY-B302");

        assertEquals("campusfix://objeto/PROY-B302", textoQuePideGenerar);
        assertArrayEquals(new byte[]{1, 2, 3}, png);
    }

    @Test
    void generarElQrDeUnObjetoInexistenteLanzaNoEncontrado() {
        assertThrows(ObjetoNoEncontradoException.class, () -> service.generarQrDeObjeto("NO"));
    }
}
