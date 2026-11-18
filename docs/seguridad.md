# Seguridad — CampusFix

## 1. Modelo de amenazas (STRIDE sobre el diagrama de contenedores)

Contenedores: **Navegador/App Sabana** → (HTTPS) → **CampusFix (JVM, HTTP + casos de uso)** → **H2 (JDBC)**; salidas simuladas a **Correo/Alertas**; y la **cadena de entrega** (GitHub → GHCR → Render).

| # | Amenaza (STRIDE) | Componente o flujo | Impacto | Mitigación / estado | Cómo se verifica |
|---|---|---|---|---|---|
| 1 | **S**poofing — suplantar a un estudiante o al personal (no hay autenticación) | Navegador/App Sabana → API `POST /incidencias` | Reportar o revisar a nombre de otro | **Riesgo aceptado en v1.0** (piloto); en producción, SSO de La Sabana (`implementacion.md` §9). La app no expone datos personales | Declarado en §3 como riesgo aceptado; sin verificación automática (no hay usuarios aún) |
| 2 | **T**ampering / **E**levación — cambiar estados (`/atender`, `/resolver`) o forzar `/seguimiento/revisar` sin rol (**authZ**) | API → `GestionarIncidenciasUseCase`, `SeguimientoService` | Escalar o cerrar casos ajenos | **Riesgo aceptado** (sin roles); recomendación: token de personal o rol del SSO | Riesgo aceptado registrado en §3; pendiente: IT que espere 401/403 |
| 3 | **T**ampering — inyección SQL en el texto del reporte | API → `IncidenciaRepositorioJdbc` → H2 | Alterar/borrar la BD | `PreparedStatement` en todas las consultas; validación de entrada | SAST Semgrep (`p/owasp-top-ten`, reglas de SQLi) en el job 5a; `IncidenciaRepositorioJdbcIT` |
| 4 | **R**epudiation — negar haber reportado o escalado | API → H2 / bandeja de notificaciones | Sin trazabilidad | Cada incidencia guarda fecha y estado; avisos en bandeja auditable (`/notificaciones`) | `FlujoHttpIT` verifica la notificación generada por cada reporte |
| 5 | **I**nformation disclosure — secretos (Deploy Hook de Render) en repo, logs o imagen | Repositorio Git, imagen Docker, GitHub Actions | Despliegues no autorizados | Secreto solo en Environment `production`; `.env` en `.gitignore` y `.dockerignore` | gitleaks sobre todo el historial (job 5c) + Grype de imagen (job 6) |
| 6 | **D**enial of service — inundar `POST /incidencias/qr` (decodificar imágenes es costoso) | Internet → Render → contenedor | Caída del servicio | `-XX:MaxRAMPercentage`, healthcheck + reinicio; SLO medido | k6 post-despliegue con umbrales p95 < 500 ms y errores < 1 % (job 7b) |
| 7 | **E**levation of privilege — escapar del contenedor si corre como root | Contenedor `campusfix` en Render | Compromiso del host | `USER app` no root; imagen JRE mínima | Humo de la imagen en el job 6 corre con el usuario `app`; Grype de imagen (crítica con parche) |
| 8 | **T**ampering — dependencia o acción de CI comprometida (cadena de suministro) | Maven Central / GitHub Actions → imagen | Código malicioso en producción | SCA Grype, Dependabot, checksum del binario de gitleaks, permisos mínimos, sin Trivy (ADR-003) | Job 5b (alta/crítica con parche) y PR #22 bloqueado (log4j 2.14.1) |
| 9 | **I**nformation disclosure — tráfico sin cifrar | Navegador ↔ Render | Lectura de reportes | HTTPS terminado por Render | Prueba de humo y k6 usan la URL `https://` de `APP_URL` |

## 2. Compuertas de calidad (quality gates)

| Compuerta | Qué la bloquea | Dónde se ve |
|---|---|---|
| SAST | Hallazgo severidad ERROR sin tratar | job `security-sast`, `reporte-sast` |
| SCA | CVE alta/crítica con parche | job `security-sca`, `reporte-sca` |
| Secretos | Cualquier secreto, incluido historial | job `security-secrets` |
| Imagen | CVE crítica con parche | job `image`, `reporte-imagen` |

## 3. Hallazgos y tratamiento

