package com.campusfix.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hace cumplir las reglas de la arquitectura hexagonal leyendo los imports
 * del codigo fuente (sin librerias externas):
 *
 * <ul>
 *   <li>El nucleo (domain, factory, strategy, observer) no depende de la
 *       aplicacion, de los adaptadores ni de tecnologia (SQL, HTTP, JSON, QR).</li>
 *   <li>La aplicacion solo conoce puertos: no importa adaptadores ni tecnologia.</li>
 *   <li>Los adaptadores de entrada y de salida no se conocen entre si, y
 *       ninguno depende del bootstrap.</li>
 * </ul>
 */
class ArquitecturaTest {

    private static final Path RAIZ = Path.of("src/main/java/com/campusfix");
    private static final Pattern IMPORT =
            Pattern.compile("^import\\s+(?:static\\s+)?([\\w.]+)\\s*;", Pattern.MULTILINE);

    private static final List<String> TECNOLOGIA = List.of(
            "java.sql", "javax.sql", "com.sun.net", "com.fasterxml",
            "com.google", "javax.imageio", "java.awt", "java.net.http");

    private static final List<String> NUCLEO = List.of(
            "domain", "factory", "strategy", "observer");

    private static List<String> importsDe(String paquete) throws IOException {
        Path dir = RAIZ.resolve(paquete.replace('.', '/'));
        List<String> resultado = new ArrayList<>();
        try (Stream<Path> archivos = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) archivos.filter(f -> f.toString().endsWith(".java"))::iterator) {
                Matcher m = IMPORT.matcher(Files.readString(p));
                while (m.find()) {
                    resultado.add(p.getFileName() + " -> " + m.group(1));
                }
            }
        }
        return resultado;
    }

    private static List<String> violaciones(String paquete, List<String> prohibidos)
            throws IOException {
        return importsDe(paquete).stream()
                .filter(linea -> prohibidos.stream().anyMatch(
                        p -> linea.substring(linea.indexOf("-> ") + 3).startsWith(p)))
                .toList();
    }

    @Test
    void laPruebaRealmenteEncuentraCodigoParaRevisar() throws IOException {
        // Guarda contra falsos verdes: si cambian las rutas, esta prueba falla.
        for (String paquete : List.of("domain", "application", "adapter", "factory",
                "strategy", "observer")) {
            assertTrue(!importsDe(paquete).isEmpty(), "Sin imports en " + paquete);
        }
    }

    @Test
    void elNucleoNoDependeDeAplicacionAdaptadoresNiBootstrap() throws IOException {
        List<String> prohibidos = List.of(
                "com.campusfix.application",
                "com.campusfix.adapter",
                "com.campusfix.bootstrap");
        for (String paquete : NUCLEO) {
            assertEquals(List.of(), violaciones(paquete, prohibidos),
                    "El paquete " + paquete + " rompe la regla de dependencia");
        }
    }

    @Test
    void elNucleoNoUsaTecnologiaDeInfraestructura() throws IOException {
        for (String paquete : NUCLEO) {
            assertEquals(List.of(), violaciones(paquete, TECNOLOGIA),
                    "El paquete " + paquete + " usa tecnologia de infraestructura");
        }
    }

    @Test
    void laAplicacionNoConoceAdaptadoresNiBootstrap() throws IOException {
        assertEquals(List.of(), violaciones("application",
                List.of("com.campusfix.adapter", "com.campusfix.bootstrap")));
    }

    @Test
    void laAplicacionNoUsaTecnologiaDeInfraestructura() throws IOException {
        assertEquals(List.of(), violaciones("application", TECNOLOGIA));
    }

    @Test
    void losAdaptadoresDeEntradaNoConocenLosDeSalida() throws IOException {
        assertEquals(List.of(),
                violaciones("adapter.in", List.of("com.campusfix.adapter.out")));
    }

    @Test
    void losAdaptadoresDeSalidaNoConocenLosDeEntrada() throws IOException {
        assertEquals(List.of(),
                violaciones("adapter.out", List.of("com.campusfix.adapter.in")));
    }

    @Test
    void ningunAdaptadorDependeDelBootstrap() throws IOException {
        assertEquals(List.of(),
                violaciones("adapter", List.of("com.campusfix.bootstrap")));
    }
}
