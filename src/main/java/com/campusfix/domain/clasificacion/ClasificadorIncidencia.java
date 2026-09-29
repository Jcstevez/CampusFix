package com.campusfix.domain.clasificacion;

/**
 * Decide automaticamente el tipo (tecnologia / infraestructura) y la
 * prioridad de un reporte. Es una abstraccion: hoy hay reglas por palabras
 * clave, manana podria ser un modelo de ML sin tocar el resto del sistema.
 */
public interface ClasificadorIncidencia {

    Clasificacion clasificar(String titulo, String descripcion, String ubicacion);
}
