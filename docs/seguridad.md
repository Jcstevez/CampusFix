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
| Fecha | Herramienta | Hallazgo | Severidad | Estado | Justificación / commit |
|---|---|---|---|---|---|
| 2026-11-16 | Grype (SCA) | Varios GHSA high en jackson-databind y jackson-core 2.16.1 | High | Corregido | Jackson 2.16.1 a 2.21.7 en pom.xml (PR #7) |

## 4. Demostración: PR inseguro bloqueado
Se abrió un PR deliberadamente inseguro (agregando la dependencia `org.apache.logging.log4j:log4j-core:2.14.1`, vulnerable a Log4Shell CVE-2021-44228); la compuerta SCA lo bloqueó y **se cerró sin integrar**.
PR: [#22](https://github.com/Jcstevez/CampusFix/pull/22) (cerrado sin integrar) · Ejecución fallida: [job 5b](https://github.com/Jcstevez/CampusFix/actions/runs/43964396752/job/133639884224).

Resultado de Grype sobre log4j-core 2.14.1: **2 críticas, 1 alta y 6 medias**. El job 5b falló con `Found vulnerabilities with level 'high' or higher` y el job 6 (imagen Docker) también falló. Además, la regla de protección exigía 1 aprobación, así que el PR no se podía integrar.

## 5. Cómo se tratan los hallazgos nuevos
1. Dependabot/alerta → PR de corrección. 2. Si es falso positivo: se documenta aquí con evidencia. 3. Si no hay parche: riesgo aceptado con responsable y fecha de revisión.
