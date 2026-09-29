# CampusFix

Sistema de gestión y seguimiento de incidencias universitarias (tecnología e
infraestructura), pensado como una nueva función de la **App Sabana**
("Reportar alerta"). Proyecto de *Diseño y Arquitectura de Software*.

- **Corte 1:** diseño con SOLID y patrones (Factory Method, Strategy, Adapter, Observer) → `docs/*.md`.
- **Corte 2:** retos → estilo arquitectónico (**hexagonal**) → pruebas unitarias, de integración y de carga.

Interfaz de la app (maqueta): repositorio `Interfaz_Arquitectura`.

## Retos asignados

1. **Reportes que no queden en el olvido**: alertas sobre los objetos reportados y correos al área de tecnología.
2. **Clasificación automática** (tecnología / infraestructura) y decidir qué es prioritario.
3. **Simular QR** para reportar de forma más eficiente al escanear el objeto.

## Documentación

| Documento | Contenido |
|---|---|
| [docs/arquitectura.md](docs/arquitectura.md) | Descripción, arquitectura inicial vs evolucionada, límites y trabajo del Corte 3 |
| [docs/adr/ADR-001-arquitectura-hexagonal.md](docs/adr/ADR-001-arquitectura-hexagonal.md) | Comparación de 4 estilos y decisión con consecuencias |
| [docs/diagramas/README.md](docs/diagramas/README.md) | C4: contexto, contenedores, componentes y secuencia |
| [docs/pruebas.md](docs/pruebas.md) | Estrategia de pruebas y resultados |
| [perf/SLO.md](perf/SLO.md) · [perf/README.md](perf/README.md) | SLO y ejecución de las pruebas de carga |

## Cómo ejecutar

