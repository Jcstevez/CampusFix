package com.campusfix.application;

import com.campusfix.application.port.in.ReportarPorQrUseCase;
import com.campusfix.application.port.out.CatalogoObjetos;
import com.campusfix.application.port.out.DecodificadorQr;
import com.campusfix.application.port.out.GeneradorQr;
import com.campusfix.domain.catalogo.CodigoQr;
import com.campusfix.domain.catalogo.ObjetoCampus;
import com.campusfix.domain.clasificacion.ClasificadorIncidencia;
import com.campusfix.domain.error.ObjetoNoEncontradoException;
import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.Prioridad;

/**
 * Reporte por QR: el objeto escaneado ya dice que es, donde esta y de que
 * tipo es; el usuario solo agrega (opcionalmente) que le pasa.
 */
public class ReporteQrService implements ReportarPorQrUseCase {

    private final IncidenciaService incidencias;
    private final CatalogoObjetos catalogo;
    private final DecodificadorQr decodificador;
    private final GeneradorQr generador;
    private final ClasificadorIncidencia clasificador;

    public ReporteQrService(
            IncidenciaService incidencias,
            CatalogoObjetos catalogo,
            DecodificadorQr decodificador,
            GeneradorQr generador,
            ClasificadorIncidencia clasificador) {

        this.incidencias = incidencias;
        this.catalogo = catalogo;
        this.decodificador = decodificador;
        this.generador = generador;
        this.clasificador = clasificador;
    }

    @Override
    public Incidencia reportarPorContenido(String contenidoQr, String descripcion) {
        CodigoQr qr = CodigoQr.desde(contenidoQr);
        ObjetoCampus objeto = buscar(qr.objetoId());

        String titulo = "Reporte: " + objeto.nombre();
        Prioridad prioridad = clasificador
                .clasificar(titulo, descripcion, objeto.ubicacion())
                .prioridad();

        return incidencias.registrar(
                objeto.tipo(), prioridad, titulo, descripcion, objeto.ubicacion());
    }

    @Override
    public Incidencia reportarPorImagen(byte[] imagenQr, String descripcion) {
        return reportarPorContenido(decodificador.decodificar(imagenQr), descripcion);
    }

    @Override
    public byte[] generarQrDeObjeto(String objetoId) {
        buscar(objetoId);
        return generador.generarPng(new CodigoQr(objetoId).contenido());
    }

    private ObjetoCampus buscar(String objetoId) {
        return catalogo.buscarPorId(objetoId)
                .orElseThrow(() -> new ObjetoNoEncontradoException(objetoId));
    }
}
