package com.campusfix.application;

public record Notificacion(
        Canal canal,
        String destinatario,
        String asunto,
        String mensaje) {

    public Notificacion {
        if (canal == null
                || destinatario == null || destinatario.isBlank()
                || asunto == null || asunto.isBlank()
                || mensaje == null) {
            throw new IllegalArgumentException(
                    "canal, destinatario y asunto son obligatorios");
        }
    }
}
