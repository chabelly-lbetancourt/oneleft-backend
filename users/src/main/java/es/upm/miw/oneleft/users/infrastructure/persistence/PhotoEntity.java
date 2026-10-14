package es.upm.miw.oneleft.users.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profile_photo")
public class PhotoEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "object_key", nullable = false, length = 120)
    private String objectKey;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PhotoEntity() {
    }

    PhotoEntity(UUID userId, String objectKey, Instant updatedAt) {
        this.userId = userId;
        this.objectKey = objectKey;
        this.updatedAt = updatedAt;
    }

    UUID getUserId() {
        return userId;
    }

    String getObjectKey() {
        return objectKey;
    }

    void replace(String objectKey, Instant updatedAt) {
        this.objectKey = objectKey;
        this.updatedAt = updatedAt;
    }
}
