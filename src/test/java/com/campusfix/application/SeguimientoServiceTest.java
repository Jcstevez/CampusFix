package com.campusfix.application;

import com.campusfix.domain.incidencia.EstadoIncidencia;
import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.Prioridad;
import com.campusfix.domain.seguimiento.PoliticaSla;
import com.campusfix.support.Ejemplos;
import com.campusfix.support.NotificadorEspia;
import com.campusfix.support.RelojMutable;
import com.campusfix.support.RepositorioEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reto 1: que los reportes no queden en el olvido. */
class SeguimientoServiceTest {

    private static final String CORREO_TI = "ti@campus.test";
    private static final String CORREO_MANT = "mant@campus.test";

    private RepositorioEnMemoria repositorio;
    private RelojMutable reloj;
    private NotificadorEspia correo;
    private NotificadorEspia alertas;
    private SeguimientoService service;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioEnMemoria();
        reloj = new RelojMutable(Ejemplos.T0);
        correo = new NotificadorEspia();
        alertas = new NotificadorEspia();
        service = crear(correo, alertas);
    }

    private SeguimientoService crear(NotificadorEspia c, NotificadorEspia a) {
        return new SeguimientoService(
                repositorio, PoliticaSla.porDefecto(), reloj, c, a, CORREO_TI, CORREO_MANT);
    }

    @Test
    void sinIncidenciasNoEscalaNada() {
        assertEquals(0, service.revisarPendientes());
    }

    @Test
    void unaAltaAUnMinutoDelPlazoNoSeEscala() {
        repositorio.guardar(Ejemplos.tecnologica("A", Prioridad.ALTA, Ejemplos.T0));
        reloj.avanzar(Duration.ofHours(2).minusMinutes(1));

        assertEquals(0, service.revisarPendientes());
    }

    @Test
    void unaAltaJustoEnElPlazoDeDosHorasSeEscala() {
        repositorio.guardar(Ejemplos.tecnologica("A", Prioridad.ALTA, Ejemplos.T0));
        reloj.avanzar(Duration.ofHours(2));

        assertEquals(1, service.revisarPendientes());
    }

    @Test
    void elPlazoDependeDeLaPrioridad() {
        // Arrange: a las 3 horas la ALTA (2 h) esta vencida y la MEDIA (24 h) no
        repositorio.guardar(Ejemplos.tecnologica("ALTA", Prioridad.ALTA, Ejemplos.T0));
        repositorio.guardar(Ejemplos.tecnologica("MEDIA", Prioridad.MEDIA, Ejemplos.T0));
        repositorio.guardar(Ejemplos.tecnologica("BAJA", Prioridad.BAJA, Ejemplos.T0));
        reloj.avanzar(Duration.ofHours(3));

        // Act
        int escaladas = service.revisarPendientes();

        // Assert
        assertEquals(1, escaladas);
        assertEquals(EstadoIncidencia.ESCALADA, repositorio.buscarPorId("ALTA").orElseThrow().getEstado());
        assertEquals(EstadoIncidencia.ABIERTA, repositorio.buscarPorId("MEDIA").orElseThrow().getEstado());
        assertEquals(EstadoIncidencia.ABIERTA, repositorio.buscarPorId("BAJA").orElseThrow().getEstado());
    }

    @Test
    void laMediaSeEscalaJustoALas24HorasYLaBajaALas72() {
        repositorio.guardar(Ejemplos.tecnologica("MEDIA", Prioridad.MEDIA, Ejemplos.T0));
        repositorio.guardar(Ejemplos.tecnologica("BAJA", Prioridad.BAJA, Ejemplos.T0));

        reloj.avanzar(Duration.ofHours(24));
        assertEquals(1, service.revisarPendientes());

        reloj.avanzar(Duration.ofHours(48));
        assertEquals(1, service.revisarPendientes());
    }

    @Test
    void laEscalacionQuedaPersistida() {
        repositorio.guardar(Ejemplos.tecnologica("A", Prioridad.ALTA, Ejemplos.T0));
        reloj.avanzar(Duration.ofHours(5));

        service.revisarPendientes();

        assertEquals(EstadoIncidencia.ESCALADA, repositorio.buscarPorId("A").orElseThrow().getEstado());
    }

    @Test
    void unaIncidenciaEnProcesoVencidaTambienSeEscala() {
        Incidencia i = Ejemplos.tecnologica("A", Prioridad.ALTA, Ejemplos.T0);
        i.iniciarAtencion();
        repositorio.guardar(i);
        reloj.avanzar(Duration.ofHours(2));

        assertEquals(1, service.revisarPendientes());
    }

    @Test
    void lasIncidenciasResueltasSeIgnoran() {
        Incidencia i = Ejemplos.tecnologica("A", Prioridad.ALTA, Ejemplos.T0);
        i.resolver();
        repositorio.guardar(i);
        reloj.avanzar(Duration.ofDays(10));

        assertEquals(0, service.revisarPendientes());
    }

    @Test
    void unaTecnologicaAvisaPorAppYPorCorreoAlAreaDeTi() {
        repositorio.guardar(Ejemplos.tecnologica("INC-TI", Prioridad.ALTA, Ejemplos.T0));
        reloj.avanzar(Duration.ofHours(3));

        service.revisarPendientes();

        assertEquals(1, correo.recibidas().size());
        Notificacion c = correo.recibidas().get(0);
        assertEquals(Canal.CORREO, c.canal());
        assertEquals(CORREO_TI, c.destinatario());
        assertTrue(c.mensaje().contains("INC-TI"));
        assertTrue(c.mensaje().contains("3 h"));
        assertEquals(1, alertas.recibidas().size());
        assertEquals(Canal.APP, alertas.recibidas().get(0).canal());
    }

    @Test
    void unaDeInfraestructuraAvisaAlAreaDeMantenimiento() {
        repositorio.guardar(Ejemplos.infraestructura("INC-INF", Prioridad.ALTA, Ejemplos.T0));
        reloj.avanzar(Duration.ofHours(3));

        service.revisarPendientes();

        assertEquals(CORREO_MANT, correo.recibidas().get(0).destinatario());
    }

    @Test
    void unaIncidenciaYaEscaladaNoGeneraUnNuevoAviso() {
        repositorio.guardar(Ejemplos.tecnologica("A", Prioridad.ALTA, Ejemplos.T0));
        reloj.avanzar(Duration.ofHours(3));
        service.revisarPendientes();

        int segundaRevision = service.revisarPendientes();

        assertEquals(0, segundaRevision);
        assertEquals(1, correo.recibidas().size());
    }

    @Test
    void siElAvisoFallaLaIncidenciaIgualQuedaEscaladaYSeSiguenRevisandoLasDemas() {
        // Arrange
        SeguimientoService conFalla = crear(new NotificadorEspia().fallando(), alertas);
        repositorio.guardar(Ejemplos.tecnologica("A", Prioridad.ALTA, Ejemplos.T0));
        repositorio.guardar(Ejemplos.tecnologica("B", Prioridad.ALTA, Ejemplos.T0));
        reloj.avanzar(Duration.ofHours(3));

        // Act
        int escaladas = conFalla.revisarPendientes();

        // Assert
        assertEquals(2, escaladas);
        assertEquals(EstadoIncidencia.ESCALADA, repositorio.buscarPorId("A").orElseThrow().getEstado());
        assertEquals(EstadoIncidencia.ESCALADA, repositorio.buscarPorId("B").orElseThrow().getEstado());
    }
}
