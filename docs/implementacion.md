# Guía de implementación — CampusFix

Esta guía es para el equipo técnico de la universidad que va a poner en marcha CampusFix en su propia infraestructura y operarlo después. Se asume que conocen Docker y Git, pero no el proyecto. Todos los nombres de variables, servicios, workflows y comandos son los del repositorio.

Los valores que deben cambiar se marcan así: `<usuario-github>`, `<servidor>`.

---

## 1. Qué se va a implementar

CampusFix recibe reportes de daños del campus (tecnología o infraestructura). Los clasifica y prioriza solo, permite reportar escaneando el QR del objeto y escala lo que supera su plazo (2 h prioridad alta, 24 h media, 72 h baja). Tiene una pantalla web y una API HTTP.

Componentes que se instalan:

| Componente | Qué es | Dónde corre |
|---|---|---|
| `campusfix` | Un contenedor con Java 21: API HTTP, pantalla web, casos de uso y base de datos H2 embebida | Docker (servidor propio) o Render |
| Volumen `campusfix-data` | Archivos de la base H2 (`/data/campusfix.mv.db`) | Disco del servidor |
| Imagen `ghcr.io/<usuario-github>/campusfix` | La imagen que construye el pipeline, etiquetada con el SHA del commit y con la versión (`v1.0.0`) | GitHub Container Registry |
| Pipeline (`.github/workflows/`) | `ci.yml`, `cd.yml`, `release.yml`, `rollback.yml` | GitHub Actions |

Vista de despliegue: [docs/diagramas/despliegue.md](diagramas/despliegue.md).

Hay dos formas de instalarlo. Escojan una:

- **Opción A — Servidor propio con Docker Compose.** Recomendada para la universidad: datos en su disco, sin depender de un plan gratis.
- **Opción B — Render + pipeline completo.** Es como está desplegado en el proyecto: cada cambio en `main` se despliega solo.

---

## 2. Requisitos previos

**Cuentas**

| Cuenta | Para qué | Opción |
|---|---|---|
| GitHub (organización o usuario) | Código, pipeline y registro de imágenes (GHCR) | A y B |
| Render | Ejecutar el contenedor con URL pública HTTPS | Solo B |

**Software** (en el servidor o en el equipo desde el que se opera)

| Software | Versión mínima | Comprobar con |
|---|---|---|
| Docker Engine | 24 | `docker --version` |
| Docker Compose (plugin v2) | 2.20 | `docker compose version` |
| Git | 2.40 | `git --version` |
| curl | cualquiera | `curl --version` |
| JDK 21 y Maven 3.9 | Solo si van a compilar o correr pruebas fuera de Docker | `java -version`, `mvn -v` |

**Recursos mínimos del servidor:** 1 vCPU, 1 GB de RAM (la JVM usa como máximo el 75 % de la memoria del contenedor), 2 GB de disco libre (la imagen pesa unos 300 MB; el resto es para datos y copias de seguridad).

**Red:** puerto TCP `8080` libre (o el que pongan en `PORT`). Salida a internet hacia `ghcr.io` para descargar la imagen.

**Dominio y HTTPS (producción):** un nombre como `campusfix.<dominio-universidad>` y un certificado. En la opción B, Render da HTTPS. En la opción A, pongan un proxy inverso (Nginx, Caddy o el balanceador de la universidad) delante del puerto 8080.

---

## 3. Costos estimados

Precios mensuales en dólares de noviembre de 2026; revísenlos en la página de cada proveedor antes de presupuestar.

| Componente | Plan usado en el proyecto | Plan recomendado para la universidad |
|---|---|---|
| GitHub (repositorio, Actions, GHCR) | Gratis (repositorio público) | Gratis si es público; GitHub Team USD 4 por usuario si es privado |
| Render — servicio web | Free: USD 0, se duerme tras 15 min sin uso y el disco es efímero | Starter: USD 7 (0,5 CPU, 512 MB, siempre encendido) |
| Render — disco persistente para `/data` | No disponible en Free | 1 GB a USD 0,25 |
| Servidor propio (opción A) | — | USD 0 adicional si usan una VM existente; una VPS pequeña cuesta USD 5–10 |
| **Total** | **USD 0** | **Opción B: ~USD 7,25 · Opción A: USD 0–10** |

