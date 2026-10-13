package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Interest;
import es.upm.miw.oneleft.plans.domain.model.Level;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
class InterestEmbeddable {

    @Enumerated(EnumType.STRING)
    private Activity activity;
    @Enumerated(EnumType.STRING)
    private Level level;

    protected InterestEmbeddable() {
    }

    InterestEmbeddable(Interest interest) {
        this.activity = interest.activity();
        this.level = interest.level();
    }

    Interest toDomain() {
        return new Interest(activity, level);
    }
}
