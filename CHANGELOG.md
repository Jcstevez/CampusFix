# Changelog
Formato [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) · versionado semántico.

## [1.0.0] — 2026-11-16
### Añadido
- Pipeline CI (build, unitarias con cobertura de dominio ≥ 70 %, integración, reglas de arquitectura, SAST, SCA, secretos, imagen + escaneo).
- Pipeline CD: publicación en GHCR (tag SHA/versión), despliegue automático en Render, humo y k6 con umbrales del SLO, rollback manual.
- Contenerización: Dockerfile multi-etapa, usuario no root, healthcheck; `docker compose up` con volumen y variables.
- Configuración por variables de entorno (`PORT`, `JDBC_URL`, `CORREO_TI`, `CORREO_MANTENIMIENTO`).
- Documentación: `devops`, `seguridad` (STRIDE), ADR-002, ADR-003, `evolucion`, `implementacion`, vista de despliegue C4.
- Dependabot (Maven, Actions, Docker).

## [0.2.0] — Corte 2
Arquitectura hexagonal, pruebas unitarias/integración/carga (k6), pantalla web.

## [0.1.0] — Corte 1
Diseño con SOLID y patrones (Factory Method, Strategy, Adapter, Observer).
