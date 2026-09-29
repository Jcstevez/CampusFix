package com.campusfix.observer;

import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.support.Ejemplos;
import com.campusfix.support.ObserverEspia;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IncidenciaSubjectTest {

    @Test
    void notificarSinObserversNoHaceNada() {
        IncidenciaSubject subject = new IncidenciaSubject();
        subject.notificarObservers(Ejemplos.tecnologica("A"));
    }

    @Test
    void todosLosObserversRecibenElEvento() {
        IncidenciaSubject subject = new IncidenciaSubject();
        ObserverEspia uno = new ObserverEspia();
        ObserverEspia dos = new ObserverEspia();
        subject.agregarObserver(uno);
        subject.agregarObserver(dos);
        Incidencia i = Ejemplos.tecnologica("A");

        subject.notificarObservers(i);

        assertEquals(1, uno.recibidas().size());
        assertEquals(1, dos.recibidas().size());
    }

    @Test
    void unObserverQueFallaNoImpideQueLosSiguientesSeEnteren() {
        // Arrange: el que falla va en medio
        IncidenciaSubject subject = new IncidenciaSubject();
        ObserverEspia antes = new ObserverEspia();
        ObserverEspia despues = new ObserverEspia();
        subject.agregarObserver(antes);
        subject.agregarObserver(ObserverEspia.queFalla());
        subject.agregarObserver(despues);

        // Act
        subject.notificarObservers(Ejemplos.tecnologica("A"));

        // Assert
        assertEquals(1, antes.recibidas().size());
        assertEquals(1, despues.recibidas().size());
    }
}
