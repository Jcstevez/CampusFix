package com.campusfix.adapter.out.mantenimiento;

public class ExternalMaintenanceClient {

    private String ultimoTicketId;
    private String ultimoAsunto;
    private String ultimoDetalle;
    private String ultimoLugar;
    private String ultimoNivel;

    public synchronized void crearTicket(
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

    public synchronized String getUltimoTicketId() {
        return ultimoTicketId;
    }

    public synchronized String getUltimoAsunto() {
        return ultimoAsunto;
    }

    public synchronized String getUltimoDetalle() {
        return ultimoDetalle;
    }

    public synchronized String getUltimoLugar() {
        return ultimoLugar;
    }

    public synchronized String getUltimoNivel() {
        return ultimoNivel;
    }
}
