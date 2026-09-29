package com.campusfix.bootstrap;

import com.campusfix.adapter.in.http.ServidorHttp;
import com.campusfix.adapter.out.catalog.CatalogoObjetosEnMemoria;
import com.campusfix.adapter.out.mantenimiento.ExternalMaintenanceClient;
import com.campusfix.adapter.out.mantenimiento.MantenimientoAdapter;
import com.campusfix.adapter.out.notification.NotificadorSimulado;
import com.campusfix.adapter.out.persistence.IncidenciaRepositorioJdbc;
import com.campusfix.adapter.out.qr.ZxingAdapter;
import com.campusfix.application.AlertaObjetoReportadoObserver;
import com.campusfix.application.Canal;
import com.campusfix.application.CorreoTecnologiaObserver;
import com.campusfix.application.IncidenciaService;
import com.campusfix.application.MantenimientoInfraestructuraObserver;
import com.campusfix.application.Notificacion;
import com.campusfix.application.ReporteQrService;
import com.campusfix.application.SeguimientoService;
import com.campusfix.domain.clasificacion.ClasificadorIncidencia;
import com.campusfix.domain.clasificacion.ClasificadorPorReglas;
import com.campusfix.domain.seguimiento.PoliticaSla;
import com.campusfix.observer.IncidenciaSubject;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Raiz de composicion: el unico lugar donde se conocen las implementaciones
 * concretas y se conectan puertos con adaptadores.
 */
public final class Aplicacion implements AutoCloseable {

    public record Config(
            int puerto,
            String jdbcUrl,
            Clock reloj,
            String correoTi,
            String correoMantenimiento,
            PoliticaSla sla) {

        public static Config porDefecto() {
            String puerto = System.getenv().getOrDefault("PORT", "8080");
            return new Config(
                    Integer.parseInt(puerto),
                    "jdbc:h2:mem:campusfix;DB_CLOSE_DELAY=-1",
                    Clock.systemUTC(),
                    "soporte-ti@unisabana.edu.co",
                    "mantenimiento@unisabana.edu.co",
                    PoliticaSla.porDefecto());
        }

        public Config conPuerto(int nuevoPuerto) {
            return new Config(nuevoPuerto, jdbcUrl, reloj, correoTi,
                    correoMantenimiento, sla);
        }

        public Config conJdbcUrl(String nuevaUrl) {
            return new Config(puerto, nuevaUrl, reloj, correoTi,
                    correoMantenimiento, sla);
        }

        public Config conReloj(Clock nuevoReloj) {
            return new Config(puerto, jdbcUrl, nuevoReloj, correoTi,
                    correoMantenimiento, sla);
        }
    }

    private final ServidorHttp servidor;

    private Aplicacion(ServidorHttp servidor) {
        this.servidor = servidor;
    }

    public static Aplicacion iniciar(Config config) throws IOException {
        var repositorio = new IncidenciaRepositorioJdbc(config.jdbcUrl());
        var catalogo = CatalogoObjetosEnMemoria.conDatosDeEjemplo();
        var zxing = new ZxingAdapter();
        var correo = new NotificadorSimulado(Canal.CORREO);
        var alertas = new NotificadorSimulado(Canal.APP);
        var mantenimiento = new MantenimientoAdapter(new ExternalMaintenanceClient());
        ClasificadorIncidencia clasificador = new ClasificadorPorReglas();

        var eventos = new IncidenciaSubject();
        eventos.agregarObserver(new AlertaObjetoReportadoObserver(alertas));
        eventos.agregarObserver(new CorreoTecnologiaObserver(correo, config.correoTi()));
        eventos.agregarObserver(new MantenimientoInfraestructuraObserver(mantenimiento));

        var incidencias = new IncidenciaService(
                repositorio, clasificador, eventos,
                () -> "INC-" + UUID.randomUUID().toString()
                        .substring(0, 8).toUpperCase());
        var reporteQr = new ReporteQrService(
                incidencias, catalogo, zxing, zxing, clasificador);
        var seguimiento = new SeguimientoService(
                repositorio, config.sla(), config.reloj(), correo, alertas,
                config.correoTi(), config.correoMantenimiento());

        var servidor = new ServidorHttp(
                config.puerto(), incidencias, reporteQr, incidencias, seguimiento,
                () -> {
                    List<Notificacion> todas = new ArrayList<>(correo.enviadas());
                    todas.addAll(alertas.enviadas());
                    return todas;
                });
        servidor.iniciar();
        return new Aplicacion(servidor);
    }

    public int puerto() {
        return servidor.puerto();
    }

    @Override
    public void close() {
        servidor.detener();
    }
}
