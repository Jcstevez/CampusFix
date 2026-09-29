# SLO de las pruebas de carga (definidos ANTES de ejecutar)

> Estos objetivos se fijaron antes de correr las pruebas. Si se cambian,
> hay que hacerlo antes de ejecutar y dejar constancia en `docs/pruebas.md`.

Sistema bajo prueba: CampusFix corriendo localmente (`mvn compile exec:java`),
con H2 en memoria. Los tiempos son de **latencia del servidor medida por el
cliente k6 en la misma maquina**.

| Endpoint (nombre en k6)        | Reto / atributo de calidad                              |
|--------------------------------|---------------------------------------------------------|
| `POST /incidencias`            | Reto 2 (clasificacion automatica) · rendimiento         |
| `POST /incidencias/qr`         | Reto 3 (reporte eficiente por QR) · rendimiento         |
| `GET /incidencias/:id`        | Reto 1 (consultar el seguimiento) · rendimiento         |
| `POST /seguimiento/revisar`    | Reto 1 (que nada quede en el olvido) · escalabilidad    |

## Escenarios y objetivos

| Escenario | Tipo | Perfil | SLO (todos deben cumplirse) |
|-----------|------|--------|------------------------------|
| `baseline` | linea base | 1 usuario virtual, 30 s | p95 <= 100 ms por endpoint · errores = 0 % |
| `carga`    | carga esperada | rampa a 50 VUs (30 s), 50 VUs sostenidos (60 s), rampa a 0 (30 s); ademas `seguimiento/revisar` cada 5 s | p95 <= 500 ms por endpoint (`revisar` <= 1000 ms) · errores < 1 % |
| `estres`   | estres (buscar el limite) | 50 -> 100 -> 200 -> 300 -> 400 VUs, 45 s por escalon | Referencia: p95 <= 1000 ms y errores < 5 %. Se documenta **en que escalon deja de cumplirse** y por que |

Justificacion de los numeros: 50 usuarios simultaneos reportando es un
escenario generoso para un campus mediano (los reportes son eventos
esporadicos, no consultas continuas); 500 ms es el umbral en el que una
accion se siente "inmediata" en una app movil.

## Como se ejecutan

Ver `perf/README.md`.
