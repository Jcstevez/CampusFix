package com.campusfix.adapter.in.http;

import com.campusfix.application.Notificacion;
import com.campusfix.application.port.in.GestionarIncidenciasUseCase;
import com.campusfix.application.port.in.ReportarIncidenciaUseCase;
import com.campusfix.application.port.in.ReportarIncidenciaUseCase.ReporteManual;
import com.campusfix.application.port.in.ReportarPorQrUseCase;
import com.campusfix.application.port.in.SeguimientoUseCase;
import com.campusfix.domain.error.RecursoNoEncontradoException;
import com.campusfix.domain.incidencia.EstadoIncidencia;
import com.campusfix.domain.incidencia.Incidencia;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Adaptador de entrada HTTP/JSON (servidor del JDK, sin framework).
 * Solo traduce HTTP a llamadas de los casos de uso y errores a codigos HTTP;
 * no contiene reglas de negocio.
 *
 * <pre>
 * GET  /                           (pantalla web: resources/web/index.html)
 * GET  /salud
 * POST /incidencias                 {titulo, descripcion, ubicacion}
 * POST /incidencias/qr              {qr, descripcion}  |  image/png + ?descripcion=
 * GET  /incidencias[?estado=ABIERTA]
 * GET  /incidencias/{id}
 * POST /incidencias/{id}/atender
 * POST /incidencias/{id}/resolver
 * POST /seguimiento/revisar
 * GET  /notificaciones              (bandeja simulada de correos y alertas)
 * GET  /qr/{objetoId}.png           (simula imprimir el QR de un objeto)
 * </pre>
 */
public class ServidorHttp {

    private static final int MAX_CUERPO_BYTES = 5 * 1024 * 1024;

    static {
        // El HttpServer del JDK trae TCP_NODELAY apagado: cabeceras y cuerpo se
        // envian en dos segmentos y Nagle + ACK retardado agregan ~40 ms a cada
        // respuesta GET. Se activa antes de que se cree el primer servidor.
        System.setProperty("sun.net.httpserver.nodelay", "true");
    }

    public record ReporteRequest(String titulo, String descripcion, String ubicacion) {
    }

    public record QrRequest(String qr, String descripcion) {
    }

    public record IncidenciaDto(
            String id,
            String tipo,
            String titulo,
            String descripcion,
            String ubicacion,
            String prioridad,
            String estado,
            String creadaEn) {

        static IncidenciaDto de(Incidencia i) {
            return new IncidenciaDto(
                    i.getId(), i.tipo().name(), i.getTitulo(), i.getDescripcion(),
                    i.getUbicacion(), i.getPrioridad().name(), i.getEstado().name(),
                    i.getCreadaEn().toString());
        }
    }

    public record NotificacionDto(
            String canal, String destinatario, String asunto, String mensaje) {

        static NotificacionDto de(Notificacion n) {
            return new NotificacionDto(
                    n.canal().name(), n.destinatario(), n.asunto(), n.mensaje());
        }
    }

    private record Respuesta(int estado, String tipo, byte[] cuerpo) {
    }

    private final ReportarIncidenciaUseCase reportar;
    private final ReportarPorQrUseCase reportarQr;
    private final GestionarIncidenciasUseCase gestionar;
    private final SeguimientoUseCase seguimiento;
    private final Supplier<List<Notificacion>> bandeja;
    private final ObjectMapper json = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final HttpServer servidor;
    private final ExecutorService ejecutor = Executors.newVirtualThreadPerTaskExecutor();

    public ServidorHttp(
            int puerto,
            ReportarIncidenciaUseCase reportar,
            ReportarPorQrUseCase reportarQr,
            GestionarIncidenciasUseCase gestionar,
            SeguimientoUseCase seguimiento,
            Supplier<List<Notificacion>> bandeja) throws IOException {

        this.reportar = reportar;
        this.reportarQr = reportarQr;
        this.gestionar = gestionar;
        this.seguimiento = seguimiento;
        this.bandeja = bandeja;
        this.servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        this.servidor.createContext("/", this::manejar);
        this.servidor.setExecutor(ejecutor);
    }

    public void iniciar() {
        servidor.start();
    }

    public int puerto() {
        return servidor.getAddress().getPort();
    }

    public void detener() {
        servidor.stop(0);
        ejecutor.shutdownNow();
    }

    private void manejar(HttpExchange ex) throws IOException {
        try {
            enviar(ex, enrutar(ex));
        } catch (RecursoNoEncontradoException e) {
            enviar(ex, error(404, e.getMessage()));
        } catch (IllegalArgumentException e) {
            enviar(ex, error(400, e.getMessage()));
        } catch (IllegalStateException e) {
            enviar(ex, error(409, e.getMessage()));
        } catch (RuntimeException e) {
            System.getLogger(ServidorHttp.class.getName())
                    .log(System.Logger.Level.ERROR, "Error no controlado", e);
            enviar(ex, error(500, "Error interno"));
        } finally {
            ex.close();
        }
    }

