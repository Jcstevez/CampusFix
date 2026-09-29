package com.campusfix.adapter.in.http;

import com.campusfix.bootstrap.Aplicacion;
import com.campusfix.support.RelojMutable;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integracion de CAJA NEGRA: se levanta la aplicacion completa
 * (HTTP + casos de uso + H2 embebida + adaptadores) y se prueba solo por su
 * interfaz publica HTTP. Cada prueba arranca su propia instancia y base de
 * datos, asi que no dependen del orden ni comparten datos.
 */
class FlujoHttpIT {

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private RelojMutable reloj;
    private Aplicacion app;
    private String base;

    @BeforeEach
    void levantarAplicacion() throws IOException {
        reloj = new RelojMutable(Instant.now());
        app = Aplicacion.iniciar(Aplicacion.Config.porDefecto()
                .conPuerto(0)
                .conJdbcUrl("jdbc:h2:mem:it-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")
                .conReloj(reloj));
        base = "http://localhost:" + app.puerto();
    }

    @AfterEach
    void detenerAplicacion() {
        app.close();
    }

    // ---------- utilidades ----------

    private HttpResponse<byte[]> enviar(HttpRequest.Builder b) {
        try {
            return http.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private HttpResponse<byte[]> get(String ruta) {
        return enviar(HttpRequest.newBuilder(URI.create(base + ruta)).GET());
    }

    private HttpResponse<byte[]> postJson(String ruta, String cuerpo) {
        return enviar(HttpRequest.newBuilder(URI.create(base + ruta))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8)));
    }

    private HttpResponse<byte[]> postVacio(String ruta) {
        return enviar(HttpRequest.newBuilder(URI.create(base + ruta))
                .POST(HttpRequest.BodyPublishers.noBody()));
    }

