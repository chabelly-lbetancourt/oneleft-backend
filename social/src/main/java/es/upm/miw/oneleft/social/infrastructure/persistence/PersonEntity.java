package es.upm.miw.oneleft.social.infrastructure.persistence;

import es.upm.miw.oneleft.social.domain.model.FeedAudience;
import es.upm.miw.oneleft.social.domain.model.Person;
import es.upm.miw.oneleft.social.domain.model.Visibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "person")
public class PersonEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Visibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "feed_audience", nullable = false, length = 10)
    private FeedAudience feedAudience;

    protected PersonEntity() {
    }

    static PersonEntity of(Person person) {
        var entity = new PersonEntity();
        entity.userId = person.userId();
        entity.name = person.name();
        entity.visibility = person.visibility();
        entity.feedAudience = person.feedAudience();
        return entity;
    }

    Person toDomain() {
        return new Person(userId, name, visibility, feedAudience);
    }
}
