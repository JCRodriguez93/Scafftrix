# @name@

Microservicio Spring Boot generado por Scafftrix siguiendo el enfoque
**Contract-First**: el contrato OpenAPI es la fuente de verdad y el codigo de la capa
API se genera a partir de el.

## Stack

- Java 17 y Spring Boot @springBootVersion@
- Maven
- OpenAPI Generator (interfaces y modelos de la API a partir de `contract/api.yaml`)
- MapStruct (mapeo entre capas)
- Flyway (migraciones de base de datos)
- PostgreSQL en produccion y H2 en memoria para desarrollo y tests

## Arquitectura

El microservicio sigue una arquitectura hexagonal (puertos y adaptadores):

```
             contrato OpenAPI (contract/api.yaml)
                          |
                          v
  api (controllers, DTOs generados, api mappers)   <- adaptador de entrada
                          |
                          v
  application (casos de uso, puertos in/out)        <- logica de aplicacion
                          |
                          v
  domain (modelo y excepciones)                     <- nucleo sin dependencias
                          ^
                          |
  infrastructure (JPA, adaptadores, persistence mappers)  <- adaptador de salida
```

Regla de dependencias: `domain` no depende de nadie; `application` depende de `domain`;
`api` e `infrastructure` dependen de `application` y `domain`. El modelo de dominio usa
`record` inmutables; las entidades JPA viven solo en `infrastructure`.

## Como ejecutar

Arrancar en local con H2 en memoria (datos de ejemplo ya cargados por Flyway):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Consola H2 (perfil `local`): http://localhost:8080/h2-console

## Tests

```bash
mvn test
```

Los tests cubren: arranque del contexto con Flyway + validacion de esquema, mappers
(MapStruct), servicios de aplicacion (Mockito), adaptadores de persistencia contra H2
(`@DataJpaTest`) y controladores (`@WebMvcTest`).

## Base de datos con PostgreSQL

```bash
mvn clean package
export POSTGRES_PASSWORD='...'    # la contraseña real, fuera del repo
export DB_PASSWORD="$POSTGRES_PASSWORD"
docker compose up --build
```

El `docker-compose.yml` levanta PostgreSQL y el microservicio conectado a el. **No empaqueta
ninguna contraseña**: si `POSTGRES_PASSWORD` o `DB_PASSWORD` no estan definidas, compose se
niega a arrancar en lugar de arrancar con un valor predecible. La aplicacion se configura por
variables de entorno:

| Variable      | Descripcion                | Por defecto     |
|---------------|----------------------------|-----------------|
| `DB_URL`      | URL JDBC                   | H2 en memoria   |
| `DB_USERNAME` | Usuario de base de datos   | `sa`            |
| `DB_PASSWORD` | Contrasena de base de datos| **obligatoria en compose**, vacio en local con H2 |
| `DB_DRIVER`   | Driver JDBC                | `org.h2.Driver` |
| `SERVER_PORT` | Puerto HTTP                | `8080`          |

## Endpoints

| Metodo | Ruta            | Descripcion            |
|--------|-----------------|------------------------|
| GET    | `/users`        | Lista usuarios         |
| GET    | `/users/{id}`   | Obtiene un usuario     |
| POST   | `/users`        | Crea un usuario        |
| PUT    | `/users/{id}`   | Actualiza un usuario   |
| DELETE | `/users/{id}`   | Elimina un usuario     |
| GET    | `/centers`      | Lista centros          |
| GET    | `/centers/{id}` | Obtiene un centro      |
| POST   | `/centers`      | Crea un centro         |
| PUT    | `/centers/{id}` | Actualiza un centro    |
| DELETE | `/centers/{id}` | Elimina un centro      |

## Modificar el contrato

1. Edita `src/main/resources/contract/api.yaml`.
2. Ejecuta `mvn generate-sources` (o `mvn compile`).
3. Ajusta los controladores y mappers si cambian las firmas generadas.

Los ficheros generados no se editan a mano: se regeneran en cada build.
