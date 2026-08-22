# Adapter - CampusFix

## 1. Problema

CampusFix necesita comunicarse con un sistema externo encargado de gestionar solicitudes de mantenimiento.

El dominio de CampusFix trabaja con objetos `Incidencia`, mientras que el sistema externo expone una operación orientada a tickets:

`crearTicket(ticketId, asunto, detalle, lugar, nivel)`.

Estas interfaces representan modelos diferentes.

El problema consiste en permitir que CampusFix utilice el sistema externo sin modificar las clases del dominio para adaptarlas directamente a los detalles del sistema externo.

---

## 2. Patrón seleccionado

Para resolver este problema se utiliza el patrón de diseño estructural **Adapter**.

El Adapter permite que una clase con una interfaz incompatible pueda ser utilizada desde el contexto que necesita otra interfaz.

En CampusFix:

- `MantenimientoAdapter` representa el Adapter.
- `ExternalMaintenanceClient` representa el sistema externo o Adaptee.
- `Incidencia` representa el modelo utilizado por CampusFix.
- `MantenimientoAdapter` traduce los datos de `Incidencia` al formato requerido por `ExternalMaintenanceClient`.

---

## 3. Estructura

```text
                 CampusFix
                    |
                    | Incidencia
                    v
          MantenimientoAdapter
                    |
                    | adapta
                    v
       ExternalMaintenanceClient
                    |
                    v
          Sistema externo
```

El flujo de adaptación es:

```
Incidencia
    |
    +-- id          -> ticketId
    +-- titulo      -> asunto
    +-- descripcion -> detalle
    +-- ubicacion   -> lugar
    +-- prioridad   -> nivel
```

---

## 4. Adaptee

`ExternalMaintenanceClient` representa el componente externo que CampusFix necesita utilizar.

Su operación principal es:

```
public void crearTicket(
        String ticketId,
        String asunto,
        String detalle,
        String lugar,
        String nivel)
```

Esta interfaz utiliza nombres y conceptos propios del sistema externo.

CampusFix, en cambio, trabaja con `Incidencia`.

---

## 5. Adapter

`MantenimientoAdapter` recibe un `ExternalMaintenanceClient` y expone una operación orientada al dominio:

```
public void enviarIncidencia(Incidencia incidencia)
```

Internamente realiza la traducción:

```
clienteExterno.crearTicket(
        incidencia.getId(),
        incidencia.getTitulo(),
        incidencia.getDescripcion(),
        incidencia.getUbicacion(),
        incidencia.getPrioridad().name()
);
```

De esta forma, el código que trabaja con CampusFix no necesita conocer los detalles de la interfaz externa.

---

## 6. Responsabilidades

### MantenimientoAdapter

Su responsabilidad es adaptar la información de CampusFix al contrato esperado por el sistema externo.

No se encarga de:

- Persistencia.
- Reglas de negocio.
- Creación de incidencias.
- Presentación.
- Pruebas.

### ExternalMaintenanceClient

Representa el componente externo y recibe los datos en el formato que dicho sistema requiere.

### Incidencia

Continúa perteneciendo al dominio de CampusFix y no necesita conocer detalles del sistema externo.

---

## 7. Relación con SOLID

### Single Responsibility Principle

`MantenimientoAdapter` tiene una responsabilidad principal: traducir la interfaz de CampusFix a la interfaz del sistema externo.

La lógica de negocio de una incidencia permanece fuera del Adapter.

### Dependency Inversion Principle

El Adapter recibe el `ExternalMaintenanceClient` mediante el constructor:

```
public MantenimientoAdapter(
        ExternalMaintenanceClient clienteExterno)
```

Esto evita crear directamente el cliente externo dentro del método `enviarIncidencia`.

La dependencia se proporciona desde afuera.

### Open/Closed Principle

La existencia del Adapter permite mantener separadas las clases del dominio de los detalles de integración.

Si posteriormente cambia la integración externa, la adaptación puede modificarse sin cambiar la estructura de `Incidencia`.

---

## 8. Aplicación de TDD

La implementación se realizó mediante ciclos de Test-Driven Development.

### Ciclo 1 - Creación del Adapter

Inicialmente se creó una prueba para verificar que una incidencia pudiera ser enviada al sistema externo.

La prueba falló porque todavía no existían:

- `ExternalMaintenanceClient`
- `MantenimientoAdapter`

Posteriormente se implementaron ambas clases.

Resultado:

```
Tests run: 7
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

### Ciclo 2 - Adaptación de todos los datos

Se agregó una segunda prueba para verificar que todos los datos de la incidencia fueran traducidos correctamente:

- Identificador.
- Título.
- Descripción.
- Ubicación.
- Prioridad.

Inicialmente la prueba falló porque `ExternalMaintenanceClient` no almacenaba los datos adicionales.

Posteriormente se implementaron los atributos y métodos necesarios.

Resultado:

```
Tests run: 8
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

---

## 9. Evidencia actual de pruebas

La clase:

```
MantenimientoAdapterTest
```

contiene actualmente dos pruebas.

### Prueba 1

Verifica que el identificador de una incidencia llegue correctamente al cliente externo.

### Prueba 2

Verifica que todos los datos sean adaptados correctamente:

```
id          -> ticketId
titulo      -> asunto
descripcion -> detalle
ubicacion   -> lugar
prioridad   -> nivel
```

Estado actual:

```
Tests run: 8
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

Las seis pruebas existentes del resto del proyecto también continúan pasando.

---

## 10. Beneficios

La utilización de Adapter en CampusFix permite:

- Integrar un sistema externo sin modificar el dominio.
- Encapsular la traducción entre interfaces.
- Reducir el acoplamiento con el sistema externo.
- Mantener separadas las responsabilidades.
- Facilitar el reemplazo o evolución de la integración.
- Probar la adaptación de forma independiente.

---

## 11. Flujo completo

```
                 CampusFix

              Incidencia
                   |
                   v
        MantenimientoAdapter
                   |
          transformación
                   |
                   v
     ExternalMaintenanceClient
                   |
                   v
          Sistema externo
```

El Adapter funciona como frontera entre el modelo interno de CampusFix y el contrato del sistema externo.

```
---

## 3. Guarda el archivo

En VS Code:

**Ctrl + S**

Luego en PowerShell ejecuta:

```powershell
git status
```

Deberías ver algo parecido a:

```
Untracked files:
    src/main/java/com/campusfix/adapter/
    src/test/java/com/campusfix/adapter/
    docs/adapter.md
```

Después **no hagas commit todavía**.
