package com.campusfix.adapter.out.notification;

import com.campusfix.application.Canal;
import com.campusfix.application.Notificacion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificadorSimuladoTest {

    @Test
    void guardaLosAvisosDeSuCanalEnLaBandeja() {
        NotificadorSimulado correo = new NotificadorSimulado(Canal.CORREO);
        Notificacion n = new Notificacion(Canal.CORREO, "ti@x.test", "Asunto", "Mensaje");

        correo.enviar(n);

        assertEquals(1, correo.enviadas().size());
        assertEquals(n, correo.enviadas().get(0));
    }

    @Test
    void rechazaAvisosDeOtroCanal() {
        NotificadorSimulado correo = new NotificadorSimulado(Canal.CORREO);
        Notificacion deApp = new Notificacion(Canal.APP, "app", "Asunto", "Mensaje");

        assertThrows(IllegalArgumentException.class, () -> correo.enviar(deApp));
    }

    @Test
    void laBandejaDevueltaNoSePuedeModificarDesdeAfuera() {
        NotificadorSimulado correo = new NotificadorSimulado(Canal.CORREO);
        assertThrows(UnsupportedOperationException.class, () -> correo.enviadas().add(
                new Notificacion(Canal.CORREO, "a", "b", "c")));
    }

    @Test
    void unaNotificacionSinDestinatarioEsInvalida() {
        assertThrows(IllegalArgumentException.class, () ->
                new Notificacion(Canal.CORREO, " ", "Asunto", "Mensaje"));
    }

    @Test
    void alLlegarALaCapacidadDescartaLosAvisosMasAntiguos() {
        // Arrange
        NotificadorSimulado correo = new NotificadorSimulado(Canal.CORREO, 2);

        // Act
        for (String asunto : new String[]{"uno", "dos", "tres"}) {
            correo.enviar(new Notificacion(Canal.CORREO, "a@x.test", asunto, "m"));
        }

        // Assert
        assertEquals(2, correo.enviadas().size());
        assertEquals("dos", correo.enviadas().get(0).asunto());
        assertEquals("tres", correo.enviadas().get(1).asunto());
    }

    @Test
    void unaCapacidadMenorAUnoEsInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> new NotificadorSimulado(Canal.CORREO, 0));
    }
}
