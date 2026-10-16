# =============================================================================
# Scafftrix - imagen de produccion
#
# Todo lo necesario para compilar y ejecutar el portal vive dentro de la
# imagen, asi que en la maquina del desarrollador solo hace falta Docker.
#
# Dos etapas:
#   build   -> compila el JAR con Maven (no existe mvnw en el repositorio)
#   runtime -> JRE. Sin compilador, sin Maven, sin codigo fuente.
#
# La imagen no lleva Node ni el CLI de JHipster. Se retiró porque ningun
# perfil lo usaba: los dos perfiles (SPRING_BOOT y SPRING_BATCH) se generan
# copiando las plantillas que viajan en el propio JAR. El badge "Motor de
# generacion" del dashboard tambien se elimino por lo mismo: sondeaba un
# binario que la aplicacion nunca invoca.
# =============================================================================

# ---------- build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# pom.xml primero: mientras no cambie, la capa de dependencias se reutiliza.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests clean package \
 && cp target/scafftrix-*.jar /build/app.jar


# ---------- runtime ----------
FROM eclipse-temurin:17-jre

ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    HOME=/home/factory \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"

# curl: solo se usa para el healthcheck del contenedor.
# zip:  no se instala a proposito; la creacion de artefactos la hace Java
#       (WorkspaceManager), no el sistema.
RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends ca-certificates curl; \
    rm -rf /var/lib/apt/lists/*; \
    groupadd --system factory \
 && useradd --system --gid factory --home-dir /home/factory --create-home factory

WORKDIR /app
COPY --from=build /build/app.jar /app/app.jar
COPY docker/entrypoint.sh /usr/local/bin/scafftrix-entrypoint.sh

# Los tres directorios con estado se crean en la imagen para que hereden el
# propietario correcto: los volumenes con nombre heredan la imagen, los bind
# mounts los arregla el entrypoint en el arranque.
RUN chmod 0755 /usr/local/bin/scafftrix-entrypoint.sh \
 && mkdir -p /opt/scafftrix/workspace /app/data /app/logs \
 && chown -R factory:factory /opt/scafftrix /app /home/factory

EXPOSE 8090

ENTRYPOINT ["/usr/local/bin/scafftrix-entrypoint.sh"]
CMD ["java", "-jar", "/app/app.jar"]