# Seguridad — CampusFix

## 1. Modelo de amenazas (STRIDE sobre el diagrama de contenedores)

Contenedores: **Navegador/App Sabana** → (HTTPS) → **CampusFix (JVM, HTTP + casos de uso)** → **H2 (JDBC)**; salidas simuladas a **Correo/Alertas**; y la **cadena de entrega** (GitHub → GHCR → Render).

| # | Amenaza (STRIDE) | Dónde | Impacto | Mitigación / estado |
|---|---|---|---|---|
| 1 | **S**poofing — suplantar a un usuario o al personal (no hay autenticación) | Navegador → API | Reportar o revisar a nombre de otro | **Riesgo aceptado en v1.0** (uso piloto); en producción se integra con el SSO de La Sabana (ver `implementacion.md` §9). Mitigación parcial: la app no expone datos personales |
| 2 | **T**ampering — modificar incidencias o endpoints de gestión (`/seguimiento/revisar`) sin autorización (**authZ**) | API | Escalar/alterar casos | **Riesgo aceptado** (sin roles); recomendación: token de personal o rol en el SSO. Entrada validada y JDBC con `PreparedStatement` (sin inyección SQL) |
| 3 | **R**epudiation — negar haber reportado/escalado | API/BD | Sin trazabilidad | Cada incidencia guarda fecha y estado; las notificaciones quedan en bandeja auditable (`/notificaciones`) |
| 4 | **I**nformation disclosure — secretos o datos en repo, logs o imagen | Repo, imagen | Fuga de credenciales | gitleaks sobre historial; secretos solo en GitHub Secrets; `.env` ignorado; imagen sin herramientas de build |
| 5 | **D**enial of service — inundar la API | API | Caída del servicio | SLO y pruebas de carga/estrés (`perf/`); límite de memoria de JVM; healthcheck + reinicio `unless-stopped` |
| 6 | **E**levation of privilege — contenedor ejecutándose como root | Contenedor | Compromiso del host | Imagen con usuario `app` no root; escaneo de imagen |
| 7 | **T**ampering — dependencia o acción de CI comprometida (cadena de suministro) | Pipeline | Código malicioso en producción | SCA (Grype), Dependabot, SHA de herramientas verificado, sin Trivy (ADR-003), permisos mínimos |
| 8 | **I**nformation disclosure — tráfico sin cifrar | Navegador ↔ API | Lectura de reportes | HTTPS terminado por Render en producción |

## 2. Compuertas de calidad (quality gates)

| Compuerta | Qué la bloquea | Dónde se ve |
|---|---|---|
| SAST | Hallazgo severidad ERROR sin tratar | job `security-sast`, `reporte-sast` |
| SCA | CVE alta/crítica con parche | job `security-sca`, `reporte-sca` |
| Secretos | Cualquier secreto, incluido historial | job `security-secrets` |
| Imagen | CVE crítica con parche | job `image`, `reporte-imagen` |

## 3. Hallazgos y tratamiento
<!-- COMPLETAR tras la primera ejecución: una fila por hallazgo real de los reportes. Estados válidos: corregido / falso positivo (con justificación) / riesgo aceptado (con justificación) -->
| Fecha | Herramienta | Hallazgo | Severidad | Estado | Justificación / commit |
|---|---|---|---|---|---|
| | | | | | |

## 4. Demostración: PR inseguro bloqueado
Se abrió un PR deliberadamente inseguro (agregando la dependencia `org.apache.logging.log4j:log4j-core:2.14.1`, vulnerable a Log4Shell CVE-2021-44228); la compuerta SCA lo bloqueó y **se cerró sin integrar**.
PR: <!-- COMPLETAR: enlace al PR cerrado --> · Ejecución fallida: <!-- COMPLETAR: enlace al run -->.

## 5. Cómo se tratan los hallazgos nuevos
1. Dependabot/alerta → PR de corrección. 2. Si es falso positivo: se documenta aquí con evidencia. 3. Si no hay parche: riesgo aceptado con responsable y fecha de revisión.
