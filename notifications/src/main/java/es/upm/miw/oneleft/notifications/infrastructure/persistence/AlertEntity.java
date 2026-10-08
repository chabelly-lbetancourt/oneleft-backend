package es.upm.miw.oneleft.notifications.infrastructure.persistence;

import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.Level;
import es.upm.miw.oneleft.notifications.domain.model.SavedAlert;
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

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "saved_alert")
class AlertEntity {

    @Id
    private UUID id;
    @Column(name = "user_id")
    private UUID userId;
    private String name;
    @Enumerated(EnumType.STRING)
    private Level level;
    private double latitude;
    private double longitude;
    @Column(name = "radius_meters")
    private int radiusMeters;
    @Column(name = "time_from")
    private LocalTime timeFrom;
    @Column(name = "time_to")
    private LocalTime timeTo;
    @Column(name = "created_at")
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "saved_alert_activity", joinColumns = @JoinColumn(name = "alert_id"))
    @Column(name = "activity")
    @Enumerated(EnumType.STRING)
    private Set<Activity> activities = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "saved_alert_day", joinColumns = @JoinColumn(name = "alert_id"))
    @Column(name = "day_of_week")
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> days = new HashSet<>();

    protected AlertEntity() {
    }

    static AlertEntity of(SavedAlert alert, Instant now) {
        var entity = new AlertEntity();
        entity.id = alert.id();
        entity.userId = alert.userId();
        entity.createdAt = now;
        entity.update(alert);
        return entity;
    }

    void update(SavedAlert alert) {
        name = alert.name();
        level = alert.level();
        latitude = alert.latitude();
        longitude = alert.longitude();
        radiusMeters = alert.radiusMeters();
        timeFrom = alert.from();
        timeTo = alert.to();
        activities.clear();
        activities.addAll(alert.activities());
        days.clear();
        days.addAll(alert.days());
    }

    SavedAlert toDomain() {
        return new SavedAlert(id, userId, name, activities.isEmpty() ? Set.of() : EnumSet.copyOf(activities), level,
                latitude, longitude, radiusMeters, days.isEmpty() ? Set.of() : EnumSet.copyOf(days), timeFrom, timeTo);
    }
}
