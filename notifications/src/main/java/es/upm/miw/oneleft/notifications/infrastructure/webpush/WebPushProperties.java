package es.upm.miw.oneleft.notifications.infrastructure.webpush;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Server keys for Web Push (VAPID), from the environment ({@code VAPID_PUBLIC_KEY}, {@code VAPID_PRIVATE_KEY}); never
 * in the repository. Without them Web Push is off and people only get the notices inside the app.
 *
 * @param subject contact of the sender for the push services ({@code mailto:})
 */
@ConfigurationProperties("oneleft.webpush")
public record WebPushProperties(String publicKey, String privateKey, String subject) {

    public boolean configured() {
        return publicKey != null && !publicKey.isBlank() && privateKey != null && !privateKey.isBlank();
    }
}
