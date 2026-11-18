# syntax=docker/dockerfile:1

# Etapa 1: compilar con Maven y reunir las librerias de ejecucion
FROM maven:3.9-eclipse-temurin-26-noble AS build
WORKDIR /src
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q package -DskipTests \
 && mvn -B -ntp -q dependency:copy-dependencies -DoutputDirectory=target/lib -DincludeScope=runtime

# Etapa 2: imagen final solo con el JRE, el jar y las librerias
FROM eclipse-temurin:21-jre-noble
# Aplica los parches de seguridad del sistema operativo (glibc, perl-base; ver docs/seguridad.md)
RUN apt-get update \
 && apt-get upgrade -y --no-install-recommends \
 && rm -rf /var/lib/apt/lists/*
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build --chown=app:app /src/target/campusfix-1.0-SNAPSHOT.jar app.jar
COPY --from=build --chown=app:app /src/target/lib ./lib
RUN mkdir /data && chown app:app /data
VOLUME /data
USER app
EXPOSE 8080

# Docker consulta /salud para saber si la aplicacion esta lista
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
  CMD curl -fsS http://localhost:${PORT:-8080}/salud || exit 1

# La JVM usa como maximo el 75 % de la memoria del contenedor
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-cp", "app.jar:lib/*", "com.campusfix.bootstrap.CampusFixApp"]
