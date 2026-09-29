package com.campusfix.adapter.out.catalog;

import com.campusfix.application.port.out.CatalogoObjetos;
import com.campusfix.domain.catalogo.ObjetoCampus;
import com.campusfix.domain.incidencia.TipoIncidencia;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Catalogo de objetos con QR, en memoria (datos de ejemplo del campus). */
public class CatalogoObjetosEnMemoria implements CatalogoObjetos {

    private final Map<String, ObjetoCampus> objetos;

    public CatalogoObjetosEnMemoria(List<ObjetoCampus> objetos) {
        this.objetos = objetos.stream()
                .collect(Collectors.toUnmodifiableMap(ObjetoCampus::id, Function.identity()));
    }

    public static CatalogoObjetosEnMemoria conDatosDeEjemplo() {
        return new CatalogoObjetosEnMemoria(List.of(
                new ObjetoCampus("PROY-B302", "Proyector",
                        "Bloque B - Aula 302", TipoIncidencia.TECNOLOGIA),
                new ObjetoCampus("PC-C101-01", "Computador 01",
                        "Bloque C - Laboratorio 101", TipoIncidencia.TECNOLOGIA),
                new ObjetoCampus("WIFI-A2", "Punto de acceso WiFi",
                        "Bloque A - Piso 2", TipoIncidencia.TECNOLOGIA),
                new ObjetoCampus("PUERTA-A201", "Puerta del aula",
                        "Bloque A - Aula 201", TipoIncidencia.INFRAESTRUCTURA),
                new ObjetoCampus("LUZ-A201", "Iluminacion del aula",
                        "Bloque A - Aula 201", TipoIncidencia.INFRAESTRUCTURA),
                new ObjetoCampus("BANO-B1", "Bano",
                        "Bloque B - Piso 1", TipoIncidencia.INFRAESTRUCTURA)));
    }

    @Override
    public Optional<ObjetoCampus> buscarPorId(String objetoId) {
        return Optional.ofNullable(objetos.get(objetoId));
    }
}
