package com.campusfix.application.port.out;

import com.campusfix.application.Notificacion;

/** Puerto de salida: envio de avisos (correo, alerta en la app). */
public interface Notificador {

    void enviar(Notificacion notificacion);
}
