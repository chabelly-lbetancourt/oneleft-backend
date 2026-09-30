package es.upm.miw.oneleft.notifications.infrastructure.persistence;

import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.NotificationPreferences;
import es.upm.miw.oneleft.notifications.domain.model.QuietHours;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
class PreferencesEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;
    private boolean enabled;
    private Double latitude;
    private Double longitude;
    @Column(name = "radius_meters")
    private int radiusMeters;
    @Column(name = "quiet_start")
    private LocalTime quietStart;
    @Column(name = "quiet_end")
    private LocalTime quietEnd;
    @Column(name = "max_per_day")
    private int maxPerDay;
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notification_preference_activity", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "activity")
    @Enumerated(EnumType.STRING)
    private Set<Activity> activities = new HashSet<>();

    protected PreferencesEntity() {
    }

    static PreferencesEntity of(NotificationPreferences preferences, Instant now) {
        var entity = new PreferencesEntity();
        entity.userId = preferences.userId();
        entity.update(preferences, now);
        return entity;
    }

    void update(NotificationPreferences preferences, Instant now) {
        enabled = preferences.enabled();
        latitude = preferences.latitude();
        longitude = preferences.longitude();
        radiusMeters = preferences.radiusMeters();
        quietStart = preferences.quietHours() == null ? null : preferences.quietHours().start();
        quietEnd = preferences.quietHours() == null ? null : preferences.quietHours().end();
        maxPerDay = preferences.maxPerDay();
        activities.clear();
        activities.addAll(preferences.activities());
        updatedAt = now;
    }

    NotificationPreferences toDomain() {
        var quiet = quietStart == null || quietEnd == null ? null : new QuietHours(quietStart, quietEnd);
        return new NotificationPreferences(userId, enabled, latitude, longitude, radiusMeters,
                activities.isEmpty() ? Set.of() : EnumSet.copyOf(activities), quiet, maxPerDay);
    }
}