Revisión de los reportes SARIF del CI de `main` ([CI #29](https://github.com/Jcstevez/CampusFix/actions/runs/44215083394), artefactos `reporte-sast`, `reporte-sca`, `reporte-secretos` y `reporte-imagen`). Cada hallazgo tiene una de tres decisiones: **corregido**, **falso positivo** o **riesgo aceptado**.

| Fecha | Control | Hallazgo | Severidad | Decisión | Justificación / evidencia |
|---|---|---|---|---|---|
| 2026-11-17 | SAST · Semgrep (`p/java`, `p/owasp-top-ten`, 170 reglas) | Sin hallazgos en `src/` | — | Sin acción | `semgrep.sarif` con 0 resultados |
| 2026-11-17 | SCA · Grype sobre `target/lib` | Sin vulnerabilidades altas/críticas con parche | — | Sin acción | `reporte-sca` con 0 resultados |
| 2026-11-17 | Secretos · gitleaks (historial completo) | Sin secretos en el código ni en el historial | — | Sin acción | `gitleaks.sarif` con 0 resultados |
| 2026-11-17 | Imagen · Grype | CVE-2026-82560 en `perl-base` 5.38.2-3.2ubuntu0.6 (corregido en 0.7) | Media (CVSS 7,5) | Corregido | Paquete del sistema operativo de la imagen base `eclipse-temurin:21-jre-noble` (Ubuntu 24.04). No bloquea (umbral: crítica con parche), pero se corrige actualizando los paquetes del SO en la etapa final del `Dockerfile` (`apt-get upgrade`). Commit: _agregar enlace_ |
| 2026-11-17 | Imagen · Grype | CVE-2026-84243 en glibc (`libc6`, `libc-bin`, `locales` 2.39-0ubuntu8.9) | Media (7,0) | Corregido | Igual que la anterior: misma corrección en el `Dockerfile` |
| 2026-11-17 | Imagen · Grype | CVE-2026-86805 en glibc (3 paquetes) | Media (6,3) | Corregido | Misma corrección |
| 2026-11-17 | Imagen · Grype | CVE-2026-8674 en glibc (3 paquetes; corregido en 2.39-0ubuntu8.12) | Media (5,3) | Corregido | Misma corrección |
| 2026-11-17 | Imagen · Grype | CVE-2026-18374 en glibc (3 paquetes) | Media (4,9) | Corregido | Misma corrección |
| 2026-11-17 | Imagen · Grype | CVE-2026-97399 en glibc (3 paquetes) | Media (3,7) | Corregido | Misma corrección |
| 2026-11-17 | Imagen · Grype | CVE-2026-95818 en glibc (3 paquetes) | Media (3,6) | Corregido | Misma corrección |
| 2026-11-16 | SCA · Grype | Varios GHSA altos en `jackson-databind` y `jackson-core` 2.16.1 | Alta | Corregido | Jackson 2.16.1 → 2.21.7 en `pom.xml` (PR #7) |
| 2026-11-16 | SCA · Grype (prueba intencional) | `log4j-core` 2.14.1, Log4Shell CVE-2021-44228: 2 críticas, 1 alta, 6 medias | Crítica | No integrado | PR #22 bloqueado y cerrado sin fusionar (sección 4) |
| — | Diseño (STRIDE #1 y #2) | Sin autenticación ni autorización en la API | Alta | Riesgo aceptado | Piloto sin datos personales. Para corregirlo: integrar el SSO de La Sabana y restringir `/atender`, `/resolver` y `/seguimiento/revisar` al personal (`implementacion.md` §9). Responsable: equipo; revisión antes de producción |

Las 7 CVE de la imagen aparecen como 19 coincidencias en el reporte, porque glibc se instala en tres paquetes (`libc6`, `libc-bin`, `locales`). Ninguna superaba el umbral (crítica con parche), por eso el job 6 pasó; se corrigen igual para no acumular deuda.

## 4. Demostración: PR inseguro bloqueado
Se abrió un PR deliberadamente inseguro (agregando la dependencia `org.apache.logging.log4j:log4j-core:2.14.1`, vulnerable a Log4Shell CVE-2021-44228); la compuerta SCA lo bloqueó y **se cerró sin integrar**.
PR: [#22](https://github.com/Jcstevez/CampusFix/pull/22) (cerrado sin integrar) · Ejecución fallida: [job 5b](https://github.com/Jcstevez/CampusFix/actions/runs/43964396752/job/133639884224).

Resultado de Grype sobre log4j-core 2.14.1: **2 críticas, 1 alta y 6 medias**. El job 5b falló con `Found vulnerabilities with level 'high' or higher` y el job 6 (imagen Docker) también falló. Además, la regla de protección exigía 1 aprobación, así que el PR no se podía integrar.

## 5. Cómo se tratan los hallazgos nuevos
1. Dependabot/alerta → PR de corrección. 2. Si es falso positivo: se documenta aquí con evidencia. 3. Si no hay parche: riesgo aceptado con responsable y fecha de revisión.
