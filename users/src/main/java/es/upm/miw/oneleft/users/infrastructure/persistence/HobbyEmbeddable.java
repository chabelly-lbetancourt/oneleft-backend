package es.upm.miw.oneleft.users.infrastructure.persistence;

import es.upm.miw.oneleft.users.domain.model.Activity;
import es.upm.miw.oneleft.users.domain.model.Level;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class HobbyEmbeddable {

    @Enumerated(EnumType.STRING)
    @Column(name = "activity", nullable = false, length = 30)
    private Activity activity;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 20)
    private Level level;

    protected HobbyEmbeddable() {
    }

    HobbyEmbeddable(Activity activity, Level level) {
        this.activity = activity;
        this.level = level;
    }

    Activity activity() {
        return activity;
    }

    Level level() {
        return level;
    }
}
