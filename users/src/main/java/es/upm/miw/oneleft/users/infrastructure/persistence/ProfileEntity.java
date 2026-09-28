package es.upm.miw.oneleft.users.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "profile")
public class ProfileEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "zone_name", length = 60)
    private String zoneName;

    @Column(name = "zone_latitude", precision = 5, scale = 2)
    private BigDecimal zoneLatitude;

    @Column(name = "zone_longitude", precision = 6, scale = 2)
    private BigDecimal zoneLongitude;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_hobby", joinColumns = @JoinColumn(name = "user_id"))
    private List<HobbyEmbeddable> hobbies = new ArrayList<>();

    protected ProfileEntity() {
    }

    ProfileEntity(UUID userId) {
        this.userId = userId;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = OffsetDateTime.now();
    }

    UUID getUserId() {
        return userId;
    }

    String getDisplayName() {
        return displayName;
    }

    void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    String getZoneName() {
        return zoneName;
    }

    BigDecimal getZoneLatitude() {
        return zoneLatitude;
    }

    BigDecimal getZoneLongitude() {
        return zoneLongitude;
    }

    void setZone(String name, BigDecimal latitude, BigDecimal longitude) {
        this.zoneName = name;
        this.zoneLatitude = latitude;
        this.zoneLongitude = longitude;
    }

    List<HobbyEmbeddable> getHobbies() {
        return hobbies;
    }
}
