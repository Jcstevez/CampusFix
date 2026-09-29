package com.campusfix.application;

import com.campusfix.application.port.in.SeguimientoUseCase;
import com.campusfix.application.port.out.IncidenciaRepositorio;
import com.campusfix.application.port.out.Notificador;
import com.campusfix.domain.incidencia.EstadoIncidencia;
import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.TipoIncidencia;
import com.campusfix.domain.seguimiento.PoliticaSla;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * Reto 1: los reportes no quedan en el olvido. Revisa las incidencias sin
 * resolver; las que superan el plazo de su prioridad se escalan y se avisa
 * por la app y por correo al area responsable.
 */
public class SeguimientoService implements SeguimientoUseCase {

    private static final System.Logger LOG =
            System.getLogger(SeguimientoService.class.getName());

    private final IncidenciaRepositorio repositorio;
    private final PoliticaSla politica;
    private final Clock reloj;
    private final Notificador correo;
    private final Notificador alertas;
    private final String correoTi;
    private final String correoMantenimiento;

    public SeguimientoService(
            IncidenciaRepositorio repositorio,
            PoliticaSla politica,
            Clock reloj,
            Notificador correo,
            Notificador alertas,
            String correoTi,
            String correoMantenimiento) {

        this.repositorio = repositorio;
        this.politica = politica;
        this.reloj = reloj;
        this.correo = correo;
        this.alertas = alertas;
        this.correoTi = correoTi;
        this.correoMantenimiento = correoMantenimiento;
    }

    @Override
    public int revisarPendientes() {
        Instant ahora = reloj.instant();
        int escaladas = 0;

        for (Incidencia incidencia : repositorio.listarPorEstados(
                Set.of(EstadoIncidencia.ABIERTA, EstadoIncidencia.EN_PROCESO))) {

            Duration plazo = politica.plazoPara(incidencia.getPrioridad());
            if (incidencia.estaVencida(plazo, ahora)) {
                incidencia.escalar();
                repositorio.guardar(incidencia);
                avisar(incidencia, ahora);
                escaladas++;
            }
        }
        return escaladas;
    }

    private void avisar(Incidencia incidencia, Instant ahora) {
        long horas = Duration.between(incidencia.getCreadaEn(), ahora).toHours();
        String mensaje = "La incidencia " + incidencia.getId() + " ("
                + incidencia.getTitulo() + ") en " + incidencia.getUbicacion()
                + " lleva " + horas + " h sin resolverse (prioridad "
                + incidencia.getPrioridad() + ").";
        String asunto = "Recordatorio: incidencia sin atender " + incidencia.getId();
        String area = incidencia.tipo() == TipoIncidencia.TECNOLOGIA
                ? correoTi
                : correoMantenimiento;

        try {
            alertas.enviar(new Notificacion(
                    Canal.APP, "coordinacion-campus", asunto, mensaje));
            correo.enviar(new Notificacion(Canal.CORREO, area, asunto, mensaje));
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING,
                    "No se pudo enviar el recordatorio de " + incidencia.getId(), e);
        }
    }
}
