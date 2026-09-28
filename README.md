# OneLeft · Backend

[![lint](https://github.com/chabelly-lbetancourt/oneleft-backend/actions/workflows/lint.yml/badge.svg?branch=dev)](https://github.com/chabelly-lbetancourt/oneleft-backend/actions/workflows/lint.yml)

**OneLeft** conecta planes para las próximas horas que tienen plazas libres («falta uno») con personas cercanas que pueden unirse en tiempo real.

## Contenido

Microservicios con **Spring Boot** y arquitectura hexagonal, expuestos a través de un **API Gateway**:

- `gateway`: punto de entrada único (Spring Cloud Gateway).
- `users`: usuarios, perfiles y reputación.
- `plans`: publicación de planes, plazas y caducidad.
- `geo`: búsqueda geoespacial de planes y personas cercanas.
- `notifications`: avisos push y en tiempo real.
- `ai`: creación de planes en lenguaje natural, moderación y ranking de notificaciones.

**Stack:** Java 21 · Spring Boot · Spring Cloud · PostgreSQL/PostGIS · Redis · RabbitMQ · Keycloak · Spring AI · Maven · JUnit · JaCoCo · SonarQube

**IDE recomendado:** IntelliJ IDEA

## Módulos actuales

| Módulo | Puerto | Estado |
|---|---|---|
| `gateway` | 8080 | Enruta `/api/v1/users/**` a `users` y `/api/v1/plans/**` a `plans` |
| `users` | 8081 | Esqueleto hexagonal |
| `plans` | 8082 | Esqueleto hexagonal |

El resto de servicios (`geo`, `notifications`, `chat`, `ai`) se añaden con sus historias de usuario.

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
- **Especificación OpenAPI:** `/v3/api-docs` en cada servicio, o a través del gateway en `/api-docs/users` y `/api-docs/plans`.

## Cómo ejecutarlo

Requisitos: Java 21 y el entorno de [oneleft-infra/docker](https://github.com/chabelly-lbetancourt/oneleft-infra/tree/dev/docker) levantado.

```bash
./mvnw verify                                   # compila y ejecuta los tests
java -jar plans/target/plans-0.1.0-SNAPSHOT.jar # o desde IntelliJ con la clase *Application
curl localhost:8080/actuator/health             # salud del gateway
```

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
