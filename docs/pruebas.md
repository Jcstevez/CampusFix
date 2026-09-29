# Estrategia y resultados de pruebas

## 1. Qué se prueba en cada nivel y por qué

| Nivel | Qué prueba | Infraestructura | Clases | Comando |
|---|---|---|---|---|
| **Unitarias** | Reglas de negocio aisladas: `Incidencia` (validación, transiciones, vencimiento), `ClasificadorPorReglas`, `CodigoQr`, `PoliticaSla`, servicios de aplicación, observers, patrones del Corte 1, y la **regla de arquitectura** | Ninguna (sin BD, red ni frameworks). Dobles hechos a mano: *fakes* (repositorio en memoria), *stubs* (clasificador/catálogo/decodificador fijos), *spies* (notificador/observer que registran llamadas) | `*Test` | `mvn test` |
| **Integración** | Las fronteras del hexágono con infraestructura real: repositorio JDBC ↔ **H2 embebida**, QR real con **ZXing** (generar → leer), y flujo de **caja negra por HTTP** con la app completa | H2 embebida y servidor HTTP real; sin mocks | `*IT` | `mvn verify` |
| **Carga** | Que los retos se atiendan bajo presión: latencia p95, throughput y errores | App real + k6 | `perf/scripts/*.js` | `perf/ejecutar.ps1` / `.sh` |

Convenciones de las unitarias: patrón **AAA** (Arrange–Act–Assert), nombres
que describen el comportamiento, **clases de equivalencia** (tecnología /
infraestructura / sin coincidencias / empate; prioridades) y **valores
límite** (título de 200 vs 201 caracteres, descripción 2000 vs 2001,
identificador de QR 40 vs 41, SLA a 1 min antes / justo en / después del
plazo, capacidad de la bandeja).

Convenciones de las de integración: cada prueba levanta su **propia
instancia y base H2** (nombre único) → no dependen del orden ni comparten
datos. `FlujoHttpIT` usa **solo la interfaz HTTP pública** (caja negra) y un
reloj controlable para probar el seguimiento sin esperar horas.

## 2. Cómo ejecutarlas

    mvn test                  # unitarias
    mvn verify                # unitarias + integración + reporte de cobertura
    # cobertura (JaCoCo):  target/site/jacoco/index.html
    # reportes JUnit:      target/surefire-reports  y  target/failsafe-reports

Carga: ver [`perf/README.md`](../perf/README.md). SLO en [`perf/SLO.md`](../perf/SLO.md).

## 3. Resultados

> **Cómo se obtuvieron.** Todo se ejecutó en el portátil del equipo
> (Windows 11, JDK 21.0.12 Temurin, Maven 3.9.16, k6 2.2.0) el 28-sep-2026, con
> `mvn clean verify -Dmaven.compiler.release=21` y `perf/ejecutar.ps1`, contra
> la aplicación real (H2 y ZXing reales). Cliente de carga y servidor corrieron
> en la **misma máquina**, así que las cifras son una cota optimista y sirven
> para comparar versiones, no para dimensionar producción.
>
> Además, en el desarrollo se hizo una verificación por mutación (cambiar `>=`
> del SLA y el desempate del clasificador): 7 pruebas fallaron, o sea que
> detectan el defecto.

### 3.1 Unitarias e integración (`mvn clean verify`: BUILD SUCCESS)

| Suite | Pruebas | Correctas | Fallas |
|---|---|---|---|
| Unitarias (surefire) | 156 | 156 | 0 |
| Integración (failsafe): `FlujoHttpIT` 26, `IncidenciaRepositorioJdbcIT` 8 (H2), `ZxingAdapterIT` 6 | 40 | 40 | 0 |

