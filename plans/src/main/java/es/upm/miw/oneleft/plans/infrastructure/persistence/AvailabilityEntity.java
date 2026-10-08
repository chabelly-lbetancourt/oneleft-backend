package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.domain.model.Availability;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "availability")
class AvailabilityEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;
    private double latitude;
    private double longitude;
    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point location;
    private Instant until;
    @Column(name = "created_at")
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "availability_interest", joinColumns = @JoinColumn(name = "user_id"))
    private List<InterestEmbeddable> interests = new ArrayList<>();

    protected AvailabilityEntity() {
    }

    AvailabilityEntity(Availability availability, Point location, Instant now) {
        this.userId = availability.userId();
        this.latitude = availability.latitude();
        this.longitude = availability.longitude();
        this.location = location;
        this.until = availability.until();
        this.createdAt = now;
        this.interests = availability.interests().stream().map(InterestEmbeddable::new)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    Availability toDomain() {
        return new Availability(userId, latitude, longitude, until,
                interests.stream().map(InterestEmbeddable::toDomain).collect(Collectors.toSet()));
    }
}
