package com.campusfix.adapter.out.persistence;

import com.campusfix.application.port.out.IncidenciaRepositorio;
import com.campusfix.domain.error.RepositorioException;
import com.campusfix.domain.incidencia.EstadoIncidencia;
import com.campusfix.domain.incidencia.Incidencia;
import com.campusfix.domain.incidencia.IncidenciaInfraestructura;
import com.campusfix.domain.incidencia.IncidenciaTecnologica;
import com.campusfix.domain.incidencia.Prioridad;
import com.campusfix.domain.incidencia.TipoIncidencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida: guarda las incidencias por JDBC (H2 en el proyecto).
 * Solo usa la API estandar java.sql; el driver se elige con la URL.
 */
public class IncidenciaRepositorioJdbc implements IncidenciaRepositorio {

    private static final String COLUMNAS =
            "id, tipo, titulo, descripcion, ubicacion, prioridad, estado, creada_en";

    private final String url;

    public IncidenciaRepositorioJdbc(String url) {
        this.url = url;
        crearEsquema();
    }

    private Connection abrir() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private void crearEsquema() {
        try (Connection c = abrir(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS incidencias ("
                    + "id VARCHAR(64) PRIMARY KEY, "
                    + "tipo VARCHAR(20) NOT NULL, "
                    + "titulo VARCHAR(200) NOT NULL, "
                    + "descripcion VARCHAR(2000) NOT NULL, "
                    + "ubicacion VARCHAR(200) NOT NULL, "
                    + "prioridad VARCHAR(10) NOT NULL, "
                    + "estado VARCHAR(15) NOT NULL, "
                    + "creada_en BIGINT NOT NULL)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_incidencias_estado "
                    + "ON incidencias(estado)");
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo crear el esquema", e);
        }
    }

    @Override
    public void guardar(Incidencia i) {
        try (Connection c = abrir()) {
            try (PreparedStatement up = c.prepareStatement(
                    "UPDATE incidencias SET tipo=?, titulo=?, descripcion=?, "
                            + "ubicacion=?, prioridad=?, estado=?, creada_en=? WHERE id=?")) {
                up.setString(1, i.tipo().name());
                up.setString(2, i.getTitulo());
                up.setString(3, i.getDescripcion());
                up.setString(4, i.getUbicacion());
                up.setString(5, i.getPrioridad().name());
                up.setString(6, i.getEstado().name());
                up.setLong(7, i.getCreadaEn().toEpochMilli());
                up.setString(8, i.getId());
                if (up.executeUpdate() > 0) {
                    return;
                }
            }
            try (PreparedStatement in = c.prepareStatement(
                    "INSERT INTO incidencias (" + COLUMNAS
                            + ") VALUES (?,?,?,?,?,?,?,?)")) {
                in.setString(1, i.getId());
                in.setString(2, i.tipo().name());
                in.setString(3, i.getTitulo());
                in.setString(4, i.getDescripcion());
                in.setString(5, i.getUbicacion());
                in.setString(6, i.getPrioridad().name());
                in.setString(7, i.getEstado().name());
                in.setLong(8, i.getCreadaEn().toEpochMilli());
                in.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RepositorioException(
                    "No se pudo guardar la incidencia " + i.getId(), e);
        }
    }

    @Override
    public Optional<Incidencia> buscarPorId(String id) {
        try (Connection c = abrir();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT " + COLUMNAS + " FROM incidencias WHERE id=?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(reconstruir(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo leer la incidencia " + id, e);
        }
    }

    @Override
    public List<Incidencia> listarPorEstados(Collection<EstadoIncidencia> estados) {
        if (estados.isEmpty()) {
            return List.of();
        }
        String marcadores = String.join(",", estados.stream().map(e -> "?").toList());
        String sql = "SELECT " + COLUMNAS + " FROM incidencias WHERE estado IN ("
                + marcadores + ") ORDER BY creada_en, id";
        try (Connection c = abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            int n = 1;
            for (EstadoIncidencia e : estados) {
                ps.setString(n++, e.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Incidencia> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(reconstruir(rs));
                }
                return resultado;
            }
        } catch (SQLException e) {
            throw new RepositorioException("No se pudieron listar las incidencias", e);
        }
    }

    private static Incidencia reconstruir(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String titulo = rs.getString("titulo");
        String descripcion = rs.getString("descripcion");
        String ubicacion = rs.getString("ubicacion");
        Prioridad prioridad = Prioridad.valueOf(rs.getString("prioridad"));
        EstadoIncidencia estado = EstadoIncidencia.valueOf(rs.getString("estado"));
        Instant creadaEn = Instant.ofEpochMilli(rs.getLong("creada_en"));

        return switch (TipoIncidencia.valueOf(rs.getString("tipo"))) {
            case TECNOLOGIA -> new IncidenciaTecnologica(
                    id, titulo, descripcion, ubicacion, prioridad, creadaEn, estado);
            case INFRAESTRUCTURA -> new IncidenciaInfraestructura(
                    id, titulo, descripcion, ubicacion, prioridad, creadaEn, estado);
        };
    }
}
