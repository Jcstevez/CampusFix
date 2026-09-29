package com.campusfix.domain.catalogo;

import com.campusfix.domain.incidencia.TipoIncidencia;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObjetoCampusTest {

    @Test
    void seCreaConTodosSusDatos() {
        ObjetoCampus o = new ObjetoCampus("P1", "Proyector", "Aula 1", TipoIncidencia.TECNOLOGIA);
        assertEquals("Proyector", o.nombre());
    }

    @Test
    void noPermiteUnNombreVacio() {
        assertThrows(IllegalArgumentException.class, () ->
                new ObjetoCampus("P1", " ", "Aula 1", TipoIncidencia.TECNOLOGIA));
    }

    @Test
    void noPermiteUnTipoNulo() {
        assertThrows(IllegalArgumentException.class, () ->
                new ObjetoCampus("P1", "Proyector", "Aula 1", null));
    }
}
