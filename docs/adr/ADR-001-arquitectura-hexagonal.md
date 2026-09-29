# ADR-001: Arquitectura hexagonal (puertos y adaptadores) para CampusFix

- **Estado:** Aceptada
- **Fecha:** 2026-09-28
- **Contexto del curso:** Diseño y Arquitectura de Software, Corte 2

## Contexto

En el Corte 1 CampusFix quedó con un dominio (`Incidencia`) y cuatro patrones
(Factory Method, Strategy, Adapter, Observer). CampusFix será una función
nueva dentro de la App Sabana ("Reportar alerta"). Los tres retos asignados:

1. **Que los reportes no queden en el olvido**: alertas sobre los objetos
   reportados y correos al área de tecnología.
2. **Clasificar automáticamente** (tecnología / infraestructura) y decidir la
   prioridad.
3. **Simular QR** para reportar de forma más eficiente al escanear un objeto.

Atributos de calidad que dominan: **modificabilidad** (hoy el correo y el
QR son simulados; mañana serán SMTP real, push, cámara del celular),
**testeabilidad** (hay que probar reglas de negocio sin BD ni red),
**disponibilidad del reporte** (un aviso que falla no debe perder el reporte)
y **rendimiento** moderado (picos de reportes).

## Opciones consideradas

Escala 1 (mal) a 5 (muy bien) según qué tan bien atiende cada criterio,
derivado de los retos.

| Criterio (reto que lo origina) | Capas (3 niveles) | **Hexagonal** | Orientada a eventos (broker) | Microservicios |
|---|:-:|:-:|:-:|:-:|
| Cambiar correo/QR/BD sin tocar reglas (R1, R3) | 3 | **5** | 4 | 4 |
| Probar reglas sin infraestructura (R2) | 3 | **5** | 3 | 3 |
| Un aviso que falla no pierde el reporte (R1) | 2 | **4** | 5 | 4 |
| Encaja con lo que ya existe (Adapter, Observer, Factory) | 3 | **5** | 3 | 2 |
| Proporcional al tamaño (1 módulo, 1 equipo de 4) | 5 | **4** | 3 | **1** |
| Costo operativo / complejidad de despliegue | 5 | **4** | 2 | 1 |
| **Total** | 21 | **27** | 20 | 15 |

Notas:
- **Capas** es más simple pero deja al dominio dependiendo de la capa de datos y no aísla los adaptadores; cambiar el proveedor de correo suele filtrarse hacia arriba.
- **Orientada a eventos con broker** resolvería R1 muy bien (desacople total del envío), pero exige infraestructura (Kafka/RabbitMQ) desproporcionada para un módulo pequeño. Se adopta su idea en pequeño: el patrón Observer interno ya publica eventos en proceso.
- **Microservicios** es la peor opción aquí: no hay un reto de escala independiente por componente que lo justifique; sumaría red, despliegues y consistencia distribuida sin beneficio.

## Decisión

Se adopta **arquitectura hexagonal**:

- **Núcleo** (`domain`, `factory`, `strategy`, `observer`): reglas de negocio, sin frameworks ni I/O.
- **Aplicación** (`application`): casos de uso y **puertos** (`port.in` = lo que el sistema ofrece, `port.out` = lo que necesita).
- **Adaptadores** (`adapter.in.http`, `adapter.out.persistence|notification|qr|catalog|mantenimiento`): implementan los puertos con tecnología concreta (HTTP del JDK, JDBC/H2, ZXing).
- **Bootstrap**: única clase que conoce implementaciones concretas y las conecta.

Regla de dependencia: `adapter → application → domain`, nunca al revés; los
adaptadores de entrada y de salida no se conocen entre sí. Está **verificada
automáticamente** por `ArquitecturaTest`.

## Consecuencias

**Positivas**
- El correo simulado se cambia por SMTP y el catálogo en memoria por una BD escribiendo *un adaptador nuevo*, sin tocar reglas (R1).
- El clasificador es una interfaz (`ClasificadorIncidencia`): las reglas por palabras clave se pueden reemplazar por un modelo de ML (R2).
- El QR se lee/genera detrás de puertos (`DecodificadorQr`, `GeneradorQr`); la app móvil real solo tendrá que enviar el texto del QR (R3).
- Las reglas se prueban en aislamiento y rápido; las fronteras con pruebas de integración.
- El Adapter del Corte 1 (`MantenimientoAdapter`) pasa a ser un adaptador de salida formal.

**Negativas / lo que se sacrifica**
- Más paquetes e interfaces que un diseño en capas para el tamaño actual (mapeo entre DTO/dominio en el borde HTTP).
- Los avisos se disparan **de forma síncrona** dentro de la petición (Observer en proceso): un notificador lento aumenta la latencia del reporte. Se acepta hoy porque es simulado; el límite y la mitigación (cola asíncrona) están en `docs/arquitectura.md`.
- No hay escalabilidad independiente por componente (es un monolito modular). Es intencional: ningún reto lo exige.