    private JsonNode cuerpo(HttpResponse<byte[]> r) {
        try {
            return json.readTree(r.body());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private JsonNode reportar(String titulo, String descripcion, String ubicacion) {
        String cuerpo = "{\"titulo\":\"" + titulo + "\",\"descripcion\":\"" + descripcion
                + "\",\"ubicacion\":\"" + ubicacion + "\"}";
        HttpResponse<byte[]> r = postJson("/incidencias", cuerpo);
        assertEquals(201, r.statusCode());
        return cuerpo(r);
    }

    // ---------- estado general ----------

    @Test
    void elServicioRespondeQueEstaSano() {
        HttpResponse<byte[]> r = get("/salud");

        assertEquals(200, r.statusCode());
        assertEquals("ok", cuerpo(r).get("estado").asText());
    }

    @Test
    void cadaInstanciaEmpiezaSinIncidenciasNiNotificaciones() {
        assertEquals(0, cuerpo(get("/incidencias")).size());
        assertEquals(0, cuerpo(get("/notificaciones")).size());
    }

    // ---------- Reto 2: clasificacion automatica ----------

    @Test
    void unReporteDeTecnologiaEnUnLaboratorioSeClasificaSoloComoTecnologiaAlta() {
        JsonNode i = reportar("Proyector no funciona", "No enciende", "Bloque C - Laboratorio 101");

        assertEquals("TECNOLOGIA", i.get("tipo").asText());
        assertEquals("ALTA", i.get("prioridad").asText());
        assertEquals("ABIERTA", i.get("estado").asText());
    }

    @Test
    void unaFugaDeAguaSeClasificaSoloComoInfraestructuraAlta() {
        JsonNode i = reportar("Fuga de agua en el baño", "Se esta inundando", "Bloque B - Piso 1");

        assertEquals("INFRAESTRUCTURA", i.get("tipo").asText());
        assertEquals("ALTA", i.get("prioridad").asText());
    }

    @Test
    void unaFallaComunEnUnAulaQuedaConPrioridadMedia() {
        JsonNode i = reportar("Silla rota", "Falta una pata", "Bloque A - Aula 201");

        assertEquals("INFRAESTRUCTURA", i.get("tipo").asText());
        assertEquals("MEDIA", i.get("prioridad").asText());
    }

    // ---------- Reto 1: alertas, correos y seguimiento ----------

    @Test
    void unReporteDeTecnologiaGeneraCorreoParaTiYAlertaEnLaApp() {
        reportar("Sin WiFi", "No hay internet", "Bloque A - Aula 101");

        JsonNode avisos = cuerpo(get("/notificaciones"));

        assertEquals(2, avisos.size());
        assertTrue(hay(avisos, "CORREO", "soporte-ti@unisabana.edu.co"));
        assertTrue(hay(avisos, "APP", "app-sabana"));
    }

    @Test
    void unReporteDeInfraestructuraGeneraAlertaPeroNoCorreoDeTi() {
        reportar("Puerta dañada", "No cierra", "Bloque A - Aula 201");

        JsonNode avisos = cuerpo(get("/notificaciones"));

        assertEquals(1, avisos.size());
        assertTrue(hay(avisos, "APP", "app-sabana"));
    }

    @Test
    void unReporteSinAtenderSeEscalaYSeAvisaAlPasarElPlazo() {
        // Arrange: una incidencia ALTA (plazo 2 h)
        String id = reportar("Fuga de agua", "Cae mucha agua", "Bloque B").get("id").asText();

        // Act 1: antes del plazo no pasa nada
        reloj.avanzar(Duration.ofHours(1));
        assertEquals(0, cuerpo(postVacio("/seguimiento/revisar")).get("escaladas").asInt());

        // Act 2: pasado el plazo se escala
        reloj.avanzar(Duration.ofHours(2));
        assertEquals(1, cuerpo(postVacio("/seguimiento/revisar")).get("escaladas").asInt());

        // Assert
        assertEquals("ESCALADA", cuerpo(get("/incidencias/" + id)).get("estado").asText());
        assertTrue(hayAsuntoQueContiene(cuerpo(get("/notificaciones")), "Recordatorio"));
        // y no se vuelve a avisar por la misma incidencia
        assertEquals(0, cuerpo(postVacio("/seguimiento/revisar")).get("escaladas").asInt());
    }

    @Test
    void unaIncidenciaResueltaNoSeEscalaAunqueYaPaseElPlazo() {
        String id = reportar("Fuga de agua", "Cae mucha agua", "Bloque B").get("id").asText();
        assertEquals(200, postVacio("/incidencias/" + id + "/resolver").statusCode());

        reloj.avanzar(Duration.ofDays(5));

        assertEquals(0, cuerpo(postVacio("/seguimiento/revisar")).get("escaladas").asInt());
    }

    // ---------- Reto 3: reporte por QR ----------

    @Test
    void elQrDeUnObjetoSeDescargaComoImagenPng() {
        HttpResponse<byte[]> r = get("/qr/PROY-B302.png");

        assertEquals(200, r.statusCode());
        assertEquals("image/png", r.headers().firstValue("Content-Type").orElse(""));
        assertTrue(r.body().length > 0);
    }

    @Test
    void escanearLaImagenDelQrCreaElReporteConLosDatosDelObjeto() {
        // Arrange: "imprimimos" el QR del proyector y luego lo "escaneamos"
        byte[] imagenQr = get("/qr/PROY-B302.png").body();

        // Act
        HttpResponse<byte[]> r = enviar(HttpRequest.newBuilder(
                URI.create(base + "/incidencias/qr?descripcion="
                        + URLEncoder.encode("No enciende", StandardCharsets.UTF_8)))
                .header("Content-Type", "image/png")
                .POST(HttpRequest.BodyPublishers.ofByteArray(imagenQr)));

        // Assert
        assertEquals(201, r.statusCode());
        JsonNode i = cuerpo(r);
        assertEquals("TECNOLOGIA", i.get("tipo").asText());
        assertEquals("Reporte: Proyector", i.get("titulo").asText());
        assertEquals("Bloque B - Aula 302", i.get("ubicacion").asText());
        assertEquals("No enciende", i.get("descripcion").asText());
    }

    @Test
    void elContenidoDelQrEnTextoTambienCreaElReporteYAvisaPorCorreo() {
        HttpResponse<byte[]> r = postJson("/incidencias/qr",
                "{\"qr\":\"campusfix://objeto/PC-C101-01\",\"descripcion\":\"Sale humo\"}");

        assertEquals(201, r.statusCode());
        assertEquals("ALTA", cuerpo(r).get("prioridad").asText());
        assertTrue(hay(cuerpo(get("/notificaciones")), "CORREO", "soporte-ti@unisabana.edu.co"));
    }

    @Test
    void unQrDeInfraestructuraNoAvisaAlCorreoDeTi() {
        postJson("/incidencias/qr", "{\"qr\":\"campusfix://objeto/PUERTA-A201\"}");

        assertTrue(!hay(cuerpo(get("/notificaciones")), "CORREO", "soporte-ti@unisabana.edu.co"));
    }

    // ---------- ciclo de vida completo (caja negra) ----------

    @Test
    void elCicloCompletoReportarConsultarAtenderYResolver() {
        // Reportar
        String id = reportar("Computador no enciende", "Nada", "Bloque D - Aula 4").get("id").asText();

        // Consultar
        assertEquals("ABIERTA", cuerpo(get("/incidencias/" + id)).get("estado").asText());

        // Atender
        assertEquals("EN_PROCESO", cuerpo(postVacio("/incidencias/" + id + "/atender")).get("estado").asText());

        // Resolver
        assertEquals("RESUELTA", cuerpo(postVacio("/incidencias/" + id + "/resolver")).get("estado").asText());

        // Listados filtrados por estado
        assertEquals(1, cuerpo(get("/incidencias?estado=RESUELTA")).size());
        assertEquals(0, cuerpo(get("/incidencias?estado=ABIERTA")).size());
        assertEquals(1, cuerpo(get("/incidencias")).size());
    }

    // ---------- errores ----------

    @Test
    void unReporteSinTituloSeRechazaConBadRequest() {
        assertEquals(400, postJson("/incidencias",
                "{\"descripcion\":\"x\",\"ubicacion\":\"Bloque A\"}").statusCode());
    }

    @Test
    void unJsonMalFormadoSeRechazaConBadRequest() {
        assertEquals(400, postJson("/incidencias", "{esto no es json").statusCode());
    }

    @Test
    void unaPeticionSinCuerpoSeRechazaConBadRequest() {
        assertEquals(400, postVacio("/incidencias").statusCode());
    }

    @Test
    void unQrQueNoEsDeCampusfixSeRechazaConBadRequest() {
        assertEquals(400, postJson("/incidencias/qr", "{\"qr\":\"http://otra.com\"}").statusCode());
    }

    @Test
    void unQrDeUnObjetoInexistenteDevuelveNotFound() {
        assertEquals(404, postJson("/incidencias/qr",
                "{\"qr\":\"campusfix://objeto/NO-EXISTE\"}").statusCode());
    }

    @Test
    void unaImagenQueNoEsUnQrSeRechazaConBadRequest() {
        HttpResponse<byte[]> r = enviar(HttpRequest.newBuilder(URI.create(base + "/incidencias/qr"))
                .header("Content-Type", "image/png")
                .POST(HttpRequest.BodyPublishers.ofString("no soy una imagen")));

        assertEquals(400, r.statusCode());
    }

    @Test
    void consultarUnaIncidenciaInexistenteDevuelveNotFound() {
        assertEquals(404, get("/incidencias/NO-EXISTE").statusCode());
    }

    @Test
    void resolverDosVecesLaMismaIncidenciaDevuelveConflict() {
        String id = reportar("Silla rota", "x", "Bloque A").get("id").asText();
        postVacio("/incidencias/" + id + "/resolver");

        assertEquals(409, postVacio("/incidencias/" + id + "/resolver").statusCode());
    }

    @Test
    void unEstadoDesconocidoEnElFiltroSeRechazaConBadRequest() {
        assertEquals(400, get("/incidencias?estado=INVENTADO").statusCode());
    }

    @Test
    void unaRutaDesconocidaDevuelveNotFound() {
        assertEquals(404, get("/no-existe").statusCode());
    }

    @Test
    void elQrDeUnObjetoInexistenteNoSePuedeImprimir() {
        assertEquals(404, get("/qr/NO-EXISTE.png").statusCode());
    }

    // ---------- concurrencia ----------

    @Test
    void variosReportesSimultaneosSeGuardanTodosSinPerderNinguno() throws Exception {
        int total = 30;
        try (ExecutorService pool = Executors.newFixedThreadPool(10)) {
            List<Future<Integer>> resultados = new ArrayList<>();
            for (int n = 0; n < total; n++) {
                int numero = n;
                resultados.add(pool.submit(() -> postJson("/incidencias",
                        "{\"titulo\":\"Silla rota " + numero
                                + "\",\"descripcion\":\"x\",\"ubicacion\":\"Bloque A\"}")
                        .statusCode()));
            }
            for (Future<Integer> f : resultados) {
                assertEquals(201, f.get().intValue());
            }
        }

        assertEquals(total, cuerpo(get("/incidencias")).size());
    }

    // ---------- auxiliares de asercion ----------

    private static boolean hay(JsonNode avisos, String canal, String destinatario) {
        for (JsonNode n : avisos) {
            if (n.get("canal").asText().equals(canal)
                    && n.get("destinatario").asText().equals(destinatario)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hayAsuntoQueContiene(JsonNode avisos, String texto) {
        for (JsonNode n : avisos) {
            if (n.get("asunto").asText().contains(texto)) {
                return true;
            }
        }
        return false;
    }
}
