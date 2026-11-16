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
<!-- COMPLETAR: pegar la retroalimentación recibida y, por cada punto, qué se hizo o por qué no -->
| Comentario del profesor | Respuesta |
|---|---|
| | |

## 6. Límites finales
Sin SSO/roles; correo simulado; datos en H2 (efímeros en el plan gratis de Render); una sola instancia (indisponibilidad breve al desplegar); el seguimiento de plazos no es automático.
