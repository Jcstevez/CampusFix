package com.campusfix.application;

import com.campusfix.application.port.in.GestionarIncidenciasUseCase;
import com.campusfix.application.port.in.ReportarIncidenciaUseCase;
import com.campusfix.application.port.out.IncidenciaRepositorio;
import com.campusfix.domain.clasificacion.Clasificacion;
import com.campusfix.domain.clasificacion.ClasificadorIncidencia;
import com.campusfix.domain.error.IncidenciaNoEncontradaException;
import com.campusfix.domain.incidencia.EstadoIncidencia;
import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.Prioridad;
import com.campusfix.domain.incidencia.TipoIncidencia;
import com.campusfix.factory.IncidenciaCreator;
import com.campusfix.factory.InfraestructuraCreator;
import com.campusfix.factory.TecnologiaCreator;
import com.campusfix.observer.IncidenciaSubject;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Casos de uso de incidencias. Orquesta: clasificar -> crear (Factory Method)
 * -> guardar -> avisar (Observer). No conoce HTTP, SQL ni correo.
 */
public class IncidenciaService
        implements ReportarIncidenciaUseCase, GestionarIncidenciasUseCase {

    private final IncidenciaRepositorio repositorio;
    private final ClasificadorIncidencia clasificador;
    private final IncidenciaSubject eventos;
    private final Supplier<String> generadorId;
    private final Map<TipoIncidencia, IncidenciaCreator> creadores =
            new EnumMap<>(TipoIncidencia.class);

    public IncidenciaService(
            IncidenciaRepositorio repositorio,
            ClasificadorIncidencia clasificador,
            IncidenciaSubject eventos,
            Supplier<String> generadorId) {

        this.repositorio = repositorio;
        this.clasificador = clasificador;
        this.eventos = eventos;
        this.generadorId = generadorId;
        creadores.put(TipoIncidencia.TECNOLOGIA, new TecnologiaCreator());
        creadores.put(TipoIncidencia.INFRAESTRUCTURA, new InfraestructuraCreator());
    }

    /** Crea con el creador indicado, guarda y notifica. */
    public Incidencia registrarIncidencia(
            IncidenciaCreator creator,
            String id,
            String titulo,
            String descripcion,
            String ubicacion,
            Prioridad prioridad) {

        Incidencia incidencia = creator.crearIncidencia(
                id, titulo, descripcion, ubicacion, prioridad);
        repositorio.guardar(incidencia);
        eventos.notificarObservers(incidencia);
        return incidencia;
    }

    /** Registra una incidencia cuyo tipo y prioridad ya se conocen. */
    public Incidencia registrar(
            TipoIncidencia tipo,
            Prioridad prioridad,
            String titulo,
            String descripcion,
            String ubicacion) {

        return registrarIncidencia(
                creadores.get(tipo),
                generadorId.get(),
                titulo,
                descripcion,
                ubicacion,
                prioridad);
    }

    @Override
    public Incidencia reportar(ReporteManual reporte) {
        Clasificacion c = clasificador.clasificar(
                reporte.titulo(), reporte.descripcion(), reporte.ubicacion());
        return registrar(
                c.tipo(),
                c.prioridad(),
                reporte.titulo(),
                reporte.descripcion(),
                reporte.ubicacion());
    }

    @Override
    public Incidencia consultar(String id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new IncidenciaNoEncontradaException(id));
    }

    @Override
    public List<Incidencia> listar(Optional<EstadoIncidencia> estado) {
        return repositorio.listarPorEstados(
                estado.map(Set::of).orElseGet(() -> Set.of(EstadoIncidencia.values())));
    }

    @Override
    public Incidencia atender(String id) {
        Incidencia incidencia = consultar(id);
        incidencia.iniciarAtencion();
        repositorio.guardar(incidencia);
        return incidencia;
    }

    @Override
    public Incidencia resolver(String id) {
        Incidencia incidencia = consultar(id);
        incidencia.resolver();
        repositorio.guardar(incidencia);
        return incidencia;
    }
}
