<p align="center">
  <img src="src/main/resources/static/img/logo.png" alt="Scafftrix" width="96">
</p>

<h1 align="center">
  <img src="docs/scafftrix-title.png" alt="Scafftrix" width="560">
</h1>

<p align="center">
  <img alt="Java 17" src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white">
  <img alt="Spring Boot 3.4.5" src="https://img.shields.io/badge/Spring_Boot-3.4.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
  <img alt="Thymeleaf 3.1" src="https://img.shields.io/badge/Thymeleaf-3.1-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white">
  <img alt="H2 2.3.232" src="https://img.shields.io/badge/H2-2.3.232-1021FF?style=for-the-badge&logoColor=white">
  <img alt="Maven 3.9" src="https://img.shields.io/badge/Maven-3.9-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white">
  <img alt="Docker" src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white">
</p>

<p align="center">
  <img alt="JUnit 5" src="https://img.shields.io/badge/JUnit_5-25A162?style=for-the-badge&logo=junit5&logoColor=white">
  <img alt="JaCoCo 0.8.13" src="https://img.shields.io/badge/JaCoCo-0.8.13-8B0000?style=for-the-badge&logoColor=white">
  <img alt="PostgreSQL 16" src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white">
  <img alt="Flyway" src="https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white">
  <img alt="OpenAPI 3" src="https://img.shields.io/badge/OpenAPI-3-6BA539?style=for-the-badge&logo=openapiinitiative&logoColor=white">
  <img alt="Git" src="https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=git&logoColor=white">
</p>

Fábrica de Software corporativa (Scafftrix) disponible como intranet: permite generar proyectos Java estandarizados desde una sencilla interfaz web. La aplicación oculta la complejidad de la generación detrás de un formulario, impone los estándares corporativos (H2, Thymeleaf, Maven), genera el proyecto de forma asíncrona y deja descargar un ZIP listo para compilar. Cada perfil se renderiza a partir de plantillas que viajan dentro del propio JAR: no hay CLI externa, ni Node, ni JHipster.

---

## Aspectos destacados de la arquitectura

- **Límite limpio y testeable**: `ProjectGenerator` aísla la estrategia de generación. Los tres perfiles (`SPRING_BOOT`, `SPRING_BATCH`, `SPRING_MICROSERVICE`) pasan por la misma implementación probada, `TemplateProjectGenerator`, sin ningún proceso externo.
- **Seguridad primero**: ninguna entrada del usuario se usa para construir rutas del sistema de ficheros; las rutas del workspace se resuelven solo por UUID y se verifican con `startsWith`. Los ficheros JSON/texto generados se escriben en UTF-8 sin BOM.
- **Plantillas, no scaffolding**: los perfiles se renderizan desde las plantillas empaquetadas en el JAR, sustituyendo los marcadores `@token@` (`@packagePath@`, `@springBootVersion@`, `@applicationClass@`, `@description@`, ...) con valores ya validados por `NamingService`. Se usa `@` como delimitador en lugar de `${...}` para que las expresiones propias de Maven de los `pom.xml` incluidos sobrevivan intactas.
- **Generación asíncrona**: los trabajos corren en un executor dedicado; el progreso se emite por SSE; el estado terminal persiste en H2.
- **Guardia corporativa**: `CorporateStandardValidator` falla al arrancar si la configuración desplegada se desvía de H2/Thymeleaf/Maven.
- **Spring Batch**: perfil propio con su propia raíz de plantilla (`project-batch-template`), que produce un proceso real (lector, procesador, writer, listener, manejo de errores) en lugar de una aplicación web con andamiaje de batch encima.
- **Historial de proyectos**: `/projects` permite búsqueda, filtros por tipo/estado, ordenación segura y paginación limitada.
- **CRUD completo**: los proyectos se crean, se inspeccionan, se **editan** (`GET/POST /projects/{id}/edit`, `/projects/{id}`) y se **eliminan** (`POST /projects/{id}/delete`). Solo se puede editar la información descriptiva (nombre, versión, descripción); la identidad técnica (tipo, stack, identificadores) es inmutable. Editar o eliminar se rechaza con `409` mientras hay una generación en curso. Al eliminar se borran tanto la fila de la base de datos como la carpeta generada del workspace, esta última solo después de confirmar la transacción.

