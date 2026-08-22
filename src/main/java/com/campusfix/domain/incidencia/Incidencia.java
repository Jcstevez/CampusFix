package com.campusfix.domain.incidencia;

public abstract class Incidencia {

    private final String id;
    private final String titulo;
    private final String descripcion;
    private final String ubicacion;
    private final Prioridad prioridad;

    protected Incidencia(
            String id,
            String titulo,
            String descripcion,
            String ubicacion,
            Prioridad prioridad) {

        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.ubicacion = ubicacion;
        this.prioridad = prioridad;
    }

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
}