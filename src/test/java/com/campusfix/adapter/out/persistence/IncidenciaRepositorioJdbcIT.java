package com.campusfix.adapter.out.persistence;

import com.campusfix.domain.incidencia.EstadoIncidencia;
import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.IncidenciaInfraestructura;
import com.campusfix.domain.incidencia.IncidenciaTecnologica;
import com.campusfix.domain.incidencia.Prioridad;
import com.campusfix.domain.incidencia.TipoIncidencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integracion real: el adaptador JDBC contra una base H2 embebida.
 * Cada prueba usa su propia base (nombre unico) para no depender del orden.
 */
class IncidenciaRepositorioJdbcIT {

    private static final Instant T0 = Instant.parse("2026-09-28T12:00:00Z");

    private IncidenciaRepositorioJdbc repositorio;

    @BeforeEach
    void baseNueva() {
        repositorio = new IncidenciaRepositorioJdbc(
                "jdbc:h2:mem:repo-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
    }

    private static Incidencia tecnologica(String id, Instant creadaEn) {
        return new IncidenciaTecnologica(id, "Proyector", "No enciende",
                "Bloque B - Aula 302", Prioridad.ALTA, creadaEn, EstadoIncidencia.ABIERTA);
    }

    @Test
    void guardarYLeerUnaTecnologicaConservaTodosSusDatos() {
        // Arrange
        Incidencia original = tecnologica("INC-1", T0);

        // Act
        repositorio.guardar(original);
        Incidencia leida = repositorio.buscarPorId("INC-1").orElseThrow();

        // Assert
        assertInstanceOf(IncidenciaTecnologica.class, leida);
        assertEquals(TipoIncidencia.TECNOLOGIA, leida.tipo());
        assertEquals("Proyector", leida.getTitulo());
        assertEquals("No enciende", leida.getDescripcion());
        assertEquals("Bloque B - Aula 302", leida.getUbicacion());
        assertEquals(Prioridad.ALTA, leida.getPrioridad());
        assertEquals(EstadoIncidencia.ABIERTA, leida.getEstado());
        assertEquals(T0, leida.getCreadaEn());
    }

    @Test
    void guardarYLeerUnaDeInfraestructuraLaReconstruyeConSuTipo() {
        repositorio.guardar(new IncidenciaInfraestructura("INC-2", "Puerta", "No cierra",
                "Bloque A", Prioridad.BAJA, T0, EstadoIncidencia.ABIERTA));

        Incidencia leida = repositorio.buscarPorId("INC-2").orElseThrow();

        assertInstanceOf(IncidenciaInfraestructura.class, leida);
    }

    @Test
    void guardarDeNuevoUnaIncidenciaExistenteLaActualizaSinDuplicar() {
        // Arrange
        Incidencia i = tecnologica("INC-3", T0);
        repositorio.guardar(i);

        // Act
        i.resolver();
        repositorio.guardar(i);

        // Assert
        assertEquals(EstadoIncidencia.RESUELTA,
                repositorio.buscarPorId("INC-3").orElseThrow().getEstado());
        assertEquals(1, repositorio.listarPorEstados(Set.of(EstadoIncidencia.values())).size());
    }

    @Test
    void buscarUnaIncidenciaQueNoExisteDevuelveVacio() {
        assertEquals(Optional.empty(), repositorio.buscarPorId("NO-EXISTE"));
    }

    @Test
    void listarPorEstadosFiltraYOrdenaDeLaMasAntiguaALaMasReciente() {
        // Arrange
        repositorio.guardar(tecnologica("C", T0.plusSeconds(30)));
        repositorio.guardar(tecnologica("A", T0));
        repositorio.guardar(tecnologica("B", T0.plusSeconds(10)));
        Incidencia resuelta = tecnologica("D", T0.plusSeconds(20));
        resuelta.resolver();
        repositorio.guardar(resuelta);

        // Act
        List<Incidencia> abiertas = repositorio.listarPorEstados(Set.of(EstadoIncidencia.ABIERTA));

        // Assert
        assertEquals(List.of("A", "B", "C"), abiertas.stream().map(Incidencia::getId).toList());
    }

    @Test
    void listarConUnaColeccionVaciaDeEstadosDevuelveVacio() {
        repositorio.guardar(tecnologica("A", T0));
        assertTrue(repositorio.listarPorEstados(Set.of()).isEmpty());
    }

    @Test
    void cadaPruebaEmpiezaConLaBaseVacia() {
        assertTrue(repositorio.listarPorEstados(Set.of(EstadoIncidencia.values())).isEmpty());
    }

    @Test
    void unTextoConTildesYEnieViajaIntactoPorLaBaseDeDatos() {
        repositorio.guardar(new IncidenciaInfraestructura("INC-N", "Baño sin señal",
                "Ñandú en el pasillo, ¿qué pasó?", "Bloque Ó", Prioridad.MEDIA,
                T0, EstadoIncidencia.ABIERTA));

        Incidencia leida = repositorio.buscarPorId("INC-N").orElseThrow();

        assertEquals("Baño sin señal", leida.getTitulo());
        assertEquals("Ñandú en el pasillo, ¿qué pasó?", leida.getDescripcion());
    }
}
