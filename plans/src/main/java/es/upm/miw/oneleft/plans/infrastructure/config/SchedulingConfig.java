package es.upm.miw.oneleft.plans.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Periodic tasks: the heartbeat of real-time connections and the lifecycle of the plans (HU-007). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
