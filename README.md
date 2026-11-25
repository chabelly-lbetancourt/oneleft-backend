# OneLeft · Backend

[![lint](https://github.com/chabelly-lbetancourt/oneleft-backend/actions/workflows/lint.yml/badge.svg?branch=dev)](https://github.com/chabelly-lbetancourt/oneleft-backend/actions/workflows/lint.yml)

**OneLeft** conecta planes para las próximas horas que tienen plazas libres («falta uno») con personas cercanas que pueden unirse en tiempo real.

## Contenido

Microservicios con **Spring Boot** y arquitectura hexagonal, expuestos a través de un **API Gateway**:

- `gateway`: punto de entrada único (Spring Cloud Gateway).
- `users`: usuarios, perfiles y reputación.
- `plans`: publicación de planes, plazas, caducidad y búsqueda de planes cercanos (PostGIS) en tiempo real (SSE), con la compatibilidad de cada plan con quien lo mira (HU-031, [ver más abajo](#compatibilidad-por-nivel-y-aficiones-hu-031)).
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
| `users` | 8081 | HU-001 usuario autenticado · HU-002 perfil con zona aproximada y aficiones · HU-020 foto de perfil en un almacenamiento compatible con S3, con enlaces firmados · HU-048 bio del perfil social · HU-045 descargar mis datos y borrar mi cuenta (ver más abajo) |
| `plans` | 8082 | HU-003 publicar un plan (evento `plan.published` en RabbitMQ) · HU-004 planes cercanos y avisos en tiempo real (SSE) · HU-025 check-in en el punto de encuentro (geocerca de 150 m con PostGIS; la ubicación no se guarda) y, al cerrarse, quién llegó (`plan.attendance-closed`) · HU-048 los 5 últimos planes celebrados de una persona, si `social` dice que se pueden ver · HU-051 reenvía por SSE los avisos de actividad nueva del feed (`feed-activity`) y HU-060 los de kudos (`kudos`) · HU-033 copia los bloqueos de `social` (cola duradera `plans.blocks`) y deja fuera, en los dos sentidos, los planes cercanos y su tiempo real, el detalle de un plan (salvo para quien ya está en él), unirse y las personas libres cerca · HU-063 quien tiene más reputación espera por delante en la lista de espera y puede organizar más planes a la vez (5 para todo el mundo, `409 plan.tooManyActive`): se lo pregunta a `reputation` con el token de la persona · HU-052 recuerdos del plan: álbum de fotos que comparten su organizador y sus participantes cuando el plan ha terminado, en el mismo almacenamiento que las fotos de perfil ([ver más abajo](#recuerdos-del-plan-hu-052)) · cancelar un plan propio antes de que empiece (`DELETE /api/v1/plans/{planId}`, evento `plan.cancelled` con `reason = ORGANIZER`; [ver más abajo](#cancelar-un-plan)) |
| `notifications` | 8083 | HU-006 avisos de planes cercanos: escucha `plan.published`, elige a quién avisar (zona, radio, actividades, horario sin avisos y máximo diario) y avisa en la app y por Web Push · HU-033 nadie recibe avisos de planes de alguien a quien bloqueó o que le bloqueó (copia de los bloqueos en la cola `notifications.blocks`) · HU-012 avisos a quien más probabilidad tiene de apuntarse: puntúa a las personas con el servicio `ranker` (si no responde, van primero las más cercanas), avisa por tandas (5 cada 3 minutos mientras queden plazas y falten más de 15 minutos; `NOTICES_*`), vuelve a comprobar horas de silencio, límite diario y bloqueos al enviar cada tanda y aprende de `plan.joined` quién aceptó (`sent_notice.accepted_at`). `NOTICES_STRATEGY=distance` ordena por distancia, para compararlo · HU-063 copia de `reputation` lo que vale el nivel de cada persona (cola duradera `notifications.perks`): la probabilidad del modelo se multiplica por su peso al ordenar los avisos y puede guardar más alertas (5 para todo el mundo) |
| `chat` | 8084 | HU-008 chat de cada plan: solo su organizador y participantes (se pregunta a `plans`), de solo lectura cuando el plan termina, mensajes entre réplicas por RabbitMQ y borrados 24 h después |
| `social` | 8085 | HU-047 seguir a personas: perfil público o privado (privado por defecto), solicitudes que se aceptan o rechazan, seguidores y seguidos. El nombre de alguien nuevo se pregunta una vez a `users` · HU-048 decide quién ve los planes y la red de alguien (su perfil es público, es uno mismo o se le sigue) y da sus seguidores y seguidos · HU-051 feed de actividad: escucha `plan.published`, `plan.joined`, `plan.left`, `plan.cancelled` y `plan.attendance-closed` en colas duraderas (`social.*`) y guarda una vez lo que hace cada persona; el feed se calcula al leerlo (*fan-out* al leer) con lo de quien se sigue y ha aceptado, guarda la primera página en Redis y avisa a cada seguidor en tiempo real (`notification.feed-activity`) · HU-059 privacidad del feed: quién ve mi actividad (mis seguidores, por defecto, o nadie; mientras es «nadie» no se guarda nada nuevo) y planes concretos que oculto de ella (un filtro al leer: al mostrarlo de nuevo vuelve lo guardado) · HU-033 bloquear: termina los seguimientos y solicitudes en los dos sentidos y, para la persona bloqueada, mi perfil no existe (404, sin aviso); publica `user.blocked` y `user.unblocked` en `oneleft.social`. Reportar a una persona o un plan con su motivo: los reportes abiertos se listan para la administración (rol `admin`) y se publican como `report.created` en `oneleft.reports` para la moderación |
| `ranker` | 8087 | HU-012 servicio en Python (FastAPI + scikit-learn) que da la probabilidad de que cada persona se apunte a un plan; ver [`ranker/README.md`](ranker/README.md). No es un módulo de Maven: tiene su propio test (pytest) e imagen |
| `reputation` | 8086 | HU-009 reputación: escucha `plan.checked-in`, `plan.attendance-closed`, `plan.cancelled` y `report.confirmed` en colas duraderas, apunta cada cambio en un libro inmutable (motivo, puntos, plan y fecha) y mantiene la puntuación como proyección recalculable · HU-044 insignias por hitos a partir de la asistencia, con aviso en tiempo real (`notification.badge-earned`) · HU-048 estadísticas de una persona (planes completados, organizados y actividades) · HU-060 kudos entre quienes llegaron a un mismo plan (Puntual, Buen rollo, Gran nivel y, al organizador, Organizó genial): uno por persona y plan, hasta 7 días después, +5 en el libro y aviso en tiempo real (`notification.kudos`, que `plans` reenvía por SSE) · HU-066 anti-trampas: topes diarios por motivo (4 asistencias, 3 planes organizados y 10 kudos recibidos al día, en hora de España), kudos de la misma persona en 30 días que valen la mitad cada vez (5, 2, 1, 0) y nada entre una pareja que se da 3 o más kudos en cada sentido; el apunte se guarda con los puntos que dio de verdad y el motivo del recorte (`limitedBy`) · HU-062 niveles por puntuación (`NEW` 0–99, `REGULAR` 100–499, `NEIGHBOURHOOD_REFERENCE` 500–1.499, `LEGEND` 1.500+) en todas las respuestas de reputación, y racha semanal: el primer check-in de cada semana (de lunes a domingo, en hora de España) da 5 puntos por semana seguida con plan, hasta 25 (`GET /me/streak`) · HU-063 ventajas por reputación: lo que vale cada nivel (`GET /me/perks`), el sello de «Organizador de confianza» (`trustedOrganizer`) en todas las respuestas de reputación y el evento `reputation.perks-changed` cuando alguien cambia de nivel ([ver más abajo](#ventajas-por-reputación-hu-063)) |

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

### Recuerdos del plan (HU-052)

Cada plan tiene un álbum de fotos que solo ven y llenan su organizador y sus participantes, cuando el plan ha terminado
(3 horas después de empezar; a un plan cancelado no se le añaden fotos). Vive en `plans`, que es quien sabe quién
estuvo en cada plan, y usa el mismo almacenamiento y el mismo mecanismo que las fotos de perfil: la foto nunca pasa por
el backend.

1. `POST /api/v1/plans/{planId}/memories/uploads` con el tipo y el tamaño (WebP o JPEG, hasta 2 MiB) devuelve un enlace
   firmado que solo acepta un `PUT` de exactamente ese tipo y tamaño durante 5 minutos, y la clave de la foto
   (`memories/<persona>/<plan>/<aleatorio>.<extensión>`: el nombre del fichero original no se usa).
2. La web sube la foto a ese enlace y la añade al álbum con `POST /api/v1/plans/{planId}/memories` y esa clave. El
   servicio comprueba en el almacenamiento que está, su tamaño y su **tipo real** (lee sus primeros bytes: lo que no
   empieza como un WebP o un JPEG se borra, `400 memory.notAPhoto`). Repetirlo devuelve la misma foto.
3. `GET /api/v1/plans/{planId}/memories?page=0&size=20` da el álbum, de la más nueva a la más antigua, en páginas de
   hasta 50 fotos (`items`, `page`, `size`, `total`). Cada foto lleva `id`, `planId`, `userId`, `size`, `uploadedAt` y
   un enlace firmado `url` que caduca en 1 hora.
4. `DELETE /api/v1/plans/{planId}/memories/{memoryId}` quita una foto, también del almacenamiento: solo quien la subió.

| Código | Estado | Cuándo |
|---|---|---|
| `plan.notFound` | 404 | No existe el plan |
| `plan.notParticipant` | 403 | No es su organizador ni un participante (en cualquiera de las cuatro operaciones) |
| `memory.planNotFinished` | 409 | El plan no ha terminado o está cancelado (al subir) |
| `memory.personLimit` · `memory.planLimit` | 409 | 10 fotos por persona y álbum, 100 fotos por álbum |
| `memory.format` · `memory.empty` · `memory.tooBig` | 400 | No es WebP ni JPEG, está vacía o pasa de 2 MiB |
| `memory.key` · `memory.notUploaded` · `memory.notAPhoto` | 400 | La clave no se dio a esa persona para ese plan, no se subió nada o el contenido no es la foto que dice |
| `memory.notFound` | 404 | El álbum no tiene esa foto |
| `memory.notYours` | 403 | La foto la subió otra persona |
| `page.invalid` | 400 | Página negativa o tamaño fuera de 1–50 |
| `memory.unavailable` | 503 | El almacenamiento no responde |

- **Configuración:** `plans` necesita las mismas variables `STORAGE_*` que `users` (tabla de arriba) y el mismo bucket;
  sus fotos van bajo `memories/` y las de perfil bajo `photos/`. Sin almacenamiento el servicio arranca igual y solo
  falla el álbum.
- **Mis datos y borrar la cuenta (HU-045):** las fotos de cada persona salen en `memories` de
  `GET /api/v1/plans/me/data` y se borran con su cuenta, del álbum y del almacenamiento. Si el almacenamiento no
  responde en ese momento, el resto se borra igual y queda un error en el log con la carpeta que hay que borrar a mano.
- **Subidas sin confirmar:** lo que alguien subió a un álbum y no añadió en 1 hora se borra del almacenamiento la
  siguiente vez que pide un enlace para ese álbum.
- **Bloqueos (HU-033):** como el resto de lo que ve el grupo de un plan (quién llegó, el chat), el álbum no se filtra
  por bloqueos entre sus miembros.
- **Pendiente:** la moderación de las imágenes (HU-011) y avisar al grupo de las fotos nuevas.

### Cancelar un plan

Quien organiza un plan puede cancelarlo antes de que empiece: `DELETE /api/v1/plans/{planId}` en `plans`, con su token.
Responde `204` y es idempotente: cancelar un plan ya cancelado responde `204` y no avisa otra vez.

| `code` | Estado | Cuándo |
|---|---|---|
| — | 401 | Sin token o con un token inválido |
| `plan.notOrganizer` | 403 | Quien lo pide no organiza el plan |
| `plan.notFound` | 404 | El plan no existe |
| `plan.started` | 409 | El plan ya ha empezado o ha terminado |
| `plan.busy` | 409 | El plan está cambiando en ese momento (bloqueo optimista); se reintenta |

- **Qué pasa con el plan:** queda en `CANCELLED` (la misma transición que cuando no llega al mínimo, HU-039): conserva
  quién se había apuntado, su lista de espera se vacía, sale de `GET /plans/mine` y de los planes cercanos, y **deja de
  contar para el límite de planes a la vez** (HU-063).
- **Evento:** `plan.cancelled` en el exchange `oneleft.plans`, con los mismos campos que el de HU-039 (`planId`,
  `organizerId`, `title`, `placeName`, `startsAt`, `reason`, `recipientIds` y `occurredAt`) y `reason = ORGANIZER`
  (el del mínimo lleva `MINIMUM_NOT_REACHED`, y el de una cuenta borrada, `ACCOUNT_DELETED`). `recipientIds` son los participantes y la lista de espera; la
  organizadora no va, porque lo ha decidido ella.
- **Quién se entera:** los mismos avisos que ya había para una cancelación: `plan-cancelled` en el flujo SSE de cada
  persona (`PlanCancelledNotice`, con `reason`) y la notificación del navegador de `notifications`, cuyo texto dice si
  lo canceló quien lo organizaba o si no se llegó al mínimo. `chat` cierra el chat del plan y `social` lo quita de los
  feeds. Quien tenía una solicitud pendiente (HU-069) no recibe aviso, igual que en HU-039.
- **Reputación (HU-009):** `reputation` solo resta por `reason = ORGANIZER`, y solo si se cancela a menos de una hora
  del inicio (`LATE_CANCELLATION`, −10). Cancelar con más margen no cuesta nada.

### Mis datos y borrar la cuenta (HU-045)

- **Descargar:** `GET /api/v1/users/me/data` devuelve un único JSON con lo de `users` y lo que cada servicio da en su
  `GET /api/v1/{plans,social,reputation,notifications,chats}/me/data` (el mismo token, una llamada por servicio). Si uno
  no responde, no hay fichero: `503 data.unavailable`.
- **Consentimiento:** `GET` y `PUT /api/v1/users/me/consent`; el texto vigente es `PrivacyPolicy.CURRENT_VERSION`.
- **Borrar:** `DELETE /api/v1/users/me/account` borra lo de `users`, las fotos y la cuenta en Keycloak (cliente
  `oneleft-accounts`, *client credentials*, solo gestiona usuarios) y publica `user.deleted` en el exchange
  `oneleft.accounts`, tras el commit. Cada servicio la recibe en su cola duradera `<servicio>.account-deleted`.
- **Qué se borra y qué se anonimiza:** `plans` libera su plaza en los planes futuros, cancela los que organizaba y deja
  «Persona eliminada» en los pasados. Cada plan suyo sin empezar se cancela con la misma transición que cualquier otra
  cancelación y publica `plan.cancelled` tras el commit, **antes** de borrar sus datos, con `reason = ACCOUNT_DELETED`:
  quienes estaban apuntados o en lista de espera reciben el aviso (SSE y navegador, con el texto de «lo canceló quien
  lo organizaba»), `chat` cierra el chat y `social` lo quita de los feeds; `reputation` no apunta nada, porque solo
  resta por `ORGANIZER`. Además, `chat` deja sus mensajes con una persona anónima; los kudos que dio pierden su
  autor; el libro de `reputation` se borra con una excepción explícita de su trigger. El resto se borra.
- **Repetible:** cada borrado puede recibirse dos veces sin efecto. No hay *outbox*: si `users` cae entre el commit y la
  publicación, hay que volver a pedir el borrado.

| Variable | Para qué |
|---|---|
| `KEYCLOAK_ADMIN_URL` | Dónde `users` llama al API de administración de Keycloak |
| `ACCOUNTS_CLIENT_SECRET` | Secreto del cliente `oneleft-accounts` (en producción, obligatorio) |

### Compatibilidad por nivel y aficiones (HU-031)

Cada plan dice lo bien que encaja con quien lo mira: una puntuación de 0 a 100 y sus motivos. No es un modelo: son
reglas fijas (`Compatibility`, en el dominio de `plans`), así que el mismo plan y el mismo perfil dan siempre lo mismo
y cada punto se puede explicar. Solo usa datos que ya había: la actividad y el nivel del plan, y las aficiones del
perfil con su nivel, que `users` ya daba a `plans` junto al género y la edad (HU-070, `GET
/api/v1/users/me/demographics/audience`, con el token de la persona). No se guarda nada nuevo.

| Parte | Puntos | Motivo (`reasons`) |
|---|---|---|
| Actividad | 60 | `SAME_ACTIVITY`: es una de mis aficiones |
| | 30 | `SIMILAR_ACTIVITY`: no lo es, pero una de mis aficiones es de la misma categoría (deporte, cultura, ocio, comer y beber) |
| | 0 | `NEW_ACTIVITY`: nada parecido en mi perfil (y siempre con `OTHER`) |
| Nivel, en una afición mía | 40 | `SAME_LEVEL`: el plan es de mi nivel · `ANY_LEVEL`: el plan no pide nivel |
| | 20 | `CLOSE_LEVEL`: un nivel por encima o por debajo |
| | 0 | `LEVEL_GAP`: dos niveles de diferencia |
| Nivel, en una actividad que no está en mi perfil (cuento como principiante) | 40 | `ANY_LEVEL` · `BEGINNER_FRIENDLY`: el plan es para principiantes |
| | 20 · 0 | `NEEDS_EXPERIENCE`: el plan pide nivel intermedio (20) o avanzado (0) |

- **Dónde sale:** `compatibility` (`score` y `reasons`, siempre dos: primero el de la actividad y después el del nivel)
  dentro del plan, en `GET /api/v1/plans/{planId}` y en cada `plan` de `GET /api/v1/plans/nearby`. En el resto de
  respuestas con un plan va a `null`.
- **Cuándo es `null`:** si el plan es mío (lo organizo yo) o si mi perfil no se puede saber: no he puesto mi fecha de
  nacimiento y género (sin ellos `users` no da nada, y tampoco me dejaría apuntarme) o `users` no responde. Una
  compatibilidad nunca hace fallar nada.
- **Ordenar:** `GET /api/v1/plans/nearby?sort=COMPATIBILITY` (por defecto `sort=DISTANCE`, como hasta ahora). Ordena
  los mismos planes que devuelve la búsqueda (los 50 más cercanos que pasan los filtros), del más compatible al menos y,
  a igualdad, del más cercano al más lejano. Sin perfil conocido el orden sigue siendo por distancia. Un `sort`
  desconocido responde `400`.
- **Tiempo real:** el aviso `plan-published` del flujo SSE no cambia: solo avisa, y la web vuelve a pedir la lista.
- **Pendiente:** usarla en el ranking de avisos (HU-012), que hoy puntúa con el servicio `ranker` y no conoce las
  aficiones del perfil.

### Ventajas por reputación (HU-063)

Los números de cada nivel solo están en `reputation` (`Perks`); los demás servicios los preguntan o los copian, nunca
los calculan.

| Nivel | Prioridad en lista de espera | Peso en los avisos | Planes a la vez | Alertas guardadas |
|---|---|---|---|---|
| `NEW` | 0 | 1,0 | 5 | 5 |
| `REGULAR` | 1 | 1,1 | 6 | 6 |
| `NEIGHBOURHOOD_REFERENCE` | 2 | 1,25 | 8 | 8 |
| `LEGEND` | 3 | 1,5 | 10 | 10 |

- **Mis ventajas:** `GET /api/v1/reputation/me/perks` devuelve `level`, `waitlistPriority`, `noticeWeight`,
  `maxActivePlans`, `maxSavedAlerts` y `trustedOrganizer`.
- **Sello «Organizador de confianza»:** nivel `REGULAR` o superior y 5 o más planes organizados que se celebraron. Va
  como `trustedOrganizer` en `GET /api/v1/reputation/me`, `/people/{userId}` y `/scores?ids=…`.
- **Lista de espera (HU-023) y planes a la vez:** `plans` pregunta `GET /me/perks` a `reputation` con el token de la
  persona (`REPUTATION_URL`), como hace con `users` y `social`. En la lista va primero quien tiene más prioridad y,
  a igualdad, quien llegó antes; la prioridad se guarda al apuntarse (`plan_waitlist.priority`). Cuentan como planes a
  la vez los publicados sin cancelar que aún no han empezado; al llegar al límite, `409 plan.tooManyActive` con el
  límite en `max`. `reputation` solo se pregunta al llegar al límite de todo el mundo. Si no responde, la persona
  tiene lo que todo el mundo: una ventaja nunca hace fallar nada.
- **Avisos (HU-012) y alertas (HU-036):** `notifications` no tiene el token de cada persona, así que guarda una copia
  (`member_perks`) a partir del evento `reputation.perks-changed` del exchange `oneleft.reputation`, con los campos
  `userId`, `level`, `waitlistPriority`, `noticeWeight`, `maxActivePlans`, `maxSavedAlerts` y `occurredAt` (gana el
  más reciente). El peso multiplica la probabilidad del modelo; con la regla de distancia (o si el modelo no
  responde) no hay pesos. Al pasar del límite de alertas, `409 alerts.limit` como hasta ahora.
- **Poner las copias al día:** el evento solo sale cuando alguien cambia de nivel. `POST
  /api/v1/reputation/scores/rebuild` (administración) anuncia además las ventajas de todas las personas con
  puntuación: hay que lanzarlo una vez tras desplegar esta historia.

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
