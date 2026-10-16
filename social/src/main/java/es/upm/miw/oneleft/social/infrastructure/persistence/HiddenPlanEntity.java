package es.upm.miw.oneleft.social.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/** A plan someone keeps out of their activity (HU-059). Written with a native insert that ignores a repeated one. */
@Entity
@Table(name = "hidden_plan")
public class HiddenPlanEntity {

    @EmbeddedId
    private Key id;

    @Column(name = "hidden_at", nullable = false)
    private Instant hiddenAt;

    protected HiddenPlanEntity() {
    }

    /** Who ({@code userId}) hides which plan ({@code planId}). */
    @Embeddable
    public record Key(@Column(name = "user_id") UUID userId,
                      @Column(name = "plan_id") UUID planId) implements Serializable {
    }
}
