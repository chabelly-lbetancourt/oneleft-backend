# OneLeft · Backend

[![lint](https://github.com/chabelly-lbetancourt/oneleft-backend/actions/workflows/lint.yml/badge.svg?branch=dev)](https://github.com/chabelly-lbetancourt/oneleft-backend/actions/workflows/lint.yml)

**OneLeft** conecta planes para las próximas horas que tienen plazas libres («falta uno») con personas cercanas que pueden unirse en tiempo real.

## Contenido

Microservicios con **Spring Boot** y arquitectura hexagonal, expuestos a través de un **API Gateway**:

- `gateway`: punto de entrada único (Spring Cloud Gateway).
- `users`: usuarios, perfiles y reputación.
- `plans`: publicación de planes, plazas, caducidad y búsqueda de planes cercanos (PostGIS) en tiempo real (SSE).
- `notifications`: avisos de planes cercanos (HU-006): preferencias de cada persona, a quién avisar y Web Push.
- `notifications`: avisos push y en tiempo real.
- `ai`: creación de planes en lenguaje natural, moderación y ranking de notificaciones.

**Stack:** Java 21 · Spring Boot · Spring Cloud · PostgreSQL/PostGIS · Redis · RabbitMQ · Keycloak · Spring AI · Maven · JUnit · JaCoCo · SonarQube

**IDE recomendado:** IntelliJ IDEA

## Módulos actuales

| Módulo | Puerto | Estado |
|---|---|---|
| `gateway` | 8080 | Enruta `/api/v1/users/**` a `users`, `/api/v1/plans/**` a `plans` y `/api/v1/notifications/**` a `notifications` |
| `users` | 8081 | HU-001 usuario autenticado · HU-002 perfil con zona aproximada y aficiones |
| `plans` | 8082 | HU-003 publicar un plan (evento `plan.published` en RabbitMQ) · HU-004 planes cercanos y avisos en tiempo real (SSE) |
| `notifications` | 8083 | HU-006 avisos de planes cercanos: escucha `plan.published`, elige a quién avisar (zona, radio, actividades, horario sin avisos y máximo diario) y avisa en la app y por Web Push |

El resto de servicios (`notifications`, `chat`, `ai`) se añaden con sus historias de usuario.

## Convenciones

- **Código en inglés:** comentarios, nombres, enumerados (`OPEN`, `INTERMEDIATE`, `BOARD_GAMES`…), logs y CI.
- **Errores de la API** en formato Problem Details (RFC 9457), con una propiedad `code` estable
  (`plan.startsTooLate`, `search.radius`…) que la web traduce al idioma de la persona usuaria.
- **Migraciones Flyway** por servicio (`V1__plans.sql`, `V2__nearby_search.sql`…); Hibernate solo valida el esquema.

## Entornos

| Rama | Entorno | Imágenes en GHCR |
|---|---|---|
| `dev` | dev: integración, Docker Compose en local | solo se construyen |
| `pre` | pre (*staging*): validación completa antes de producción | `ghcr.io/chabelly-lbetancourt/oneleft-<servicio>:pre` y `:sha-…` |
| `main` | pro (producción) | `ghcr.io/chabelly-lbetancourt/oneleft-<servicio>:latest` y `:sha-…` |

La promoción es siempre `issue#N → dev → pre → main` (ver [CONTRIBUTING.md](CONTRIBUTING.md)); el workflow `lint`
rechaza cualquier otro camino.

## Arquitectura hexagonal

Cada servicio se organiza en tres capas:

```
es.upm.miw.oneleft.<servicio>
├── domain/            entidades y reglas de negocio, sin dependencias de Spring
│   ├── model/
│   └── port/in, out/  puertos de entrada (casos de uso) y de salida (persistencia, eventos)
├── application/       implementación de los casos de uso
└── infrastructure/    adaptadores: rest/ (entrada), persistence/ (salida) y config/
```

Las reglas de dependencia se comprueban en cada build con **ArchUnit** (`HexagonalArchitectureTest`): el dominio
no depende de las otras capas ni de Spring, y la aplicación no depende de la infraestructura.

## Documentación de la API (OpenAPI)

- **Swagger UI:** <http://localhost:8080/swagger-ui.html>, en el gateway, con la API de todos los servicios
  (selector *Select a definition*).
- **Probar endpoints protegidos:** botón *Authorize* → inicio de sesión en Keycloak (Authorization Code + PKCE).
- **Especificación OpenAPI:** `/v3/api-docs` en cada servicio, o a través del gateway en `/api-docs/users`, `/api-docs/plans` y `/api-docs/notifications`.

## Cómo ejecutarlo

Requisitos: Java 21 y el entorno de [oneleft-infra/docker](https://github.com/chabelly-lbetancourt/oneleft-infra/tree/dev/docker) levantado.

```bash
./mvnw verify                                   # compila y ejecuta los tests
java -jar plans/target/plans-0.1.0-SNAPSHOT.jar # o desde IntelliJ con la clase *Application
curl localhost:8080/actuator/health             # salud del gateway
```

### Datos de demostración (seed)

Arrancados en local sin elegir perfil (IntelliJ, `./mvnw spring-boot:run` o `java -jar`), `users` y `plans` cargan
el seed nada más arrancar: los perfiles de las usuarias de prueba y 8 planes alrededor de Vallecas. Es el perfil por
defecto (`spring.profiles.default: seed`) y es idempotente: reiniciar no duplica nada, y los planes solo se vuelven a
publicar cuando los anteriores ya han empezado.

| Cómo se arranca | ¿Seed? |
|---|---|
| Local sin perfil | Sí (perfil por defecto) |
| Local con otro perfil, p. ej. `--spring.profiles.active=observability` | No: añade `seed` si lo quieres (`observability,seed`) |
| Imágenes Docker | Solo si se pide: la imagen fija `SPRING_PROFILES_DEFAULT=default`. El compose de desarrollo activa `observability,seed` |
| Tests | No, salvo los tests del seed (`@ActiveProfiles("seed")`) |
| **pro** | Nunca: los *seeders* exigen `seed & !pro` |

### Web Push (HU-006)

`notifications` envía los avisos como notificaciones del sistema con **Web Push**, implementado con el JDK: cifrado del
mensaje (RFC 8291, comprobado con el ejemplo de la RFC) y firma VAPID (RFC 8292). Necesita un par de claves VAPID en
`VAPID_PUBLIC_KEY` y `VAPID_PRIVATE_KEY` (se generan con `oneleft-infra/docker/generate-vapid-keys.sh`). Sin ellas el
servicio funciona igual, pero los avisos solo llegan dentro de la app.

Imágenes Docker:

```bash
./mvnw -DskipTests package
docker build -t oneleft/plans:0.1.0 plans
```

## Proyecto

| Repositorio | Contenido |
|---|---|
| [oneleft-backend](https://github.com/chabelly-lbetancourt/oneleft-backend) | Microservicios Spring Boot |
| [oneleft-frontend](https://github.com/chabelly-lbetancourt/oneleft-frontend) | App web Angular y app Android con Capacitor |
| [oneleft-infra](https://github.com/chabelly-lbetancourt/oneleft-infra) | Docker, Kubernetes, AWS y observabilidad |
| [oneleft-docs](https://github.com/chabelly-lbetancourt/oneleft-docs) | Memoria del TFM y documentación del proceso |

Tablero Kanban: [OneLeft · TFM](https://github.com/users/chabelly-lbetancourt/projects/4) · Normas de trabajo: [CONTRIBUTING.md](CONTRIBUTING.md)

---
Trabajo Fin de Máster · Máster Universitario en Ingeniería Web · ETSISI · Universidad Politécnica de Madrid
