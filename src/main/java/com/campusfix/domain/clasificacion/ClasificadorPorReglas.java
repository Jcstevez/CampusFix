package com.campusfix.domain.clasificacion;

import com.campusfix.domain.incidencia.Prioridad;
import com.campusfix.domain.incidencia.TipoIncidencia;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/**
 * Clasificacion por reglas de palabras clave.
 *
 * Tipo: gana la categoria con mas coincidencias (las del titulo pesan doble).
 * Si no hay coincidencias o hay empate, se asigna INFRAESTRUCTURA.
 *
 * Prioridad: ALTA si hay una palabra critica (riesgo para personas o
 * servicio caido); BAJA si solo hay palabras esteticas/menores; MEDIA en el
 * resto. Si queda MEDIA y la ubicacion es sensible (laboratorio, biblioteca,
 * auditorio...) sube a ALTA.
 *
 * Las palabras se comparan sin tildes ni mayusculas. Las de 3 letras o menos
 * deben coincidir exactas; las mas largas coinciden por prefijo
 * ("computador" acepta "computadores").
 */
public class ClasificadorPorReglas implements ClasificadorIncidencia {

    private static final Set<String> TECNOLOGIA = Set.of(
            "proyector", "computador", "portatil", "pc", "pcs", "impresora",
            "escaner", "wifi", "internet", "router", "red", "redes",
            "servidor", "software", "aplicacion", "plataforma", "sistema",
            "pantalla", "monitor", "teclado", "mouse", "hdmi", "vga", "usb",
            "microfono", "parlante", "televisor", "tv", "correo",
            "contrasena", "moodle", "senal", "conexion", "aula virtual");

    private static final Set<String> INFRAESTRUCTURA = Set.of(
            "puerta", "ventana", "techo", "gotera", "pared", "piso",
            "escalera", "ascensor", "cerradura", "llave", "bano", "sanitario",
            "lavamanos", "grifo", "tuberia", "fuga", "filtracion", "humedad",
            "goteo", "silla", "mesa", "escritorio", "tablero", "pupitre",
            "aire acondicionado", "ventilador", "bombillo", "lampara",
            "luminaria", "luz", "interruptor", "tomacorriente", "enchufe",
            "ducha", "pintura", "rejilla", "vidrio", "rampa", "cancha",
            "jardin", "cesped", "arbol", "pasillo");

    private static final Set<String> CRITICAS = Set.of(
            "incendio", "humo", "chispa", "cortocircuito", "gas",
            "inundacion", "electrocut", "atrapad", "riesgo", "peligr",
            "emergencia", "urgente", "fuga", "corto circuito",
            "descarga electrica", "sin luz", "sin agua", "caida total");

    private static final Set<String> MENORES = Set.of(
            "rayon", "raya", "estetic", "manchad", "desgastad",
            "sugerencia", "cosmetic", "descolorid");

    private static final Set<String> UBICACIONES_SENSIBLES = Set.of(
            "laboratorio", "biblioteca", "auditorio", "servidor",
            "urgencias", "enfermeria");

    @Override
    public Clasificacion clasificar(
            String titulo, String descripcion, String ubicacion) {

        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("titulo es obligatorio");
        }

        String tit = normalizar(titulo);
        String desc = normalizar(descripcion);
        String ubi = normalizar(ubicacion);
        String todo = (tit + " " + desc).strip();

        return new Clasificacion(
                decidirTipo(tit, desc),
                decidirPrioridad(todo, ubi));
    }

    private TipoIncidencia decidirTipo(String titulo, String descripcion) {
        int tecnologia = 2 * coincidencias(titulo, TECNOLOGIA)
                + coincidencias(descripcion, TECNOLOGIA);
        int infraestructura = 2 * coincidencias(titulo, INFRAESTRUCTURA)
                + coincidencias(descripcion, INFRAESTRUCTURA);

        return tecnologia > infraestructura
                ? TipoIncidencia.TECNOLOGIA
                : TipoIncidencia.INFRAESTRUCTURA;
    }

    private Prioridad decidirPrioridad(String texto, String ubicacion) {
        if (coincidencias(texto, CRITICAS) > 0) {
            return Prioridad.ALTA;
        }
        if (coincidencias(texto, MENORES) > 0) {
            return Prioridad.BAJA;
        }
        if (coincidencias(ubicacion, UBICACIONES_SENSIBLES) > 0) {
            return Prioridad.ALTA;
        }
        return Prioridad.MEDIA;
    }

    private static int coincidencias(String textoNormalizado, Set<String> palabras) {
        if (textoNormalizado.isEmpty()) {
            return 0;
        }
        String[] tokens = textoNormalizado.split(" ");
        String conBordes = " " + textoNormalizado + " ";
        int total = 0;
        for (String palabra : palabras) {
            if (palabra.contains(" ")) {
                if (conBordes.contains(" " + palabra + " ")) {
                    total++;
                }
            } else if (Arrays.stream(tokens).anyMatch(t -> coincide(t, palabra))) {
                total++;
            }
        }
        return total;
    }

    private static boolean coincide(String token, String palabra) {
        return palabra.length() <= 3
                ? token.equals(palabra)
                : token.startsWith(palabra);
    }

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .strip();
    }
}
