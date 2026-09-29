package com.campusfix.adapter.out.notification;

import com.campusfix.application.Canal;
import com.campusfix.application.Notificacion;
import com.campusfix.application.port.out.Notificador;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Notificador simulado: en lugar de enviar el aviso lo deja en una bandeja
 * de salida en memoria (correo o alerta de la app, segun el canal).
 * Reemplazable por un adaptador SMTP / push sin tocar la aplicacion.
 *
 * La bandeja esta acotada: al llegar a la capacidad descarta los avisos mas
 * antiguos, para que una carga alta no agote la memoria.
 */
public class NotificadorSimulado implements Notificador {

    public static final int CAPACIDAD_POR_DEFECTO = 10_000;

    private final Canal canal;
    private final int capacidad;
    private final Deque<Notificacion> enviadas = new ArrayDeque<>();

    public NotificadorSimulado(Canal canal) {
        this(canal, CAPACIDAD_POR_DEFECTO);
    }

    public NotificadorSimulado(Canal canal, int capacidad) {
        if (capacidad < 1) {
            throw new IllegalArgumentException("La capacidad debe ser al menos 1");
        }
        this.canal = canal;
        this.capacidad = capacidad;
    }

    @Override
    public synchronized void enviar(Notificacion notificacion) {
        if (notificacion.canal() != canal) {
            throw new IllegalArgumentException(
                    "Este notificador solo atiende el canal " + canal);
        }
        if (enviadas.size() == capacidad) {
            enviadas.removeFirst();
        }
        enviadas.addLast(notificacion);
    }

    /** Copia inmutable de los avisos, del mas antiguo al mas reciente. */
    public synchronized List<Notificacion> enviadas() {
        return List.copyOf(enviadas);
    }
}
