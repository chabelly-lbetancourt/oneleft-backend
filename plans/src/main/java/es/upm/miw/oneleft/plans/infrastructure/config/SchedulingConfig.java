package es.upm.miw.oneleft.plans.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Tareas periódicas, como el latido de las conexiones en tiempo real. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
