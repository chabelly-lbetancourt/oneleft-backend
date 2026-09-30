package es.upm.miw.oneleft.notifications.infrastructure.rest;

import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.NotificationPreferences;
import es.upm.miw.oneleft.notifications.domain.model.PushSubscription;
import es.upm.miw.oneleft.notifications.domain.model.QuietHours;

import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

final class NotificationDtos {

    private NotificationDtos() {
    }

    /** Hours without notices, local time ({@code HH:mm}). */
    record QuietHoursDto(LocalTime start, LocalTime end) {

        static QuietHoursDto of(QuietHours quietHours) {
            return quietHours == null ? null : new QuietHoursDto(quietHours.start(), quietHours.end());
        }

        QuietHours toDomain() {
            return new QuietHours(start, end);
        }
    }

    /**
     * @param activities empty for every activity
     * @param quietHours {@code null} for notices at any time
     */
    record PreferencesDto(boolean enabled, Double latitude, Double longitude, int radiusMeters, Set<Activity> activities,
                          QuietHoursDto quietHours, int maxPerDay) {

        static PreferencesDto of(NotificationPreferences preferences) {
            return new PreferencesDto(preferences.enabled(), preferences.latitude(), preferences.longitude(),
                    preferences.radiusMeters(), preferences.activities(), QuietHoursDto.of(preferences.quietHours()),
                    preferences.maxPerDay());
        }

        NotificationPreferences toDomain(UUID userId) {
            return new NotificationPreferences(userId, enabled, latitude, longitude, radiusMeters, activities,
                    quietHours == null ? null : quietHours.toDomain(), maxPerDay);
        }
    }

    record PublicKeyResponse(String publicKey) {
    }

    record SubscriptionKeys(String p256dh, String auth) {
    }

    /** A browser's {@code PushSubscription} as the Push API gives it, plus the language of the app. */
    record SubscriptionRequest(String endpoint, SubscriptionKeys keys, String language) {

        PushSubscription toDomain(UUID userId) {
            return new PushSubscription(userId, endpoint, keys == null ? null : keys.p256dh(),
                    keys == null ? null : keys.auth(), language);
        }
    }
}
