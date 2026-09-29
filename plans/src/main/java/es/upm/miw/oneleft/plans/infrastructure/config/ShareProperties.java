package es.upm.miw.oneleft.plans.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.ZoneId;

/**
 * Shared plan links (HU-024).
 *
 * @param webUrl public address of the web app, where the link finally opens the plan
 * @param zone   time zone of the times shown in the link preview (the plans are local, for the next few hours)
 */
@ConfigurationProperties("oneleft.share")
public record ShareProperties(URI webUrl, ZoneId zone) {

    public ShareProperties {
        if (webUrl == null || zone == null) {
            throw new IllegalArgumentException("oneleft.share.web-url and oneleft.share.zone are required");
        }
    }
}