---

## 4. Configuración

### 4.1 Variables de entorno de la aplicación

Se definen en el archivo `.env` (copia de `.env.example`) o en el panel de Render.

| Variable | Para qué sirve | Ejemplo | Obligatoria | Dónde se configura |
|---|---|---|---|---|
| `PORT` | Puerto HTTP donde escucha la app | `8080` | No (por defecto `8080`) | `.env` · panel de Render (Render la pone sola) |
| `JDBC_URL` | Base de datos H2. En memoria se pierde al reiniciar; en archivo persiste en `/data` | `jdbc:h2:file:/data/campusfix` | No (por defecto en memoria) | `.env` · panel de Render |
| `CORREO_TI` | Destino de los avisos de incidencias de tecnología | `soporte-ti@unisabana.edu.co` | No | `.env` · panel de Render |
| `CORREO_MANTENIMIENTO` | Destino de los avisos de infraestructura | `mantenimiento@unisabana.edu.co` | No | `.env` · panel de Render |

En producción, usen siempre `JDBC_URL=jdbc:h2:file:/data/campusfix`. Ninguna de estas variables es secreta.

### 4.2 Secretos y variables del pipeline (solo opción B)

Se configuran en GitHub: **Settings → Environments → `production`**.

| Nombre | Tipo | Para qué sirve | Ejemplo | Cómo obtenerlo |
|---|---|---|---|---|
| `RENDER_DEPLOY_HOOK_URL` | Secret | URL que llama el CD para que Render despliegue una imagen | `https://api.render.com/deploy/srv-xxxx?key=yyyy` | Render → el servicio → **Settings → Deploy Hook** → copiar |
| `APP_URL` | Variable | URL pública que usan la prueba de humo y k6 | `https://campusfix-xxxx.onrender.com` | Render → la URL que aparece arriba del servicio |
| `GITHUB_TOKEN` | Automático | Publicar la imagen en GHCR | — | Lo crea GitHub Actions; no se configura |

El Deploy Hook es la única credencial sensible: quien lo tenga puede desplegar en su servicio. No lo pongan en el repositorio ni en el `.env`.

---

## 5. Instalación paso a paso

### Opción A — Servidor propio con Docker Compose

**A1. Clonar el repositorio**
```bash
git clone https://github.com/<usuario-github>/CampusFix.git
cd CampusFix
```
Resultado esperado: una carpeta `CampusFix` con `Dockerfile`, `docker-compose.yml` y `.env.example`.

**A2. Crear el archivo de configuración**
```bash
cp .env.example .env
```
Abran `.env` y cambien la línea de `JDBC_URL` por:
```
JDBC_URL=jdbc:h2:file:/data/campusfix
```
Ajusten `CORREO_TI` y `CORREO_MANTENIMIENTO` a los correos reales.

**A3. Construir y arrancar**
```bash
docker compose up -d --build
```
Resultado esperado: termina con `Container campusfix  Started`. La primera vez tarda 2–4 minutos porque descarga Maven y compila.

Si prefieren usar la imagen ya construida por el pipeline en vez de compilar, en `docker-compose.yml` cambien `build: .` e `image: campusfix:dev` por:
```yaml
    image: ghcr.io/<usuario-github>/campusfix:v1.0.0
```
y corran `docker compose up -d`.

**A4. Esperar a que quede sano**
```bash
docker compose ps
```
Resultado esperado: en la columna `STATUS` dice `Up ... (healthy)`. Puede tardar hasta 30 segundos (`start_period` del healthcheck).

**A5. Datos iniciales y primer usuario administrador**
No hay que cargar nada: el catálogo de objetos con QR viene incluido en la aplicación (6 objetos de ejemplo: `PROY-B302`, `PC-C101-01`, `WIFI-A2`, `PUERTA-A201`, `LUZ-A201`, `BANO-B1`). La versión 1.0 **no tiene usuarios ni inicio de sesión**, así que no hay administrador que crear; ver la sección 9.

**A6. Programar la revisión de plazos**
La revisión que escala lo vencido no corre sola. Agreguen una tarea programada en el servidor (`crontab -e`):
```
*/15 * * * * curl -fsS -X POST http://localhost:8080/seguimiento/revisar > /dev/null
```
Resultado esperado: cada 15 minutos se llama a la revisión. Se comprueba en la sección 6.