## Modelo de despliegue: todo dentro de Docker

Esta es la forma soportada de ejecutar la fábrica.

La compilación ocurre **dentro de la imagen**: Maven y el JDK viven en la etapa `build` y se descartan después, de modo que la imagen final solo lleva un JRE. El host necesita Docker y nada más: no se instala nada a nivel global y cada desarrollador y cada servidor de intranet ejecuta exactamente la misma versión del portal.

```bash
docker compose build     # compila el JAR dentro de la imagen (etapa build)
docker compose up -d
docker compose logs -f scafftrix
```

El portal queda disponible en [http://localhost:8090](http://localhost:8090).

Comprueba que el contenedor arrancó como healthy:

```bash
docker compose ps                # STATUS debe ser "healthy"
curl -s http://localhost:8090/actuator/health
```

Para pararlo conservando los datos:

```bash
docker compose down          # ./data, ./logs y el volumen del workspace sobreviven
```

### Qué contiene la imagen

| Etapa | Imagen base | Contenido |
|---|---|---|
| `build` | `maven:3.9-eclipse-temurin-17` | Compila el JAR. No está presente en la imagen final. |
| runtime | `eclipse-temurin:17-jre` | JRE 17 y `curl` (healthcheck). Sin compilador, sin Maven, sin código fuente, **sin Node ni CLI de JHipster**. |

La imagen no necesita ninguna herramienta externa para generar un proyecto: cada perfil se renderiza a partir de las plantillas que viajan en el JAR. Por eso bajó de ~1,29 GB a 660 MB.

### Datos persistentes

| Ruta dentro del contenedor | Montaje | Contenido |
|---|---|---|
| `/opt/scafftrix/workspace` | volumen con nombre `workspace-data` | proyectos generados, logs de generación y ZIPs descargables |
| `/app/data` | bind mount `./data` | base de datos H2 con el historial de proyectos |
| `/app/logs` | bind mount `./logs` | `scafftrix.log` |

Los dos bind mounts importan: sin ellos, `docker compose down` recreaba el contenedor y tiraba la base de datos H2, perdiendo todo el historial de proyectos.

El contenedor arranca como `root` solo para dejar esos tres directorios escribibles y después baja al usuario sin privilegios `factory` (`setpriv`). Así es como funcionan `./data` y `./logs` en un host Linux, donde Docker crea los directorios de los bind mounts como propiedad de `root`. En Docker Desktop (Windows/macOS) el paso es inocuo.

### Endurecimiento del contenedor

`docker-compose.yml` activa lo que el entrypoint solo no puede dar:

| Ajuste | Efecto |
|---|---|
| `security_opt: no-new-privileges:true` | ningún proceso puede recuperar privilegios después del descenso con `setpriv` |
| `read_only: true` | el sistema de ficheros de la imagen es inmutable; solo aceptan escritura los tres montajes de estado y `/tmp` |
| `tmpfs: /tmp` | donde la JVM coloca sus propios temporales |
| `chown` no recursivo en el entrypoint | el entrypoint solo corrige la propiedad de los puntos de montaje, y únicamente si son de root. Los ficheros de dentro los crea siempre la aplicación como `factory`, así que un `chown` recursivo nunca es necesario — y sería la parte peligrosa si un bind mount estuviera mal configurado |

Quedan dos cosas, y ambas son propiedades de Docker, no defectos de este repositorio:

- `docker exec` sobre este contenedor abre una shell como root. Quien puede ejecutar `docker exec` ya controla el daemon de Docker y por tanto el host; la mitigación real es decidir quién puede usar Docker ahí, algo que queda fuera de la imagen.
- El comando de arranque (`"$@"`) es el que declare `docker-compose.yml`. Así funciona el entrypoint de todas las imágenes oficiales; aquí los montajes y el comando quedan fijados en el repositorio.

Los proyectos generados siguen la misma regla: la plantilla del microservicio no empaqueta ninguna contraseña de base de datos y se niega a arrancar (`${DB_PASSWORD:?}` / `${POSTGRES_PASSWORD:?}`) si no se inyecta una en el momento del despliegue.

### Nunca ejecutes dos instancias contra el mismo `data/`

H2 es una base de datos embebida de un solo escritor. Si el JAR también corre en el host con `./data`, el segundo proceso falla con *La base de datos puede que ya esté siendo utilizada*. Para la instancia local antes de `docker compose up`, y viceversa.

## Ejecutar el JAR en el host (solo desarrollo)

No es el despliegue soportado, pero es útil al depurar. Requiere la cadena de herramientas en el host:

- Java 17 o superior
- Maven 3.9 o superior

No hace falta Node ni CLI de generadora: los perfiles se renderizan desde las plantillas que viajan en el JAR.

```bash
mvn clean package
java -jar target/scafftrix-1.0.0-SNAPSHOT.jar
```

Evita también `mvn spring-boot:run`: abre un JVM hijo que sobrevive al apagado y mantiene bloqueados el puerto 8090 y el fichero de H2.

## Configuración

Toda la configuración está en `application.yml` bajo `scafftrix.*`. Se puede sobreescribir con variables de entorno usando el relaxed binding de Spring, por ejemplo `SCAFFTRIX_WORKSPACE_PATH=/opt/scafftrix/workspace`.

Puntos clave:

- `scafftrix.workspace.path`: `./workspace` en el host, `/opt/scafftrix/workspace` en el contenedor (lo fija `docker-compose.yml`).
- `scafftrix.defaults.*`: solo valores por defecto de la interfaz (`group-id`, `version`, `spring-boot-version`). La tecnología fijada no está aquí: cada perfil fija su propio stack en `ProjectType`.
- `scafftrix.versions.spring-boot`: las versiones de Spring Boot que ofrece el formulario de creación, de más reciente a menos. La predeterminada es `4.1.1`; una versión fuera de esa lista se rechaza.
- `scafftrix.generation.timeout`: `PT15M` por defecto.

Solo dos decisiones técnicas las elige el usuario; todo lo demás va fijado por perfil:

| Decisión | Dónde | Se rechaza cuando |
|---|---|---|
| Versión de Spring Boot | `scafftrix.versions.spring-boot` | No está en la lista ofrecida |
| Formato de configuración | `ConfigFormat` (`.properties` / `.yml`) | — (elección libre) |

La versión colocada en `@springBootVersion@` se escribe en el `pom.xml` generado. Las versiones pre-release (`*-SNAPSHOT`, `*-M<n>`, `*-RC<n>`) añaden automáticamente los repositorios de milestones/snapshots de Spring; las versiones GA no.

## Perfiles de generación

No hay generadora externa. Los perfiles se renderizan desde las plantillas que viajan en el JAR, mediante `TemplateProjectGenerator`:

| Perfil | Raíz de plantilla | Verifica |
|---|---|---|
| `SPRING_BOOT` | `project-template` | `pom.xml`, paquete, `contract/api.yaml` |
| `SPRING_BATCH` | `project-batch-template` | `pom.xml`, paquete, `schema-all.sql`, `sample-data.csv` |
| `SPRING_MICROSERVICE` | `project-microservice-template` | `pom.xml`, paquete, `contract/api.yaml`, `Dockerfile`, `docker-compose.yml`, `db/migration` |

`SPRING_MICROSERVICE` es un microservicio contract-first: el contrato OpenAPI (`src/main/resources/contract/api.yaml`) es la fuente de verdad, las interfaces y modelos de la API se generan a partir de él y el código sigue un layout hexagonal (`domain`, `application`, `infrastructure`, `api`). Incluye migraciones Flyway, un `Dockerfile` y un `docker-compose.yml` con PostgreSQL, además de su propia suite de tests (contexto, mappers, servicios, adaptadores de persistencia, controladores).

No hay `.yo-rc.json` ni sello de `serverPort`: los campos del perfil se sustituyen en las plantillas. Los proyectos generados se compilan, no se ejecutan, así que no publican puertos; el propio portal solo escucha en el `8090`.

Cada perfil fija su propia tecnología (se muestra de solo lectura en la interfaz, no es un estándar global):

| Perfil | Base de datos | Frontend | Contract-first |
|---|---|---|---|
| `SPRING_BOOT` | H2 | Thymeleaf | Sí |
| `SPRING_BATCH` | H2 | ninguno (sin capa web) | No |
| `SPRING_MICROSERVICE` | H2 (dev) / PostgreSQL (prod) | ninguno | Sí |

El formato de configuración es seleccionable en todos los perfiles. Cada plantilla trae tanto `application.properties` como `application.yml` (con sus variantes de perfil/test) y el generador conserva solo la familia seleccionada, de modo que un proyecto generado nunca lleva dos ficheros de configuración compitiendo.

La descripción que se escribe en el formulario se copia al `<description>` del `pom.xml` generado; si se deja vacía, el generador usa la descripción por defecto del perfil.

## Tests y cobertura

```bash
mvn clean verify
```

La suite cubre las capas unitaria, slice MVC (`@WebMvcTest`) y slice de persistencia (`@DataJpaTest`), incluido el ciclo completo de CRUD (actualización/borrado, validación, `409` durante la generación). Actualmente ejecuta **228 tests**, con la verificación de cada perfil generado incluida.

`verify` ejecuta además JaCoCo y escribe el informe de cobertura en `target/site/jacoco/` (HTML para humanos, `jacoco.xml` para máquinas). La cobertura está en torno al 95%; el informativo no bloquea nunca la build.

## Notas

- Sin React/Angular/Vue. Spring MVC + Thymeleaf tradicionales.
- H2 se usa como base de datos del portal. Los proyectos generados también usan H2, salvo el perfil de microservicio, que apunta a PostgreSQL en producción (H2 solo para desarrollo y tests).
- El recorrido del workspace está bloqueado por doble comprobación (UUID + `startsWith`).
- UTF-8 sin BOM en los JSON/texto que escribe el generador.
- Los enlaces de texto deben llevar un color explícito. El nombre del proyecto en la tabla de proyectos es el caso de estudio: sin regla propia caía al azul por defecto del navegador `#0000EE`, que puntúa 1,64:1 sobre la tarjeta oscura.

## Estructura del repositorio

| Ruta | Qué es |
|---|---|
| `src/main/java/es/scafftrix/factory/` | el portal (Spring Boot + Thymeleaf) |
| `src/main/resources/project-*-template/` | las tres plantillas que la fábrica renderiza en los proyectos generados |
| `src/main/resources/static/`, `templates/` | recursos de interfaz (CSS, vistas Thymeleaf, `Angels.ttf`, logo) |
| `docs/scafftrix-title.png` | título del README, renderizado con `Angels.ttf` y el degradado corporativo de `style.css` |
| `src/test/java/` | 228 tests |
| `pom.xml` | `es.scafftrix:scafftrix`; JaCoCo es el único plugin de build además de Spring Boot |
| `Dockerfile` | multi-stage: build con Maven → runtime solo JRE |
| `docker/entrypoint.sh` | corrige los permisos de los directorios de estado y después baja al usuario sin privilegios `factory` |
| `docker-compose.yml` | el despliegue soportado (puerto 8090, volumen `workspace-data`, bind mounts `./data`, `./logs`) |
| `.dockerignore`, `.gitignore` | mantienen fuera de la imagen y del repo el estado de build, los ficheros H2 y los logs |