    private Respuesta enrutar(HttpExchange ex) throws IOException {
        String metodo = ex.getRequestMethod();
        String[] seg = segmentos(ex.getRequestURI().getPath());
        Map<String, String> consulta = parsearConsulta(ex.getRequestURI().getRawQuery());

        if (seg.length == 0 && metodo.equals("GET")) {
            return new Respuesta(200, "text/html; charset=utf-8", pantalla());
        }
        if (seg.length == 1 && seg[0].equals("salud") && metodo.equals("GET")) {
            return ok(200, Map.of("estado", "ok"));
        }
        if (seg.length >= 1 && seg[0].equals("incidencias")) {
            return rutasIncidencias(ex, metodo, seg, consulta);
        }
        if (seg.length == 2 && seg[0].equals("seguimiento")
                && seg[1].equals("revisar") && metodo.equals("POST")) {
            return ok(200, Map.of("escaladas", seguimiento.revisarPendientes()));
        }
        if (seg.length == 1 && seg[0].equals("notificaciones") && metodo.equals("GET")) {
            return ok(200, bandeja.get().stream().map(NotificacionDto::de).toList());
        }
        if (seg.length == 2 && seg[0].equals("qr") && seg[1].endsWith(".png")
                && metodo.equals("GET")) {
            String objetoId = seg[1].substring(0, seg[1].length() - ".png".length());
            return new Respuesta(200, "image/png", reportarQr.generarQrDeObjeto(objetoId));
        }
        return error(404, "Ruta no encontrada");
    }

    private static byte[] pantalla() throws IOException {
        try (var in = ServidorHttp.class.getResourceAsStream("/web/index.html")) {
            if (in == null) {
                throw new IllegalStateException("Falta el recurso web/index.html");
            }
            return in.readAllBytes();
        }
    }

    private Respuesta rutasIncidencias(
            HttpExchange ex, String metodo, String[] seg, Map<String, String> consulta)
            throws IOException {

        if (seg.length == 1) {
            if (metodo.equals("POST")) {
                ReporteRequest r = leer(ex, ReporteRequest.class);
                return ok(201, IncidenciaDto.de(reportar.reportar(
                        new ReporteManual(r.titulo(), r.descripcion(), r.ubicacion()))));
            }
            if (metodo.equals("GET")) {
                Optional<EstadoIncidencia> estado = Optional.ofNullable(consulta.get("estado"))
                        .map(ServidorHttp::parsearEstado);
                return ok(200, gestionar.listar(estado).stream()
                        .map(IncidenciaDto::de).toList());
            }
        }
        if (seg.length == 2 && seg[1].equals("qr") && metodo.equals("POST")) {
            return reportarPorQr(ex, consulta);
        }
        if (seg.length == 2 && metodo.equals("GET")) {
            return ok(200, IncidenciaDto.de(gestionar.consultar(seg[1])));
        }
        if (seg.length == 3 && metodo.equals("POST")) {
            if (seg[2].equals("atender")) {
                return ok(200, IncidenciaDto.de(gestionar.atender(seg[1])));
            }
            if (seg[2].equals("resolver")) {
                return ok(200, IncidenciaDto.de(gestionar.resolver(seg[1])));
            }
        }
        return error(404, "Ruta no encontrada");
    }

    private Respuesta reportarPorQr(HttpExchange ex, Map<String, String> consulta)
            throws IOException {

        String tipo = ex.getRequestHeaders().getFirst("Content-Type");
        if (tipo != null && tipo.toLowerCase().startsWith("image/")) {
            byte[] imagen = leerCuerpo(ex);
            return ok(201, IncidenciaDto.de(reportarQr.reportarPorImagen(
                    imagen, consulta.get("descripcion"))));
        }
        QrRequest r = leer(ex, QrRequest.class);
        return ok(201, IncidenciaDto.de(
                reportarQr.reportarPorContenido(r.qr(), r.descripcion())));
    }

    private static EstadoIncidencia parsearEstado(String valor) {
        try {
            return EstadoIncidencia.valueOf(valor.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado desconocido: " + valor);
        }
    }

    private static byte[] leerCuerpo(HttpExchange ex) throws IOException {
        byte[] cuerpo = ex.getRequestBody().readNBytes(MAX_CUERPO_BYTES + 1);
        if (cuerpo.length > MAX_CUERPO_BYTES) {
            throw new IllegalArgumentException("El cuerpo supera 5 MB");
        }
        return cuerpo;
    }

    private <T> T leer(HttpExchange ex, Class<T> tipo) throws IOException {
        byte[] cuerpo = leerCuerpo(ex);
        if (cuerpo.length == 0) {
            throw new IllegalArgumentException("Falta el cuerpo JSON");
        }
        try {
            return json.readValue(cuerpo, tipo);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON invalido");
        }
    }

    private Respuesta ok(int estado, Object cuerpo) {
        try {
            return new Respuesta(estado, "application/json; charset=utf-8",
                    json.writeValueAsBytes(cuerpo));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la respuesta", e);
        }
    }

    private Respuesta error(int estado, String mensaje) {
        return ok(estado, Map.of("error", mensaje == null ? "" : mensaje));
    }

    private static void enviar(HttpExchange ex, Respuesta r) throws IOException {
        ex.getResponseHeaders().set("Content-Type", r.tipo());
        ex.sendResponseHeaders(r.estado(), r.cuerpo().length);
        ex.getResponseBody().write(r.cuerpo());
    }

    private static String[] segmentos(String ruta) {
        return java.util.Arrays.stream(ruta.split("/"))
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    private static Map<String, String> parsearConsulta(String consulta) {
        Map<String, String> params = new HashMap<>();
        if (consulta == null || consulta.isEmpty()) {
            return params;
        }
        for (String par : consulta.split("&")) {
            int i = par.indexOf('=');
            String clave = i < 0 ? par : par.substring(0, i);
            String valor = i < 0 ? "" : par.substring(i + 1);
            params.put(
                    URLDecoder.decode(clave, StandardCharsets.UTF_8),
                    URLDecoder.decode(valor, StandardCharsets.UTF_8));
        }
        return params;
    }
}
