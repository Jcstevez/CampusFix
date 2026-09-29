package com.campusfix.application;

import com.campusfix.application.port.out.MantenimientoPuerto;
import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.Prioridad;
import com.campusfix.support.Ejemplos;
import com.campusfix.support.NotificadorEspia;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reto 1: alertas y correos al reportar. */
class ObserversDeNotificacionTest {

    @Test
    void cadaReporteGeneraUnaAlertaEnLaApp() {
        // Arrange
        NotificadorEspia alertas = new NotificadorEspia();
        AlertaObjetoReportadoObserver observer = new AlertaObjetoReportadoObserver(alertas);
        Incidencia i = Ejemplos.tecnologica("INC-1", Prioridad.ALTA, Ejemplos.T0);

        // Act
        observer.actualizar(i);

        // Assert
        assertEquals(1, alertas.recibidas().size());
        Notificacion n = alertas.recibidas().get(0);
        assertEquals(Canal.APP, n.canal());
        assertTrue(n.asunto().contains("Proyector no funciona"));
        assertTrue(n.mensaje().contains("INC-1"));
    }

    @Test
    void unaIncidenciaTecnologicaEnviaCorreoAlAreaDeTi() {
        NotificadorEspia correo = new NotificadorEspia();
        CorreoTecnologiaObserver observer = new CorreoTecnologiaObserver(correo, "ti@campus.test");

        observer.actualizar(Ejemplos.tecnologica("INC-1", Prioridad.ALTA, Ejemplos.T0));

        assertEquals(1, correo.recibidas().size());
        Notificacion n = correo.recibidas().get(0);
        assertEquals(Canal.CORREO, n.canal());
        assertEquals("ti@campus.test", n.destinatario());
        assertTrue(n.asunto().contains("ALTA"));
    }

    @Test
    void unaIncidenciaDeInfraestructuraNoEnviaCorreoDeTi() {
        NotificadorEspia correo = new NotificadorEspia();
        CorreoTecnologiaObserver observer = new CorreoTecnologiaObserver(correo, "ti@campus.test");

        observer.actualizar(Ejemplos.infraestructura("INC-2"));

        assertEquals(0, correo.recibidas().size());
    }

    @Test
    void unaIncidenciaDeInfraestructuraSeEnviaAlSistemaDeMantenimiento() {
        List<Incidencia> enviadas = new ArrayList<>();
        MantenimientoPuerto mantenimiento = enviadas::add;
        MantenimientoInfraestructuraObserver observer =
                new MantenimientoInfraestructuraObserver(mantenimiento);
        Incidencia i = Ejemplos.infraestructura("INC-2");

        observer.actualizar(i);

        assertEquals(List.of(i), enviadas);
    }

    @Test
    void unaIncidenciaTecnologicaNoSeEnviaAlSistemaDeMantenimiento() {
        List<Incidencia> enviadas = new ArrayList<>();
        MantenimientoInfraestructuraObserver observer =
                new MantenimientoInfraestructuraObserver(enviadas::add);

        observer.actualizar(Ejemplos.tecnologica("INC-1"));

        assertEquals(0, enviadas.size());
    }
}
