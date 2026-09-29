package es.upm.miw.oneleft.plans.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ShareProperties.class)
public class ShareConfig {
}