Cobertura JaCoCo (unitarias + integración, 47 clases): **96,6 % de instrucciones,
95,6 % de líneas, 82,0 % de ramas**. Paquetes `domain.*`: 98,3 % instrucciones
en `incidencia`, 100 % en `clasificacion`, `catalogo` y `seguimiento`, 81,5 % en
`error`. Lo menos cubierto: `adapter.out.qr` (86,3 %) y `adapter.out.persistence`
(90,5 %), sobre todo ramas de error de E/S. Reporte: `target/site/jacoco/index.html`.

### 3.2 Carga (k6, SLO definido antes en `perf/SLO.md`)

| Escenario | Endpoint | p95 (ms) | Throughput (req/s) | Errores | SLO | Resultado |
|---|---|---|---|---|---|---|
| baseline (1 VU, 30 s) | POST /incidencias | 5,9 | 9,2 (total) | 0 % | p95 ≤ 100 ms | Cumple |
| baseline | POST /incidencias/qr | 6,5 | | 0 % | p95 ≤ 100 ms | Cumple |
| baseline | GET /incidencias/:id | 2,8 | | 0 % | p95 ≤ 100 ms | Cumple |
| carga (hasta 50 VUs, 2 min) | POST /incidencias | 3,2 | 352 (total) | 0 % | p95 ≤ 500 ms, err < 1 % | Cumple |
| carga | POST /incidencias/qr | 3,1 | | 0 % | idem | Cumple |
| carga | GET /incidencias/:id | 2,0 | | 0 % | idem | Cumple |
| carga | POST /seguimiento/revisar | 33,8 | | 0 % | p95 ≤ 1000 ms | Cumple |
| estrés (hasta 400 VUs, 4 min) | POST /incidencias | 2,7 | 1480 (total) | 0 % | p95 ≤ 1000 ms, err < 5 % | Cumple |
| estrés | POST /incidencias/qr | 2,7 | | 0 % | idem | Cumple |
| estrés | GET /incidencias/:id | 3,0 | | 0 % | idem | Cumple |

Volumen: baseline 277 peticiones, carga 42 286, estrés 355 275; ninguna falló.
**Con 400 usuarios virtuales no se llegó al punto de quiebre**: no hubo errores
ni degradación del p95, así que el límite real del sistema es mayor que lo
probado (y la máquina compartida con k6 pudo ser la restricción).

### 3.3 Análisis del cuello de botella

**Lo que sí muestran los datos**

1. `POST /seguimiento/revisar` es, de lejos, el endpoint más lento en carga
   (p95 33,8 ms frente a 2–3 ms de los demás, unas 10 veces más). Es coherente
   con su diseño: recorre todas las incidencias abiertas, así que su costo crece
   con los reportes acumulados. Es el primer candidato a cuello de botella.
2. El baseline mostró ~44 ms por GET antes de activar `TCP_NODELAY` en el
   servidor; tras el cambio bajó a ~2 ms (fue un hallazgo de la medición, no de
   la teoría).
3. Un solo `POST /incidencias/qr` del escenario de estrés tardó **52,9 s**
   (máximo), mientras su p95 fue 2,7 ms. Es una petición aislada entre miles y
   **no está explicada**: hipótesis, cola de conexiones del servidor
   (`backlog` por defecto) al subir a 400 VUs, o una pausa del JVM. Habría que
   repetir el estrés y registrar cuántas peticiones superan 1 s antes de
   afirmar la causa.

**Hipótesis que los datos no confirmaron ni descartaron:** la conexión JDBC por
operación (sin pool) no se vio como problema hasta 1480 req/s; y los avisos
síncronos son baratos porque la bandeja es en memoria (con SMTP real serían el
primer problema).

**Qué ofrece la arquitectura para mitigar:** el repositorio está detrás de un
puerto, así que agregar un pool o consultar solo lo vencido en `revisarPendientes`
es cambiar **un adaptador**; los avisos pueden pasar a una cola asíncrona
cambiando el `Notificador`. Lo que **no** ofrece: escalado horizontal
independiente (monolito modular con base embebida).