### Opción B — Render + pipeline completo

**B1. Copiar el repositorio a la cuenta de la universidad**
En GitHub, abran `https://github.com/Jcstevez/CampusFix` y den **Fork** (o importen el repositorio con **New → Import repository**). Quedará en `https://github.com/<usuario-github>/CampusFix`.

**B2. Activar GitHub Actions**
En el repositorio nuevo: **Actions → I understand my workflows, go ahead and enable them**.

**B3. Publicar la primera imagen**
En **Actions → CD → Run workflow → Run workflow**. El job `7a · Publicar imagen en GHCR` termina en verde; el job `7b` fallará porque todavía no existe el secreto. Es esperado.
Resultado esperado: en la página del repositorio, a la derecha, aparece **Packages → campusfix**.

**B4. Hacer público el paquete**
**Packages → campusfix → Package settings → Change visibility → Public**. Así Render puede descargar la imagen sin credenciales.

**B5. Crear el servicio en Render**
En Render: **New → Web Service → Existing Image**.
- Image URL: `ghcr.io/<usuario-github>/campusfix:<SHA>` (el SHA del commit que publicó el paso B3; aparece en **Packages → campusfix**).
- Instance type: `Starter` (o `Free` para probar).
- **Environment**: agreguen `JDBC_URL=jdbc:h2:file:/data/campusfix`, `CORREO_TI` y `CORREO_MANTENIMIENTO`.
- **Disks** (solo planes pagos): **Add Disk**, mount path `/data`, tamaño 1 GB.
- Health check path: `/salud`.

Resultado esperado: en 2–3 minutos el servicio queda **Live** y muestra su URL `https://<nombre>.onrender.com`.

**B6. Copiar el Deploy Hook**
Render → el servicio → **Settings → Deploy Hook → Copy**.

**B7. Crear el Environment y sus secretos en GitHub**
**Settings → Environments → New environment → `production`**.
- **Add environment secret**: nombre `RENDER_DEPLOY_HOOK_URL`, valor el Deploy Hook del paso B6.
- **Add environment variable**: nombre `APP_URL`, valor la URL del paso B5, **sin** `/` al final.

**B8. Proteger `main`**
**Settings → Rules → Rulesets → New branch ruleset** sobre `main`: activar **Require a pull request before merging** (1 aprobación), **Require status checks to pass** (agregar los 8 checks del CI) y **Block force pushes**.

**B9. Primer despliegue completo**
**Actions → CD → Run workflow**. Resultado esperado: los dos jobs en verde. El job `7b` muestra el humo (`pantalla 200`, `revisar 200`) y el resumen de k6 con todos los umbrales cumplidos.

**B10. Revisión de plazos programada**
Render tiene **Cron Jobs**. Creen uno con el comando:
```
curl -fsS -X POST https://<nombre>.onrender.com/seguimiento/revisar
```
y horario `*/15 * * * *`. Cuesta un mínimo de USD 1 al mes.

---

## 6. Verificación

**Healthcheck**
```bash
curl -s http://<servidor>:8080/salud
```
Resultado esperado: `{"estado":"ok"}`.

**Prueba de humo** (la misma que corre el pipeline)
```bash
U=http://<servidor>:8080
curl -fsS -o /dev/null -w "pantalla %{http_code}\n" "$U/"
curl -fsS -X POST "$U/seguimiento/revisar" -o /dev/null -w "revisar %{http_code}\n"
```
Resultado esperado: `pantalla 200` y `revisar 200`.

**Recorrido corto por el flujo principal**
1. Reportar con texto:
   ```bash
   curl -s -X POST http://<servidor>:8080/incidencias -H "Content-Type: application/json" \
     -d '{"titulo":"Proyector no funciona","descripcion":"No enciende","ubicacion":"Bloque C - Laboratorio 101"}'
   ```
   Resultado esperado: un JSON con `"tipo":"TECNOLOGIA"`, una `prioridad` y `"estado":"ABIERTA"`.
2. Reportar con QR:
   ```bash
   curl -s -X POST http://<servidor>:8080/incidencias/qr -H "Content-Type: application/json" \
     -d '{"qr":"campusfix://objeto/PROY-B302","descripcion":"No enciende"}'
   ```
   Resultado esperado: un JSON con la ubicación del proyector (`Bloque B - Aula 302`).
