# Evolución de CampusFix (Cortes 1 → 3)

## 1. Los tres cortes
| Corte | Entregable | Qué se decidió |
|---|---|---|
| 1 | Diseño | SOLID y patrones: Factory Method (creación de incidencias), Strategy (prioridad/SLA), Adapter (QR, notificaciones), Observer (avisos) |
| 2 | Arquitectura y pruebas | Estilo **hexagonal** (ADR-001), pruebas unitarias/integración/carga, pantalla web, k6 con SLO |
| 3 | Entrega continua y segura | Contenedor, pipeline CI/CD, DevSecOps, despliegue público, guía de implementación |

## 2. Decisiones que se mantuvieron
* Hexagonal (puertos/adaptadores): permitió cambiar de H2 en memoria a archivo y de simulación a contenedor **sin tocar el dominio**; ahora además se **verifica en el pipeline** (`ArquitecturaTest`).
* Un solo despliegue (monolito modular): suficiente para el SLO medido en el Corte 2.
* Java 21 + Maven + JDK HttpServer (pocas dependencias = menor superficie de ataque).

## 3. Decisiones que cambiaron
| Antes | Ahora | Por qué |
|---|---|---|
| Configuración fija en código | Variables de entorno (`Config.desde`) | 12-factor; mismo artefacto en todos los entornos |
| Ejecución local con `mvn exec:java` | Imagen Docker inmutable + Render | Entrega reproducible y rollback |
| Pruebas manuales | CI con compuertas (cobertura ≥ 70 %, arquitectura, seguridad) | Calidad automática |

## 4. Pendientes del Corte 2 (`arquitectura.md` §7) y su resolución
| Pendiente declarado | Estado en Corte 3 |
|---|---|
| Correo/alertas simulados | **Se mantiene simulado** (límite declarado); `implementacion.md` §9 indica cómo conectar SMTP real |
| Notificaciones síncronas | Se mantiene; el SLO medido lo tolera |
| Seguimiento solo manual | Se mantiene manual (`/seguimiento/revisar`); recomendado un programador externo (cron) — §9 de implementación |
| Sin autenticación/autorización | **Riesgo aceptado**, documentado en STRIDE (`seguridad.md` #1–#2) |
| Catálogo en memoria, H2 en memoria | Mitigado parcialmente: H2 en archivo con volumen `/data` vía `JDBC_URL` |
| Sin pool de conexiones | Se mantiene; recomendado en producción |
| Valor atípico de 52,9 s sin explicar | Se vigila con k6 de despliegue (umbral p95) |

## 5. Retroalimentación del profesor (Corte 2)
Calificación del Corte 2: **81,5/90** en la rúbrica y **9,73/10** en la evaluación entre pares (equivalente a **91,23/100**).

| Comentario del profesor | Respuesta |
|---|---|
| Proceso: integración sin control de calidad ni revisión | **Resuelto en el Corte 3.** Flujo con ramas y PR, rama `main` protegida (ruleset: PR + 1 aprobación + 8 checks obligatorios, sin force push), aprobaciones de otros integrantes (PR #7 y #21). |
| Despliegue manual | **Resuelto.** CD automático tras CI verde (workflow `cd.yml`), con smoke test y prueba de carga k6, más rollback manual probado y Release v1.0.0. |
| Paquete `strategy` con código sin uso | **No resuelto.** Se declara como límite; no se alcanzó a refactorizar. |
| Falta prueba end-to-end del flujo de mantenimiento | **No resuelto.** Se declara como límite. |
| Precisión del clasificador | **No resuelto.** Sin mediciones nuevas; se declara como límite. |
| Prueba de punto de quiebre y decodificación de QR bajo carga | **Parcial.** Desde el Corte 3 el k6 post-despliegue (`perf/scripts/despliegue.js`) mide `POST /incidencias`, `POST /incidencias/qr`, `GET /incidencias/:id` y `POST /seguimiento/revisar` con los umbrales del SLO del Corte 2 en cada despliegue. El punto de quiebre sigue sin medirse. |
| Mutantes no reproducibles; valor atípico de 52,9 s | **No resuelto.** No se reprodujo; se declara sin inventar datos. |

## 6. Límites finales
Sin SSO/roles; correo simulado; datos en H2 (efímeros en el plan gratis de Render); una sola instancia (indisponibilidad breve al desplegar); el seguimiento de plazos no es automático.