Requisitos: **JDK 21 o superior**, **Maven 3.9+**, y [k6](https://k6.io) solo para carga.

```bash
mvn test        # pruebas unitarias
mvn verify      # unitarias + integración (H2 embebida, QR real, HTTP caja negra) + cobertura
                # cobertura: target/site/jacoco/index.html
mvn compile exec:java   # arranca en http://localhost:8080 (abre esa direccion en el navegador: pantalla web)
```

Carga: `powershell -ExecutionPolicy Bypass -File perf/ejecutar.ps1` (o `bash perf/ejecutar.sh`).

### Probar a mano (con la app arriba)

```bash
# Reto 2: el sistema clasifica solo (aquí: TECNOLOGIA / ALTA por ser laboratorio)
curl -X POST localhost:8080/incidencias -H "Content-Type: application/json" \
  -d '{"titulo":"Proyector no funciona","descripcion":"No enciende","ubicacion":"Bloque C - Laboratorio 101"}'

# Reto 3: "imprimir" el QR de un objeto y "escanearlo"
curl -o proyector.png localhost:8080/qr/PROY-B302.png
curl -X POST "localhost:8080/incidencias/qr?descripcion=No%20enciende" \
  -H "Content-Type: image/png" --data-binary @proyector.png
#   o con el texto ya leído por la cámara:
curl -X POST localhost:8080/incidencias/qr -H "Content-Type: application/json" \
  -d '{"qr":"campusfix://objeto/PROY-B302","descripcion":"No enciende"}'

# Reto 1: avisos generados y revisión de pendientes
curl localhost:8080/notificaciones
curl -X POST localhost:8080/seguimiento/revisar
```

Objetos con QR de ejemplo: `PROY-B302`, `PC-C101-01`, `WIFI-A2`, `PUERTA-A201`, `LUZ-A201`, `BANO-B1`.

| Método y ruta | Descripción |
|---|---|
| `POST /incidencias` | Reporta con texto; clasifica tipo y prioridad |
| `POST /incidencias/qr` | Reporta con QR (JSON `{qr, descripcion}` o imagen `image/png`) |
| `GET /incidencias[?estado=]` · `GET /incidencias/{id}` | Consulta |
| `POST /incidencias/{id}/atender` · `/resolver` | Cambia el estado |
| `POST /seguimiento/revisar` | Escala y avisa lo que superó su plazo |
| `GET /notificaciones` | Bandeja simulada de correos y alertas |
| `GET /qr/{objetoId}.png` | QR (PNG) del objeto |
| `GET /salud` | Estado del servicio |

## Tabla de trazabilidad

Cada reto → atributo de calidad → decisión → dónde está → prueba → resultado.
Resultados medidos el 28-sep-2026 con `mvn clean verify` (156 unitarias + 40 de
integración, 0 fallas, cobertura 96,6 % instrucciones / 82,0 % ramas) y k6
(detalle y salvedades en `docs/pruebas.md`, sección 3).

| Reto | Atributo de calidad | Decisión arquitectónica | Dónde está | Prueba | Resultado |
|---|---|---|---|---|---|
| **1a.** Alertas sobre los objetos reportados y correos a tecnología, sin que un fallo pierda el reporte | Modificabilidad, disponibilidad | Puertos `Notificador`/`MantenimientoPuerto` + Observer tolerante a fallas; adaptadores intercambiables (correo hoy simulado) | `application/AlertaObjetoReportadoObserver`, `CorreoTecnologiaObserver`, `MantenimientoInfraestructuraObserver`; `observer/IncidenciaSubject`; `adapter/out/notification`, `adapter/out/mantenimiento` | Unitarias `ObserversDeNotificacionTest`, `IncidenciaSubjectTest` (un observer que falla no afecta a los demás); integración `FlujoHttpIT` (correo a TI y alerta) | Unitarias 156/156 ✔ · `FlujoHttpIT` 26/26 ✔ con H2 y ZXing reales. **Límite:** correo simulado, avisos síncronos |
| **1b.** Que ningún reporte quede en el olvido | Confiabilidad / seguimiento | Estado de ciclo de vida + `PoliticaSla` por prioridad + caso de uso de revisión que escala y avisa | `domain/incidencia/Incidencia` (estado, `estaVencida`), `domain/seguimiento/PoliticaSla`, `application/SeguimientoService` | Unitarias `SeguimientoServiceTest` (límites 1 min antes / justo en el plazo; por prioridad; sin re-avisar), `IncidenciaTest`; `FlujoHttpIT` con reloj controlable; k6 `carga` (`POST /seguimiento/revisar`) | Unitarias ✔ · HTTP ✔ · `POST /seguimiento/revisar` p95 33,8 ms con 50 VUs (SLO ≤ 1000 ms) ✔. **Límite:** se dispara a mano (falta scheduler) |
| **2.** Clasificar automáticamente tecnología/infraestructura y decidir la prioridad | Modificabilidad, testeabilidad | Estrategia de clasificación detrás de la interfaz `ClasificadorIncidencia` (reglas hoy, ML mañana) + Factory Method elige el tipo | `domain/clasificacion/*`, `factory/*`, `application/IncidenciaService` | Unitarias `ClasificadorPorReglasTest` (clases de equivalencia, empate, palabras cortas, ubicación sensible); `FlujoHttpIT`; k6 `POST /incidencias` | Unitarias ✔ (incl. mutación: 7 defectos detectados) · HTTP ✔ · `POST /incidencias` p95 3,2 ms con 50 VUs (SLO ≤ 500 ms) ✔. **Límite:** palabras clave, sin contexto ni medición de precisión con datos reales |
| **3.** Simular QR y leerlos para reportar más eficiente | Usabilidad (menos pasos), modificabilidad | El QR solo lleva el id del objeto; catálogo resuelve tipo/ubicación; QR detrás de puertos `GeneradorQr`/`DecodificadorQr` (adaptador ZXing) | `domain/catalogo/CodigoQr`, `application/ReporteQrService`, `adapter/out/qr/ZxingAdapter`, `adapter/out/catalog` | Unitarias `CodigoQrTest`, `ReporteQrServiceTest`; integración `ZxingAdapterIT` (generar→leer, imagen sin QR); `FlujoHttpIT` (imprimir→escanear); k6 `POST /incidencias/qr` | Unitarias ✔ · HTTP ✔ · `ZxingAdapterIT` 6/6 ✔ (ZXing real) · `POST /incidencias/qr` p95 3,1 ms con 50 VUs (SLO ≤ 500 ms) ✔. **Límite:** no hay cámara real; se recibe imagen o texto |
| **Arquitectura** (transversal) | Modificabilidad | Regla de dependencia hexagonal | Toda la estructura de paquetes | `ArquitecturaTest` (lee los imports de `src/main`) | 8/8 ✔ |

## Estructura del repositorio

```
src/main/java/com/campusfix   domain · factory · strategy · observer · application · adapter · bootstrap
src/test/java/com/campusfix   pruebas unitarias (*Test) e integración (*IT) + support (dobles)
perf/                         SLO, scripts k6 y reportes
docs/                         arquitectura, ADR, diagramas, pruebas y documentos del Corte 1
```