3. Ver los avisos generados:
   ```bash
   curl -s http://<servidor>:8080/notificaciones
   ```
   Resultado esperado: una lista con el correo a TI y la alerta del objeto.
4. Abrir `http://<servidor>:8080/` en el navegador: aparece la pantalla «CampusFix · Reporte de daños» con los QR.

**Persistencia (opción A)**
```bash
docker compose restart app
curl -s http://<servidor>:8080/incidencias
```
Resultado esperado: siguen apareciendo las incidencias del paso anterior. Si la lista sale vacía, `JDBC_URL` sigue en memoria (ver sección 8).

---

## 7. Operación

### 7.1 Publicar una nueva versión

**Opción B (automático):** todo cambio entra por pull request a `main`. Cuando el CI de `main` termina en verde, el CD publica la imagen `ghcr.io/<usuario-github>/campusfix:<SHA>` y la despliega sola. Para marcar una versión:
```bash
git switch main && git pull
git tag v1.1.0
git push origin v1.1.0
```
Resultado esperado: el workflow **Release** publica la imagen `:v1.1.0` y crea el release en GitHub.

**Opción A:** cambien la etiqueta de la imagen en `docker-compose.yml` (por ejemplo `:v1.1.0`) y corran:
```bash
docker compose pull && docker compose up -d
```

### 7.2 Volver a la versión anterior

**Opción B:** **Actions → Rollback → Run workflow**, y en `version` escriban el SHA completo o la versión anterior (`v1.0.0`). Desde la consola:
```bash
gh workflow run Rollback -f version=v1.0.0
```
Resultado esperado: el job termina en verde cuando `/salud` vuelve a responder. Comprueben en Render → **Events** que la imagen desplegada es la del SHA o versión indicada.

**Opción A:** pongan en `docker-compose.yml` la etiqueta anterior y corran `docker compose up -d`.

### 7.3 Copia de seguridad y restauración (opción A)

El nombre del volumen lleva delante el nombre de la carpeta. Compruébenlo con `docker volume ls`; si la carpeta se llama `CampusFix`, el volumen es `campusfix_campusfix-data`.

**Hacer la copia**
```bash
docker compose stop app
docker run --rm -v campusfix_campusfix-data:/data -v "$PWD":/respaldo alpine \
  tar czf /respaldo/campusfix-$(date +%F).tgz -C /data .
docker compose start app
```
Resultado esperado: un archivo `campusfix-AAAA-MM-DD.tgz` en la carpeta actual. Detener la app unos segundos evita copiar la base a mitad de una escritura.

**Restaurar**
```bash
docker compose stop app
docker run --rm -v campusfix_campusfix-data:/data -v "$PWD":/respaldo alpine \
  sh -c "rm -rf /data/* && tar xzf /respaldo/campusfix-AAAA-MM-DD.tgz -C /data"
docker compose start app
```
Resultado esperado: después de `docker compose ps` en `healthy`, `GET /incidencias` devuelve los datos de la copia.

Programen la copia diaria con `cron` y guarden los archivos fuera del servidor.

**Opción B:** con disco persistente en Render, las copias se hacen desde el panel (**Disks → Snapshots**). En el plan Free no hay disco y los datos se pierden en cada despliegue.

### 7.4 Cambiar un secreto (Deploy Hook)

1. Render → el servicio → **Settings → Deploy Hook → Regenerate**. El hook anterior deja de funcionar.
2. GitHub → **Settings → Environments → production → `RENDER_DEPLOY_HOOK_URL` → Update** y peguen el nuevo valor.
3. Comprueben con **Actions → CD → Run workflow**: el job `7b` debe terminar en verde.

Para cambiar una variable de la app: en la opción A editen `.env` y corran `docker compose up -d` (Compose recrea el contenedor); en la opción B cámbienla en Render → **Environment** (Render reinicia solo).

### 7.5 Consultar los logs

- Opción A: `docker compose logs -f app` (Ctrl+C para salir). Para las últimas 200 líneas: `docker compose logs --tail 200 app`.
- Opción B: Render → el servicio → **Logs**.
- Pipeline: GitHub → **Actions** → la ejecución → el job. Los reportes de pruebas, cobertura, seguridad y k6 están en **Artifacts** al final de cada ejecución.

