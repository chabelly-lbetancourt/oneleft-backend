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
