# Diagramas (C4 en Mermaid — GitHub los renderiza)

## Nivel 1: contexto

```mermaid
flowchart LR
    estudiante(["Estudiante / docente<br/>reporta un daño"])
    ti(["Área de TI<br/>recibe correos"])
    mant(["Mantenimiento<br/>recibe tickets"])
    app["<b>App Sabana</b><br/>(existente)<br/>nueva función: Reportar alerta"]
    cf["<b>CampusFix</b><br/>gestiona y da seguimiento<br/>a incidencias"]
    ext["Sistema externo<br/>de mantenimiento"]
    smtp["Correo (SMTP)<br/>simulado hoy"]

    estudiante --> app
    app -- "HTTP/JSON<br/>(texto o QR escaneado)" --> cf
    cf -- "tickets (Adapter)" --> ext
    cf -- "correos / alertas" --> smtp
    smtp --> ti
    ext --> mant
```

## Nivel 2: contenedores

```mermaid
flowchart TB
    subgraph app["App Sabana (móvil) — fuera de alcance"]
        ui["Interfaz 'Reportar alerta'<br/>+ lector de QR de la cámara"]
    end
    subgraph cf["CampusFix (un proceso Java 23)"]
        api["<b>API HTTP/JSON</b><br/>ServidorHttp (JDK)"]
        core["<b>Aplicación + Dominio</b><br/>casos de uso, reglas,<br/>clasificador, patrones"]
        db[("H2 embebida<br/>tabla incidencias")]
    end
    ui -- "POST /incidencias<br/>POST /incidencias/qr" --> api
    api --> core
    core -- "JDBC" --> db
```

## Nivel 3: componentes (hexágono)

```mermaid
flowchart LR
    subgraph IN["Adaptadores de entrada"]
        http["adapter.in.http<br/>ServidorHttp"]
    end
    subgraph APP["application"]
        pin{{"port.in<br/>ReportarIncidenciaUseCase<br/>ReportarPorQrUseCase<br/>GestionarIncidenciasUseCase<br/>SeguimientoUseCase"}}
        svc["IncidenciaService<br/>ReporteQrService<br/>SeguimientoService"]
        obs["Observers:<br/>AlertaObjetoReportado<br/>CorreoTecnologia<br/>MantenimientoInfraestructura"]
        pout{{"port.out<br/>IncidenciaRepositorio · CatalogoObjetos<br/>Notificador · MantenimientoPuerto<br/>DecodificadorQr · GeneradorQr"}}
    end
    subgraph DOM["Núcleo"]
        dom["domain: Incidencia (Tecnológica / Infraestructura)<br/>ClasificadorIncidencia · CodigoQr · PoliticaSla<br/>factory (Factory Method) · strategy · observer (Subject)"]
    end
    subgraph OUT["Adaptadores de salida"]
        jdbc["persistence<br/>IncidenciaRepositorioJdbc"]
        noti["notification<br/>NotificadorSimulado (correo / app)"]
        zx["qr<br/>ZxingAdapter"]
        cat["catalog<br/>CatalogoObjetosEnMemoria"]
        man["mantenimiento<br/>MantenimientoAdapter"]
    end
    http --> pin --> svc
    svc --> dom
    svc --> obs
    svc --> pout
    obs --> pout
    pout -.implementan.-> jdbc & noti & zx & cat & man
```

## Flujo del reporte por QR (Reto 3 → 2 → 1)

```mermaid
sequenceDiagram
    participant App as App Sabana
    participant H as ServidorHttp
    participant Q as ReporteQrService
    participant C as Catálogo
    participant I as IncidenciaService
    participant R as Repositorio (H2)
    participant S as Subject (Observer)
    App->>H: POST /incidencias/qr {qr, descripcion}
    H->>Q: reportarPorContenido
    Q->>C: buscarPorId(objeto)
    Q->>Q: clasificador → prioridad
    Q->>I: registrar(tipo, prioridad, ...)
    I->>I: Factory Method crea la incidencia
    I->>R: guardar
    I->>S: notificarObservers
    S-->>S: alerta app · correo TI (si tecnología) · ticket (si infraestructura)
    H-->>App: 201 + incidencia
```

Cada cierto tiempo `POST /seguimiento/revisar` (Reto 1) escala y avisa lo que
superó su plazo según la prioridad.