### 7.6 Reiniciar el servicio

- Opción A: `docker compose restart app`.
- Opción B: Render → **Manual Deploy → Restart service**.

---

## 8. Solución de problemas

| Síntoma | Causa | Cómo resolverlo |
|---|---|---|
| `docker compose ps` muestra `unhealthy` | La app no arrancó: falta memoria o `PORT` no coincide con el puerto publicado | `docker compose logs app`. Si dice `OutOfMemoryError`, den al menos 1 GB de RAM. Revisen que `PORT` sea el mismo en `.env` y en los `ports` |
| `Bind for 0.0.0.0:8080 failed: port is already allocated` | Otro servicio usa el 8080 | Cambien `PORT=8081` en `.env` y corran `docker compose up -d` |
| Las incidencias desaparecen al reiniciar | `JDBC_URL` sigue en memoria (`jdbc:h2:mem:...`) | Pongan `JDBC_URL=jdbc:h2:file:/data/campusfix` en `.env` y corran `docker compose up -d` |
| `NumberFormatException` en los logs al arrancar | `PORT` tiene un valor que no es número | Corrijan `PORT` en `.env` |
| `pull access denied for ghcr.io/...` | El paquete en GHCR es privado | Háganlo público (paso B4) o corran `docker login ghcr.io -u <usuario-github>` con un token que tenga permiso `read:packages` |
| El job `7b · Desplegar` falla en «Desplegar» con `curl: (22) ... 404` | `RENDER_DEPLOY_HOOK_URL` está vacío, mal copiado o fue regenerado | Vuelvan a copiarlo desde Render (paso B6) al Environment `production` |
| El job `7b` falla en «Esperar a que el servicio responda» | `APP_URL` está mal (termina en `/` o apunta a otro servicio), o Render tardó más de 400 s en despertar | Corrijan `APP_URL` sin `/` final. En el plan Free, vuelvan a correr el CD; en Starter no se duerme |
| k6 falla con `thresholds ... have been crossed` | La latencia o los errores superan el SLO (p95 ≥ 500 ms o errores ≥ 1 %) | Revisen el artefacto `reporte-k6-despliegue`. En el plan Free suele ser el arranque en frío: corran el CD otra vez. Si se repite, suban de plan |
| La página tarda casi un minuto en abrir | Render Free durmió el servicio tras 15 minutos sin uso | Es esperado en Free. Usen Starter o un ping periódico a `/salud` |
| Los correos no llegan | En la versión 1.0 los avisos son simulados: quedan en `GET /notificaciones` | Es esperado. Ver la sección 9 |

---

## 9. Recomendaciones para producción

Lo que debe cambiar respecto al entorno del proyecto:

1. **Autenticación y roles.** Integrar el inicio de sesión de la universidad (SSO) y restringir `/incidencias/{id}/atender`, `/resolver` y `/seguimiento/revisar` al personal. Hoy es un riesgo aceptado (amenazas 1 y 2 de [docs/seguridad.md](seguridad.md)).
2. **HTTPS.** En la opción A, poner un proxy inverso con certificado delante del puerto 8080 y no exponer el 8080 directamente.
3. **Base de datos.** Para más de un servidor o más volumen, pasar a PostgreSQL gestionado con pool de conexiones. Gracias a la arquitectura hexagonal solo cambia el adaptador `IncidenciaRepositorioJdbc`.
4. **Copias de seguridad programadas** (sección 7.3), guardadas fuera del servidor y con una restauración probada al menos una vez por semestre.
5. **Recursos.** Starter (512 MB) alcanza para el piloto. Si `docker stats` o Render muestran memoria por encima del 80 %, pasen a 1–2 GB.
6. **Correo real.** Crear un adaptador SMTP detrás del puerto `Notificador`; el dominio no cambia.
7. **Revisión de plazos automática** con el cron de los pasos A6 o B10.
8. **Mantener el pipeline.** No desactivar checks ni bajar umbrales. Revisar cada semana los PR de Dependabot.

Riesgos aceptados que siguen abiertos (detalle en [docs/seguridad.md](seguridad.md)): sin autenticación ni autorización, correo simulado y datos efímeros en el plan gratis de Render.
