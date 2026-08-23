# Strategy - CampusFix

## 1. Problema

CampusFix necesita permitir que una incidencia pueda recibir diferentes tratamientos dependiendo de su tipo.

Una incidencia tecnológica requiere una atención diferente a una incidencia de infraestructura. Si esta lógica se implementara directamente mediante múltiples condiciones dentro de un servicio, el código crecería y quedaría más acoplado a las diferentes formas de atención.

El problema de diseño consiste en permitir cambiar el comportamiento de atención de una incidencia sin modificar el código que utiliza dicho comportamiento.

---

## 2. Patrón seleccionado

Para resolver este problema se utiliza el patrón de diseño comportamental **Strategy**.

Strategy permite definir una familia de comportamientos mediante una abstracción común y encapsular cada comportamiento en una estrategia concreta.

En CampusFix:

- `EstrategiaIncidencia` representa la Strategy.
- `EstrategiaTecnologica` representa una Concrete Strategy.
- `EstrategiaInfraestructura` representa una Concrete Strategy.
- `Incidencia` representa el objeto sobre el cual se ejecuta la estrategia.

---

## 3. Estructura

```text
                  <<interface>>
              EstrategiaIncidencia
                       |
              ejecutar(Incidencia)
                       △
                       |
             +---------+---------+
             |                   |
             |                   |
   EstrategiaTecnologica   EstrategiaInfraestructura
             |                   |
             |                   |
             v                   v
   "Atender incidencia    "Atender incidencia
      tecnológica"          de infraestructura"
```

Las estrategias concretas implementan el mismo contrato, pero proporcionan comportamientos diferentes.

---

## 4. Strategy

La interfaz `EstrategiaIncidencia` define el contrato común para las diferentes estrategias:

```java
public interface EstrategiaIncidencia {

    String ejecutar(Incidencia incidencia);
}
```

La estrategia recibe una `Incidencia` y devuelve la acción correspondiente.

La abstracción permite que el código cliente trabaje con `EstrategiaIncidencia` sin depender directamente de una estrategia concreta.

---

## 5. Concrete Strategies

### EstrategiaTecnologica

`EstrategiaTecnologica` implementa el comportamiento correspondiente a las incidencias tecnológicas.

```java
@Override
public String ejecutar(Incidencia incidencia) {
    return "Atender incidencia tecnológica";
}
```

Esta estrategia encapsula el comportamiento específico para una incidencia tecnológica.

### EstrategiaInfraestructura

`EstrategiaInfraestructura` implementa el comportamiento correspondiente a las incidencias de infraestructura.

```java
@Override
public String ejecutar(Incidencia incidencia) {
    return "Atender incidencia de infraestructura";
}
```

Esta estrategia permite mantener separado el comportamiento de las incidencias de infraestructura.

---

## 6. Relación con Factory Method

Strategy complementa el patrón Factory Method implementado anteriormente en CampusFix.

Factory Method se encarga de la creación de la incidencia:

```text
IncidenciaCreator
       |
       +-- TecnologiaCreator
       |       |
       |       v
       |   IncidenciaTecnologica
       |
       +-- InfraestructuraCreator
               |
               v
       IncidenciaInfraestructura
```

Strategy se encarga de definir el comportamiento o tratamiento de la incidencia:

```text
Incidencia
     |
     v
EstrategiaIncidencia
     |
     +-- EstrategiaTecnologica
     |
     +-- EstrategiaInfraestructura
```

De esta manera, ambos patrones tienen responsabilidades diferentes:

- **Factory Method** encapsula la creación.
- **Strategy** encapsula el comportamiento.

Esto evita mezclar responsabilidades dentro de una única clase.

---

## 7. Relación con SOLID

### Single Responsibility Principle

Cada estrategia tiene una única responsabilidad: definir cómo se atiende un determinado tipo de incidencia.

`EstrategiaTecnologica` se encarga del comportamiento tecnológico y `EstrategiaInfraestructura` del comportamiento de infraestructura.

### Open/Closed Principle

El diseño permite agregar nuevas estrategias sin modificar las existentes.

Por ejemplo, podría agregarse:

```text
EstrategiaIncidencia
       |
       +-- EstrategiaTecnologica
       |
       +-- EstrategiaInfraestructura
       |
       +-- EstrategiaSeguridad
```

La nueva estrategia implementaría la interfaz `EstrategiaIncidencia` sin necesidad de modificar las estrategias anteriores.

### Dependency Inversion Principle

El código cliente puede depender de la abstracción:

```text
EstrategiaIncidencia
```

en lugar de depender directamente de:

```text
EstrategiaTecnologica
```

o:

```text
EstrategiaInfraestructura
```

Esto reduce el acoplamiento entre el consumidor y las implementaciones concretas.

---

## 8. Aplicación de TDD

La implementación se desarrolló mediante ciclos de Test-Driven Development.

### Ciclo 1 - Estrategia tecnológica

Primero se creó una prueba que esperaba que una estrategia tecnológica definiera la acción:

> "Atender incidencia tecnológica"

Inicialmente las clases de estrategia no existían, por lo que la prueba no podía compilar.

Después se creó `EstrategiaIncidencia`, y posteriormente `EstrategiaTecnologica`.

Una vez implementado el comportamiento mínimo necesario, la prueba pasó correctamente.

### Ciclo 2 - Estrategia de infraestructura

Posteriormente se incorporó una segunda estrategia: `EstrategiaInfraestructura`.

Esta estrategia implementa el mismo contrato, pero devuelve el comportamiento correspondiente a infraestructura.

Se agregó una prueba específica para verificar este comportamiento.

---

## 9. Pruebas

Actualmente existen pruebas para las dos estrategias:

**`EstrategiaIncidenciaTest`**

La clase contiene dos pruebas:

1. Verificación del comportamiento de `EstrategiaTecnologica`.
2. Verificación del comportamiento de `EstrategiaInfraestructura`.

Además, el proyecto mantiene las pruebas implementadas por los integrantes anteriores.

Resultado actual de la ejecución:

```text
Tests run: 10
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

La ejecución utilizada para validar el proyecto fue:

```bash
mvn clean test
```

---

## 10. Extensibilidad

El patrón permite agregar nuevos comportamientos mediante nuevas implementaciones de `EstrategiaIncidencia`.

Por ejemplo:

```text
EstrategiaIncidencia
       |
       +-- EstrategiaTecnologica
       |
       +-- EstrategiaInfraestructura
       |
       +-- EstrategiaSeguridad
       |
       +-- EstrategiaMantenimiento
```

Cada nueva estrategia puede encapsular su propio comportamiento sin modificar las estrategias existentes.

Esto facilita la evolución de CampusFix a medida que aparecen nuevos tipos de atención.

---

## 11. Beneficios

La aplicación de Strategy en CampusFix permite:

- Encapsular comportamientos diferentes.
- Reducir el uso de condicionales para seleccionar comportamientos.
- Disminuir el acoplamiento entre el código cliente y las estrategias concretas.
- Facilitar la incorporación de nuevos comportamientos.
- Aplicar el principio Open/Closed.
- Permitir probar cada estrategia de forma independiente.
- Complementar el Factory Method utilizado para la creación de incidencias.
