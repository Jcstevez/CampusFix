# Pruebas de carga (k6)

Requisitos: [k6](https://k6.io/docs/get-started/installation/) (`winget install k6` en Windows), JDK 23 y Maven.
El SLO esta en [`SLO.md`](SLO.md) (definido antes de ejecutar).

## Un solo comando (arranca la app, corre los 3 escenarios y la apaga)

Windows (PowerShell), desde la raiz del repo:

    powershell -ExecutionPolicy Bypass -File perf/ejecutar.ps1

Linux / macOS:

    bash perf/ejecutar.sh

## A mano (dos terminales, desde la raiz del repo)

    # Terminal 1
    mvn -q compile exec:java

    # Terminal 2 (reiniciar la app entre escenarios para partir de base vacia)
    k6 run perf/scripts/baseline.js
    k6 run perf/scripts/carga.js
    k6 run perf/scripts/estres.js

Cada escenario deja su resumen en `perf/reports/<escenario>.md`.
Otra URL: `k6 run -e BASE_URL=http://host:puerto perf/scripts/carga.js`.
