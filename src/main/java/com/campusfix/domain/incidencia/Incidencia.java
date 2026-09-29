package com.campusfix.domain.incidencia;

import java.time.Duration;
import java.time.Instant;

public abstract class Incidencia {

    public static final int MAX_TITULO = 200;
    public static final int MAX_DESCRIPCION = 2000;
    public static final int MAX_UBICACION = 200;

    private final String id;
    private final String titulo;
    private final String descripcion;
    private final String ubicacion;
    private final Prioridad prioridad;
    private final Instant creadaEn;
    private EstadoIncidencia estado;

    protected Incidencia(
            String id,
            String titulo,
            String descripcion,
            String ubicacion,
            Prioridad prioridad) {

        this(id, titulo, descripcion, ubicacion, prioridad,
                Instant.now(), EstadoIncidencia.ABIERTA);
    }

    /** Constructor completo, tambien usado para reconstruir desde persistencia. */
    protected Incidencia(
            String id,
            String titulo,
            String descripcion,
            String ubicacion,
            Prioridad prioridad,
            Instant creadaEn,
            EstadoIncidencia estado) {

        this.id = requerirTexto(id, "id", 64);
        this.titulo = requerirTexto(titulo, "titulo", MAX_TITULO);
        this.descripcion = descripcion == null ? "" : descripcion.strip();
        if (this.descripcion.length() > MAX_DESCRIPCION) {
            throw new IllegalArgumentException(
                    "descripcion supera " + MAX_DESCRIPCION + " caracteres");
        }
        this.ubicacion = requerirTexto(ubicacion, "ubicacion", MAX_UBICACION);
        if (prioridad == null) {
            throw new IllegalArgumentException("prioridad es obligatoria");
        }
        if (creadaEn == null || estado == null) {
            throw new IllegalArgumentException(
                    "fecha de creacion y estado son obligatorios");
        }
        this.prioridad = prioridad;
        this.creadaEn = creadaEn;
        this.estado = estado;
    }

    private static String requerirTexto(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " es obligatorio");
        }
        String limpio = valor.strip();
        if (limpio.length() > max) {
            throw new IllegalArgumentException(
                    campo + " supera " + max + " caracteres");
        }
        return limpio;
    }

    public abstract TipoIncidencia tipo();

    public String getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public EstadoIncidencia getEstado() {
        return estado;
    }

    public void iniciarAtencion() {
        if (estado != EstadoIncidencia.ABIERTA
                && estado != EstadoIncidencia.ESCALADA) {
            throw new IllegalStateException(
                    "No se puede atender una incidencia en estado " + estado);
        }
        estado = EstadoIncidencia.EN_PROCESO;
    }

    public void escalar() {
        if (estado != EstadoIncidencia.ABIERTA
                && estado != EstadoIncidencia.EN_PROCESO) {
            throw new IllegalStateException(
                    "No se puede escalar una incidencia en estado " + estado);
        }
        estado = EstadoIncidencia.ESCALADA;
    }

    public void resolver() {
        if (estado == EstadoIncidencia.RESUELTA) {
            throw new IllegalStateException("La incidencia ya esta resuelta");
        }
        estado = EstadoIncidencia.RESUELTA;
    }

    /**
     * Una incidencia esta vencida si sigue sin resolver ni escalar y ya
     * transcurrio el plazo (limite inclusivo: tiempo transcurrido >= plazo).
     */
    public boolean estaVencida(Duration plazo, Instant ahora) {
        if (estado != EstadoIncidencia.ABIERTA
                && estado != EstadoIncidencia.EN_PROCESO) {
            return false;
        }
        return Duration.between(creadaEn, ahora).compareTo(plazo) >= 0;
    }
}
