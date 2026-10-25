# OneLeft · Backend

[![lint](https://github.com/chabelly-lbetancourt/oneleft-backend/actions/workflows/lint.yml/badge.svg?branch=dev)](https://github.com/chabelly-lbetancourt/oneleft-backend/actions/workflows/lint.yml)

**OneLeft** conecta planes para las próximas horas que tienen plazas libres («falta uno») con personas cercanas que pueden unirse en tiempo real.

## Contenido

Microservicios con **Spring Boot** y arquitectura hexagonal, expuestos a través de un **API Gateway**:

- `gateway`: punto de entrada único (Spring Cloud Gateway).
- `users`: usuarios, perfiles y reputación.
- `plans`: publicación de planes, plazas, caducidad y búsqueda de planes cercanos (PostGIS) en tiempo real (SSE).
- `notifications`: avisos de planes cercanos (HU-006): preferencias de cada persona, a quién avisar y Web Push.
- `chat`: chat de cada plan en tiempo real (HU-008) por WebSocket, con el gateway como relé.
- `social`: seguir a personas (HU-047), con perfiles públicos o privados y solicitudes, el feed de actividad (HU-051) y bloquear y reportar a personas y planes (HU-033).
- `reputation`: reputación dirigida por eventos (HU-009), con un libro de apuntes inmutable.
- `ai`: creación de planes en lenguaje natural, moderación y ranking de notificaciones.

**Stack:** Java 21 · Spring Boot · Spring Cloud · PostgreSQL/PostGIS · Redis · RabbitMQ · Keycloak · Spring AI · Maven · JUnit · JaCoCo · SonarQube

**IDE recomendado:** IntelliJ IDEA

## Módulos actuales

| Módulo | Puerto | Estado |
|---|---|---|
| `gateway` | 8080 | Enruta `/api/v1/users/**` a `users`, `/api/v1/plans/**` y `/api/v1/places/**` a `plans`, `/api/v1/notifications/**` a `notifications`, `/api/v1/chats/**` a `chat`, `/api/v1/social/**` a `social` y `/api/v1/reputation/**` a `reputation`. Hace de relé del WebSocket `/ws/chat` (el token va en el subprotocolo `oneleft.bearer`) |
| `users` | 8081 | HU-001 usuario autenticado · HU-002 perfil con zona aproximada y aficiones · HU-020 foto de perfil en un almacenamiento compatible con S3, con enlaces firmados · HU-048 bio del perfil social |
| `plans` | 8082 | HU-003 publicar un plan (evento `plan.published` en RabbitMQ) · HU-004 planes cercanos y avisos en tiempo real (SSE) · HU-025 check-in en el punto de encuentro (geocerca de 150 m con PostGIS; la ubicación no se guarda) y, al cerrarse, quién llegó (`plan.attendance-closed`) · HU-048 los 5 últimos planes celebrados de una persona, si `social` dice que se pueden ver · HU-051 reenvía por SSE los avisos de actividad nueva del feed (`feed-activity`) y HU-060 los de kudos (`kudos`) · HU-033 copia los bloqueos de `social` (cola duradera `plans.blocks`) y deja fuera, en los dos sentidos, los planes cercanos y su tiempo real, el detalle de un plan (salvo para quien ya está en él), unirse y las personas libres cerca |
| `notifications` | 8083 | HU-006 avisos de planes cercanos: escucha `plan.published`, elige a quién avisar (zona, radio, actividades, horario sin avisos y máximo diario) y avisa en la app y por Web Push · HU-033 nadie recibe avisos de planes de alguien a quien bloqueó o que le bloqueó (copia de los bloqueos en la cola `notifications.blocks`) |
| `chat` | 8084 | HU-008 chat de cada plan: solo su organizador y participantes (se pregunta a `plans`), de solo lectura cuando el plan termina, mensajes entre réplicas por RabbitMQ y borrados 24 h después |
| `social` | 8085 | HU-047 seguir a personas: perfil público o privado (privado por defecto), solicitudes que se aceptan o rechazan, seguidores y seguidos. El nombre de alguien nuevo se pregunta una vez a `users` · HU-048 decide quién ve los planes y la red de alguien (su perfil es público, es uno mismo o se le sigue) y da sus seguidores y seguidos · HU-051 feed de actividad: escucha `plan.published`, `plan.joined`, `plan.left`, `plan.cancelled` y `plan.attendance-closed` en colas duraderas (`social.*`) y guarda una vez lo que hace cada persona; el feed se calcula al leerlo (*fan-out* al leer) con lo de quien se sigue y ha aceptado, guarda la primera página en Redis y avisa a cada seguidor en tiempo real (`notification.feed-activity`) · HU-059 privacidad del feed: quién ve mi actividad (mis seguidores, por defecto, o nadie; mientras es «nadie» no se guarda nada nuevo) y planes concretos que oculto de ella (un filtro al leer: al mostrarlo de nuevo vuelve lo guardado) · HU-033 bloquear: termina los seguimientos y solicitudes en los dos sentidos y, para la persona bloqueada, mi perfil no existe (404, sin aviso); publica `user.blocked` y `user.unblocked` en `oneleft.social`. Reportar a una persona o un plan con su motivo: los reportes abiertos se listan para la administración (rol `admin`) y se publican como `report.created` en `oneleft.reports` para la moderación |
| `reputation` | 8086 | HU-009 reputación: escucha `plan.checked-in`, `plan.attendance-closed`, `plan.cancelled` y `report.confirmed` en colas duraderas, apunta cada cambio en un libro inmutable (motivo, puntos, plan y fecha) y mantiene la puntuación como proyección recalculable · HU-044 insignias por hitos a partir de la asistencia, con aviso en tiempo real (`notification.badge-earned`) · HU-048 estadísticas de una persona (planes completados, organizados y actividades) · HU-060 kudos entre quienes llegaron a un mismo plan (Puntual, Buen rollo, Gran nivel y, al organizador, Organizó genial): uno por persona y plan, hasta 7 días después, +5 en el libro y aviso en tiempo real (`notification.kudos`, que `plans` reenvía por SSE) |

El servicio `ai` se añade con sus historias de usuario.

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

### Fotos de perfil (HU-020)

`users` guarda las fotos en un almacenamiento de objetos compatible con S3: Versity S3 Gateway en Docker y S3 o un
bucket de Lightsail al desplegar, con el mismo código (AWS SDK) y otra configuración. La foto nunca pasa por el
backend:

1. `POST /api/v1/users/me/photo/uploads` con el tipo y el tamaño (WebP o JPEG, hasta 1 MiB) devuelve un enlace firmado
   que solo acepta un `PUT` de exactamente ese tipo y tamaño durante 5 minutos.
2. La web sube la foto a ese enlace y la confirma con `PUT /api/v1/users/me/photo`, que comprueba en el almacenamiento
   que está y es válida, la guarda y borra la anterior.
3. Las fotos se ven con enlaces firmados que caducan en 1 hora y solo con sesión: `GET /api/v1/users/photos?ids=…`
   (hasta 50 personas por petición, una por lista). El bucket es privado.

| Variable | Para qué | Por defecto |
|---|---|---|
| `STORAGE_ENDPOINT` | Dónde lo llama el servicio | `http://localhost:7070` |
| `STORAGE_PUBLIC_ENDPOINT` | Dónde lo alcanza el navegador: los enlaces se firman para esa dirección | `STORAGE_ENDPOINT` |
| `STORAGE_BUCKET` · `STORAGE_REGION` | Bucket y región | `oneleft-photos` · `us-east-1` |
| `STORAGE_ACCESS_KEY` · `STORAGE_SECRET_KEY` | Claves, siempre desde el entorno al desplegar | las de desarrollo |
| `STORAGE_PATH_STYLE` | El bucket en la ruta, como piden los servidores compatibles | `true` |
| `STORAGE_CREATE_BUCKET` | Crea el bucket al arrancar (desarrollo); desplegado, `false` | `true` |

Si el almacenamiento no responde, el servicio arranca igual y solo fallan las fotos (`503`, `photo.unavailable`).

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
| [oneleft-infra](https://github.com/chabelly-lbetancourt/oneleft-infra) | Docker Compose, despliegue en AWS Lightsail y observabilidad |
| [oneleft-docs](https://github.com/chabelly-lbetancourt/oneleft-docs) | Memoria del TFM y documentación del proceso |

Tablero Kanban: [OneLeft · TFM](https://github.com/users/chabelly-lbetancourt/projects/4) · Normas de trabajo: [CONTRIBUTING.md](CONTRIBUTING.md)

---
Trabajo Fin de Máster · Máster Universitario en Ingeniería Web · ETSISI · Universidad Politécnica de Madrid
