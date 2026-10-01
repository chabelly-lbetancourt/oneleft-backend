package es.upm.miw.oneleft.notifications.domain.model;

import java.util.UUID;

/**
 * A browser subscribed to Web Push. The endpoint belongs to the browser's push service; the keys let the server
 * encrypt messages that only that browser can read.
 *
 * @param language language of the notices shown by the system (the app's language when subscribing)
 */
public record PushSubscription(UUID userId, String endpoint, String p256dh, String auth, String language) {

    public PushSubscription {
        if (endpoint == null || !endpoint.startsWith("https://")) {
            throw new ValidationException("notifications.pushEndpoint", "Push endpoints are https URLs");
        }
        if (p256dh == null || p256dh.isBlank() || auth == null || auth.isBlank()) {
            throw new ValidationException("notifications.pushKeys", "The subscription keys are missing");
        }
        language = "en".equals(language) ? "en" : "es";
    }
}
