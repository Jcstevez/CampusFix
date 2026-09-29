package com.campusfix.application;

import com.campusfix.application.port.in.ReportarIncidenciaUseCase.ReporteManual;
import com.campusfix.domain.clasificacion.Clasificacion;
import com.campusfix.domain.clasificacion.ClasificadorIncidencia;
import com.campusfix.domain.error.IncidenciaNoEncontradaException;
import com.campusfix.domain.incidencia.EstadoIncidencia;
import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.IncidenciaInfraestructura;
import com.campusfix.domain.incidencia.IncidenciaTecnologica;
import com.campusfix.domain.incidencia.Prioridad;
import com.campusfix.domain.incidencia.TipoIncidencia;
import com.campusfix.factory.IncidenciaCreator;
import com.campusfix.factory.TecnologiaCreator;
import com.campusfix.observer.IncidenciaSubject;
import com.campusfix.support.ObserverEspia;
import com.campusfix.support.RepositorioEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IncidenciaServiceTest {

    private RepositorioEnMemoria repositorio;
    private IncidenciaSubject eventos;
    private ObserverEspia espia;
    private ClasificadorIncidencia clasificadorFijo;
    private IncidenciaService service;
    private int secuencia;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioEnMemoria();
        eventos = new IncidenciaSubject();
        espia = new ObserverEspia();
        eventos.agregarObserver(espia);
        // doble de prueba (stub): siempre responde INFRAESTRUCTURA / BAJA
        clasificadorFijo = (t, d, u) ->
                new Clasificacion(TipoIncidencia.INFRAESTRUCTURA, Prioridad.BAJA);
        secuencia = 0;
        service = new IncidenciaService(
                repositorio, clasificadorFijo, eventos, () -> "INC-T" + (++secuencia));
    }

    // ---------- registrarIncidencia (Factory Method) ----------

    @Test
    void debeRegistrarUnaIncidenciaTecnologica() {
        // Arrange
        TecnologiaCreator creator = new TecnologiaCreator();

        // Act
        Incidencia incidencia = service.registrarIncidencia(
                creator, "INC-003", "Computador no enciende",
                "El computador del laboratorio no enciende",
                "Bloque C - Laboratorio 1", Prioridad.ALTA);

        // Assert
        assertInstanceOf(IncidenciaTecnologica.class, incidencia);
    }

    @Test
    void debeRegistrarUnaIncidenciaUsandoLaAbstraccionCreator() {
        // Arrange: un creador anonimo prueba que el servicio solo depende de la abstraccion
        IncidenciaCreator creator = new IncidenciaCreator() {
            @Override
            public Incidencia crearIncidencia(
                    String id, String titulo, String descripcion,
                    String ubicacion, Prioridad prioridad) {
                return new IncidenciaTecnologica(id, titulo, descripcion, ubicacion, prioridad);
            }
        };

        // Act
        Incidencia incidencia = service.registrarIncidencia(
                creator, "INC-004", "Pantalla sin senal",
                "La pantalla del laboratorio no muestra imagen",
                "Bloque C - Laboratorio 2", Prioridad.MEDIA);

        // Assert
        assertEquals("INC-004", incidencia.getId());
        assertEquals("Pantalla sin senal", incidencia.getTitulo());
        assertEquals(Prioridad.MEDIA, incidencia.getPrioridad());
    }

    @Test
    void registrarGuardaLaIncidenciaYAvisaALosObservers() {
        // Act
        Incidencia i = service.registrarIncidencia(
                new TecnologiaCreator(), "INC-9", "t", "d", "u", Prioridad.MEDIA);

        // Assert
        assertEquals(Optional.of(i), repositorio.buscarPorId("INC-9"));
        assertEquals(List.of(i), espia.recibidas());
    }

    @Test
    void siUnObserverFallaLaIncidenciaIgualQuedaGuardadaYSeDevuelve() {
        // Arrange
        eventos.agregarObserver(ObserverEspia.queFalla());

        // Act
        Incidencia i = service.registrarIncidencia(
                new TecnologiaCreator(), "INC-9", "t", "d", "u", Prioridad.MEDIA);

        // Assert
        assertEquals(1, repositorio.total());
        assertEquals("INC-9", i.getId());
    }

    // ---------- reportar (usa el clasificador) ----------

    @Test
    void reportarUsaElTipoYLaPrioridadDelClasificador() {
        // Act
        Incidencia i = service.reportar(new ReporteManual("Algo", "detalle", "Bloque A"));

        // Assert
        assertInstanceOf(IncidenciaInfraestructura.class, i);
        assertEquals(Prioridad.BAJA, i.getPrioridad());
        assertEquals("INC-T1", i.getId());
        assertEquals(EstadoIncidencia.ABIERTA, i.getEstado());
    }

    @Test
    void reportarPasaTituloDescripcionYUbicacionAlClasificador() {
        // Arrange
        String[] recibido = new String[3];
        ClasificadorIncidencia espiaClasificador = (t, d, u) -> {
            recibido[0] = t;
            recibido[1] = d;
            recibido[2] = u;
            return new Clasificacion(TipoIncidencia.TECNOLOGIA, Prioridad.ALTA);
        };
        IncidenciaService s = new IncidenciaService(
                repositorio, espiaClasificador, eventos, () -> "INC-X");

        // Act
        Incidencia i = s.reportar(new ReporteManual("Titulo", "Descripcion", "Ubicacion"));

        // Assert
        assertEquals("Titulo", recibido[0]);
        assertEquals("Descripcion", recibido[1]);
        assertEquals("Ubicacion", recibido[2]);
        assertInstanceOf(IncidenciaTecnologica.class, i);
    }

    @Test
    void reportarConDatosInvalidosNoGuardaNiAvisa() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.reportar(new ReporteManual("Titulo", "d", " ")));
        assertEquals(0, repositorio.total());
        assertEquals(0, espia.recibidas().size());
    }

    // ---------- consultar / listar ----------

    @Test
    void consultarDevuelveLaIncidenciaGuardada() {
        Incidencia creada = service.reportar(new ReporteManual("t", "d", "u"));
        assertEquals(creada, service.consultar(creada.getId()));
    }

    @Test
    void consultarUnaIncidenciaInexistenteLanzaNoEncontrada() {
        assertThrows(IncidenciaNoEncontradaException.class, () -> service.consultar("NO-EXISTE"));
    }

    @Test
    void listarSinFiltroDevuelveTodasLasIncidencias() {
        Incidencia a = service.reportar(new ReporteManual("a", "d", "u"));
        Incidencia b = service.reportar(new ReporteManual("b", "d", "u"));
        service.resolver(b.getId());

        assertEquals(2, service.listar(Optional.empty()).size());
    }

    @Test
    void listarConEstadoSoloDevuelveLasQueLoTienen() {
        Incidencia a = service.reportar(new ReporteManual("a", "d", "u"));
        Incidencia b = service.reportar(new ReporteManual("b", "d", "u"));
        service.resolver(b.getId());

        List<Incidencia> abiertas = service.listar(Optional.of(EstadoIncidencia.ABIERTA));

        assertEquals(List.of(a), abiertas);
    }

    // ---------- atender / resolver ----------

    @Test
    void atenderCambiaElEstadoYLoPersiste() {
        Incidencia i = service.reportar(new ReporteManual("t", "d", "u"));

        service.atender(i.getId());

        assertEquals(EstadoIncidencia.EN_PROCESO,
                repositorio.buscarPorId(i.getId()).orElseThrow().getEstado());
    }

    @Test
    void resolverCambiaElEstadoYLoPersiste() {
        Incidencia i = service.reportar(new ReporteManual("t", "d", "u"));

        service.resolver(i.getId());

        assertEquals(EstadoIncidencia.RESUELTA,
                repositorio.buscarPorId(i.getId()).orElseThrow().getEstado());
    }

    @Test
    void resolverUnaInexistenteLanzaNoEncontrada() {
        assertThrows(IncidenciaNoEncontradaException.class, () -> service.resolver("NO"));
    }

    @Test
    void resolverDosVecesLanzaEstadoIlegal() {
        Incidencia i = service.reportar(new ReporteManual("t", "d", "u"));
        service.resolver(i.getId());

        assertThrows(IllegalStateException.class, () -> service.resolver(i.getId()));
    }
}
