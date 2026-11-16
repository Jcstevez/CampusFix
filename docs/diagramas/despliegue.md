# Vista de despliegue (C4)

```mermaid
flowchart LR
  dev[Desarrollador] -->|PR| gh[GitHub · Actions CI]
  gh -->|merge a main| cd[GitHub Actions CD]
  cd -->|push imagen :SHA| ghcr[(GHCR)]
  cd -->|Deploy Hook + imgURL| render
  subgraph render[Render · Web Service Docker]
    c[Contenedor CampusFix\nJVM 21 · usuario no root · :PORT]
    v[(/data · H2 (efímero en plan gratis))]
    c --- v
  end
  ghcr -->|pull| c
  user[Usuario del campus] -->|HTTPS| c
  cd -->|humo + k6| c
```
Nodos: GitHub (CI/CD), GHCR (registro), Render (ejecución), contenedor `campusfix` con healthcheck `/salud`.
