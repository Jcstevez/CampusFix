# Factory Method - CampusFix

## 1. Problema

CampusFix necesita permitir la creación de diferentes tipos de incidencias dentro del campus universitario.

Actualmente se contemplan, entre otros posibles tipos, las incidencias tecnológicas y las incidencias de infraestructura.

El problema de diseño consiste en evitar que el código cliente tenga que conocer directamente qué clase concreta debe instanciar para cada tipo de incidencia.

---

## 2. Patrón seleccionado

Para resolver este problema se utiliza el patrón de diseño creacional **Factory Method**.

El patrón permite definir un método para crear incidencias en un creador abstracto, dejando que los creadores concretos determinen qué producto concreto se instancia.

En CampusFix:

- `IncidenciaCreator` representa el Creator.
- `TecnologiaCreator` representa un Concrete Creator.
- `InfraestructuraCreator` representa un Concrete Creator.
- `Incidencia` representa el producto abstracto.
- `IncidenciaTecnologica` representa un Concrete Product.
- `IncidenciaInfraestructura` representa un Concrete Product.

---

## 3. Estructura

```text
			 <<abstract>>
		    IncidenciaCreator
			   |
		    crearIncidencia()
			   △
		 +---------+---------+
		 |                   |
		 |                   |
	TecnologiaCreator    InfraestructuraCreator
		 |                   |
		 |                   |
		 v                   v
   IncidenciaTecnologica   IncidenciaInfraestructura
		 △                   △
		 |                   |
		 +---------+---------+
			   |
		    <<abstract>>
		       Incidencia
```

### Consumidor del Factory Method

El patrón es utilizado por `IncidenciaService`, ubicado en el paquete `application`.

El servicio recibe una instancia de `IncidenciaCreator` mediante la abstracción y delega en ella la creación de la incidencia.

```text
IncidenciaService
	|
	| depende de
	v
IncidenciaCreator
	^
	|
 +-----+------------------+
 |                        |
 |                        |
TecnologiaCreator   InfraestructuraCreator
```

El servicio no necesita conocer directamente qué implementación concreta se utiliza para crear la incidencia.

---

## 4. Creator

La clase `IncidenciaCreator` define el Factory Method:

```java
public abstract class IncidenciaCreator {

    public abstract Incidencia crearIncidencia(
	    String id,
	    String titulo,
	    String descripcion,
	    String ubicacion,
	    Prioridad prioridad);
}
```

El creador define el contrato de creación, pero no decide directamente qué producto concreto será construido.

---

## 5. Concrete Creators

### TecnologiaCreator

`TecnologiaCreator` implementa el Factory Method y crea una instancia de `IncidenciaTecnologica`.

```java
@Override
public Incidencia crearIncidencia(
	String id,
	String titulo,
	String descripcion,
	String ubicacion,
	Prioridad prioridad) {

    return new IncidenciaTecnologica(
	    id,
	    titulo,
	    descripcion,
	    ubicacion,
	    prioridad);
}
```

### InfraestructuraCreator

`InfraestructuraCreator` implementa el mismo Factory Method, pero crea una instancia de `IncidenciaInfraestructura`.

```java
@Override
public Incidencia crearIncidencia(
	String id,
	String titulo,
	String descripcion,
	String ubicacion,
	Prioridad prioridad) {

    return new IncidenciaInfraestructura(
	    id,
	    titulo,
	    descripcion,
	    ubicacion,
	    prioridad);
}
```

---

## 6. Productos

La clase abstracta `Incidencia` contiene los datos comunes de una incidencia:

- Identificador.
- Título.
- Descripción.
- Ubicación.
- Prioridad.

Los atributos son privados y finales para mantener el encapsulamiento y evitar modificaciones arbitrarias después de la construcción.

Las clases concretas son:

- `IncidenciaTecnologica`
- `IncidenciaInfraestructura`

Ambas heredan de `Incidencia`.

---

## 7. Relación con SOLID

### Single Responsibility Principle

`Incidencia` representa los datos comunes de una incidencia.

Los creadores se encargan de la creación de los objetos.

No se mezclan en estas clases responsabilidades de persistencia, comunicación, notificación o presentación.

### Open/Closed Principle

El diseño permite agregar nuevos tipos de incidencias mediante nuevos Concrete Creators y Concrete Products.

Por ejemplo, para incorporar una nueva categoría podría crearse:

```
NuevoCreator
    |
    v
NuevoTipoDeIncidencia
```

sin modificar `TecnologiaCreator` ni `InfraestructuraCreator`.

### Dependency Inversion Principle

`IncidenciaService` depende de la abstracción `IncidenciaCreator` y no de los creadores concretos.

Esto permite que el servicio trabaje con diferentes implementaciones de `IncidenciaCreator`, como `TecnologiaCreator` e `InfraestructuraCreator`, sin modificar su código.

Además, el Factory Method utiliza `Incidencia` como abstracción del producto retornado.

De esta forma se reducen las dependencias directas entre el componente de aplicación y las implementaciones concretas de creación.

---

## 8. Aplicación de TDD

La implementación se desarrolló mediante ciclos de Test-Driven Development.

### Incidencia tecnológica

Se creó inicialmente una prueba que esperaba que `TecnologiaCreator` produjera una `IncidenciaTecnologica`.

Después se implementó el código mínimo necesario para que la prueba pasara.

Posteriormente se incorporaron los datos básicos de la incidencia.

### Incidencia de infraestructura

Se creó una prueba para `InfraestructuraCreator` antes de implementar las clases correspondientes.

El proyecto inicialmente falló durante la compilación porque las clases todavía no existían.

Posteriormente se implementaron:

- `IncidenciaInfraestructura`
- `InfraestructuraCreator`

y las pruebas pasaron correctamente.

---

## 9. Evidencia actual de pruebas

Actualmente existen tres clases de prueba:

```
TecnologiaCreatorTest
InfraestructuraCreatorTest
IncidenciaServiceTest
```

Cada clase contiene dos pruebas:

1. Verificación del tipo concreto creado.
2. Verificación de los datos de la incidencia.

Estado actual:

```
Tests run: 4
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

### Pruebas del servicio

`IncidenciaServiceTest` verifica:

1. El registro de una incidencia tecnológica utilizando `TecnologiaCreator`.
2. El registro de una incidencia utilizando la abstracción `IncidenciaCreator`.

Estas pruebas permiten verificar que el servicio puede trabajar con la abstracción del creador y no necesita depender directamente de una implementación concreta.

---

## 10. Extensibilidad

El diseño permite incorporar nuevos tipos de incidencia mediante la creación de nuevos Concrete Creators y Concrete Products.

Por ejemplo:

```text
IncidenciaCreator
       |
       +-- TecnologiaCreator
       |       |
       |       +-- IncidenciaTecnologica
       |
       +-- InfraestructuraCreator
		 |
		 +-- IncidenciaInfraestructura
```

Una futura categoría podría incorporarse siguiendo la misma estructura sin modificar los creadores existentes.

---

## 11. Beneficios

La aplicación de Factory Method en CampusFix permite:

- Encapsular la creación de incidencias.
- Reducir el acoplamiento entre el cliente y las clases concretas.
- Facilitar la incorporación de nuevos tipos de incidencia.
- Mantener responsabilidades separadas.
- Probar cada Concrete Creator de forma independiente.

---

