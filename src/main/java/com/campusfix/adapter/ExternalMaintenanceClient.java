package com.campusfix.adapter;

public class ExternalMaintenanceClient {

    private String ultimoTicketId;
    private String ultimoAsunto;
    private String ultimoDetalle;
    private String ultimoLugar;
    private String ultimoNivel;

    public void crearTicket(
            String ticketId,
            String asunto,
            String detalle,
            String lugar,
            String nivel) {

        this.ultimoTicketId = ticketId;
        this.ultimoAsunto = asunto;
        this.ultimoDetalle = detalle;
        this.ultimoLugar = lugar;
        this.ultimoNivel = nivel;
    }

    public String getUltimoTicketId() {
        return ultimoTicketId;
    }

    public String getUltimoAsunto() {
        return ultimoAsunto;
    }

    public String getUltimoDetalle() {
        return ultimoDetalle;
    }

    public String getUltimoLugar() {
        return ultimoLugar;
    }

    public String getUltimoNivel() {
        return ultimoNivel;
    }
}
