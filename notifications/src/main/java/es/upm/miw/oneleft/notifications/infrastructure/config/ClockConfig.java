package es.upm.miw.oneleft.notifications.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Injectable clock in the local time zone: quiet hours and the daily limit of notices follow the local day, and the
 * tests use a fixed clock.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock(@Value("${oneleft.notifications.zone}") ZoneId zone) {
        return Clock.system(zone);
    }
}
