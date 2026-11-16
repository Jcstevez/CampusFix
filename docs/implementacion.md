# Guía de implementación — CampusFix
Dirigida al equipo técnico de la universidad (o negocio) que va a instalar y operar CampusFix. No requiere conocer el código.

## 1. Qué es y qué hace
CampusFix recibe reportes de incidencias del campus (tecnología/infraestructura), las clasifica y prioriza solas, permite reportar escaneando un QR y escala las que superan su plazo (2 h alta, 24 h media, 72 h baja). Es **un solo contenedor** con una pantalla web y una API HTTP.

## 2. Requisitos previos
* Docker 24+ y Docker Compose v2 (o una plataforma que ejecute imágenes Docker).
* 1 vCPU, 512 MB de RAM, 1 GB de disco. Puerto TCP libre (8080 por defecto).
* Salida a internet solo para descargar la imagen (`ghcr.io`).
* Opcional: dominio/HTTPS (proxy inverso o la plataforma), SMTP (futuro).

## 3. Costos estimados
| Opción | Costo mensual aproximado |
|---|---|
| Render plan gratis (demo/piloto; se duerme, disco efímero) | USD 0 |
| Render o VPS pequeño con disco persistente | USD 7–10 |
| Servidor propio de la universidad | USD 0 adicional (infraestructura existente) |

## 4. Configuración (variables de entorno)
| Variable | Obligatoria | Por defecto | Descripción |
|---|---|---|---|
| `PORT` | No | `8080` | Puerto HTTP |
| `JDBC_URL` | No | `jdbc:h2:mem:campusfix;DB_CLOSE_DELAY=-1` | Base de datos. Para persistir: `jdbc:h2:file:/data/campusfix` |
| `CORREO_TI` | No | `soporte-ti@unisabana.edu.co` | Destino de avisos de tecnología |
| `CORREO_MANTENIMIENTO` | No | `mantenimiento@unisabana.edu.co` | Destino de avisos de infraestructura |

## 5. Instalación paso a paso
1. Crear una carpeta y guardar `docker-compose.yml` y `.env.example` (del repositorio).
2. `cp .env.example .env` y ajustar los valores.
3. Descargar y arrancar: `docker compose up -d` (la imagen oficial es `ghcr.io/jcstevez/campusfix:<versión>`; para usarla, cambiar `build: .` por `image: ghcr.io/jcstevez/campusfix:v1.0.0`).
4. Esperar ~30 s a que el contenedor quede `healthy`: `docker compose ps`.
5. Abrir `http://<servidor>:8080/` (pantalla web).

## 6. Verificación
* `curl http://<servidor>:8080/salud` → HTTP 200.
* En la pantalla, crear una incidencia de prueba; debe aparecer clasificada y priorizada.
* `curl http://<servidor>:8080/notificaciones` → lista los avisos generados.
* `docker compose ps` muestra `healthy`.

## 7. Operación
* **Logs:** `docker compose logs -f app`.
* **Reinicio:** `docker compose restart app`.
* **Actualizar:** cambiar el tag de la imagen y `docker compose up -d`.
* **Revisión de plazos:** programar `curl -X POST http://<servidor>:8080/seguimiento/revisar` cada 15 min (cron/Programador de tareas).
* **Respaldo:** con `JDBC_URL` en archivo, copiar el volumen `campusfix-data`.
* **Rollback:** volver al tag anterior de la imagen y `docker compose up -d`.

## 8. Solución de problemas
| Síntoma | Causa probable | Solución |
|---|---|---|
| Contenedor `unhealthy` | Puerto ocupado o falta de memoria | Cambiar `PORT`; subir RAM; ver logs |
| `Address already in use` | Otro servicio usa 8080 | Cambiar `PORT` en `.env` |
| Se pierden las incidencias al reiniciar | `JDBC_URL` en memoria | Usar `jdbc:h2:file:/data/campusfix` con volumen |
| `pull access denied` | Paquete GHCR privado | Hacer público el paquete o `docker login ghcr.io` |
| La web tarda ~1 min en abrir (Render gratis) | El servicio estaba dormido | Esperar; usar plan pago o un ping periódico |
| Los correos no llegan | Los avisos son **simulados** | Esperado en v1.0; ver §9 |
| `NumberFormatException` al arrancar | `PORT` no numérico | Corregir `.env` |

## 9. Recomendaciones para producción
* Autenticación/roles (SSO institucional) y restringir `/seguimiento/revisar` al personal.
* Conectar un servidor SMTP real (nuevo adaptador de salida; el dominio no cambia).
* Base de datos gestionada (PostgreSQL) y pool de conexiones en lugar de H2.
* HTTPS con proxy inverso, límites de tasa y monitoreo (`/salud`).
* Respaldos y retención de logs; actualizar la imagen cuando Dependabot/CI publiquen versiones nuevas.
