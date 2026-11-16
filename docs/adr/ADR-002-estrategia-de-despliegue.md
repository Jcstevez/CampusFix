# ADR-002 — Estrategia de despliegue de CampusFix

**Estado:** aceptada · **Fecha:** 2026-11-16 · **Contexto:** Corte 3

## Contexto
CampusFix es un único contenedor (JVM + H2). El equipo es de 4 estudiantes, sin presupuesto, y necesita despliegue **automático**, con **URL pública**, **rollback** y evidencia verificable en GitHub Actions.

## Opciones consideradas
| Opción | Ventajas | Desventajas |
|---|---|---|
| **A. Render (imagen de GHCR) vía Deploy Hook** | Gratis, URL HTTPS, despliega imágenes por tag, rollback = redesplegar un tag, sin gestionar servidores | El plan gratis se duerme tras inactividad (hay que esperar `/salud`); disco efímero |
| B. VM propia + SSH + docker compose | Control total, disco persistente | Hay que operar y parchear el servidor, guardar llave SSH, costo |
| C. Kubernetes (kind/minikube en el runner) | Cumple bonus K8s | No es un entorno real accesible; complejidad alta para el plazo |
| D. Heroku/Fly.io | Similares a A | Requieren tarjeta o cuenta de pago |

## Decisión
Opción **A**. Estrategia de liberación: **reemplazo directo (recreate)** de la única instancia, protegida por: humo + k6 tras el deploy, y **rollback** re-desplegando la imagen anterior (tags inmutables por SHA).

## Consecuencias
* (+) Costo cero, despliegue en minutos, rollback con un clic.
* (+) Imagen inmutable ⇒ lo probado en CI es exactamente lo desplegado.
* (−) Hay una breve indisponibilidad en cada despliegue (aceptable: uso interno del campus, no 24/7 crítico).
* (−) El plan gratis duerme el servicio y el disco es efímero ⇒ con H2 los datos se pierden al redesplegar (riesgo aceptado y documentado; en producción real se usa `JDBC_URL` con volumen/disco persistente o una base gestionada — ver `docs/implementacion.md`).
* (−) Si Render cambia su plan gratuito, se migra a la opción B sin cambiar el pipeline salvo el paso de deploy.
