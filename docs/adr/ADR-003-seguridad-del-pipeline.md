# ADR-003 — Estrategia de seguridad del pipeline

**Estado:** aceptada · **Fecha:** 2026-11-16 · **Contexto:** Corte 3

## Contexto
Hay que detectar vulnerabilidades en código, dependencias, secretos e imagen **antes** de desplegar, de forma automática y sin costo, y que las herramientas mismas no sean un riesgo para la cadena de suministro.

## Decisión
| Compuerta | Herramienta | Umbral que bloquea |
|---|---|---|
| SAST | Semgrep (reglas `p/java`, `p/owasp-top-ten`), imagen fijada `1.97.0` | severidad ERROR |
| SCA | Grype (`anchore/scan-action`) sobre las dependencias reales empaquetadas | alta/crítica con parche |
| Secretos | gitleaks (binario descargado con verificación SHA-256) sobre el historial | cualquier hallazgo |
| Imagen | Grype sobre la imagen construida | crítica con parche |

* **No se usa Trivy** (su acción de GitHub sufrió un compromiso de cadena de suministro en marzo de 2026); se prefieren herramientas alternativas equivalentes. El binario de gitleaks se verifica por checksum y las acciones de terceros se mantienen al día con Dependabot.
* Permisos mínimos: `permissions: contents: read` por defecto; `packages: write` solo en publicación.
* Secretos solo en GitHub Secrets/Environments; el Environment `production` limita su uso.
* Dependabot (Maven, Actions, Docker) abre PRs semanales de actualización.

## Consecuencias
* (+) Cuatro compuertas independientes, cada una con reporte SARIF como artifact.
* (+) `only-fixed` evita bloquear por CVEs sin solución disponible (se registran como riesgo aceptado en `docs/seguridad.md`).
* (−) Posibles falsos positivos de SAST: se documentan en `docs/seguridad.md` con justificación.
* (−) Semgrep descarga reglas del registro público (requiere red en el job).
