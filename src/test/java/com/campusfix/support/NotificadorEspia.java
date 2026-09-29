package com.campusfix.support;

import com.campusfix.application.Notificacion;
import com.campusfix.application.port.out.Notificador;

import java.util.ArrayList;
import java.util.List;

/** Doble de prueba (spy): registra lo enviado y puede simular una falla. */
public class NotificadorEspia implements Notificador {

    private final List<Notificacion> recibidas = new ArrayList<>();
    private boolean fallar;

    public NotificadorEspia fallando() {
        this.fallar = true;
        return this;
    }

    @Override
    public void enviar(Notificacion notificacion) {
        if (fallar) {
            throw new IllegalStateException("Fallo simulado del notificador");
        }
        recibidas.add(notificacion);
    }

    public List<Notificacion> recibidas() {
        return recibidas;
    }
}
