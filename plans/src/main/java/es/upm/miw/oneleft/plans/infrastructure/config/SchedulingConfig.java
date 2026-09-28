package es.upm.miw.oneleft.plans.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Periodic tasks, such as the heartbeat of real-time connections. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
