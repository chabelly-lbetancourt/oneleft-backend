package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.domain.model.Memory;
import es.upm.miw.oneleft.plans.domain.model.MemoryKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plan_memory")
class MemoryEntity {

    @Id
    private UUID id;
    @Column(name = "plan_id", nullable = false)
    private UUID planId;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "object_key", nullable = false, unique = true, length = MemoryKey.MAX_LENGTH)
    private String objectKey;
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected MemoryEntity() {
    }

    MemoryEntity(Memory memory) {
        this.id = memory.id();
        this.planId = memory.planId();
        this.userId = memory.userId();
        this.objectKey = memory.key();
        this.sizeBytes = memory.size();
        this.uploadedAt = memory.uploadedAt();
    }

    Memory toDomain() {
        return new Memory(id, planId, userId, objectKey, sizeBytes, uploadedAt);
    }
}
