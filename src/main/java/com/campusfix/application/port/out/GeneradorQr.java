package com.campusfix.application.port.out;

/** Puerto de salida: dibuja un codigo QR (PNG) con un texto. */
public interface GeneradorQr {

    byte[] generarPng(String contenido);
}
