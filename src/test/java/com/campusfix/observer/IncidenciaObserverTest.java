package com.campusfix.observer;

import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.IncidenciaTecnologica;
import com.campusfix.domain.incidencia.Prioridad;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IncidenciaObserverTest {

    @Test
    void debeNotificarAlObserverCuandoSeRegistraUnaIncidencia() {

        IncidenciaSubject subject = new IncidenciaSubject();
        ObserverPrueba observer = new ObserverPrueba();

        subject.agregarObserver(observer);

        Incidencia incidencia = new IncidenciaTecnologica(
                "INC-004",
                "Computador no enciende",
                "El computador del laboratorio no enciende",
                "Bloque C - Laboratorio 1",
                Prioridad.ALTA
        );

        subject.notificarObservers(incidencia);

        assertEquals(incidencia, observer.incidenciaRecibida);
    }

    private static class ObserverPrueba implements IncidenciaObserver {

        private Incidencia incidenciaRecibida;

        @Override
        public void actualizar(Incidencia incidencia) {
            this.incidenciaRecibida = incidencia;
        }
    }
}