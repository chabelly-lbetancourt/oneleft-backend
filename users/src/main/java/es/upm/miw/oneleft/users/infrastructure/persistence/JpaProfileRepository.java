package es.upm.miw.oneleft.users.infrastructure.persistence;

import es.upm.miw.oneleft.users.domain.model.ApproximateZone;
import es.upm.miw.oneleft.users.domain.model.Hobby;
import es.upm.miw.oneleft.users.domain.model.Profile;
import es.upm.miw.oneleft.users.domain.port.out.ProfileRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Output adapter: implements the domain port with Spring Data JPA and PostgreSQL.
 */
@Repository
public class JpaProfileRepository implements ProfileRepository {

    private final SpringDataProfileRepository jpa;

    public JpaProfileRepository(SpringDataProfileRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Profile> findById(UUID userId) {
        return jpa.findById(userId).map(JpaProfileRepository::toDomain);
    }

    @Override
    public Profile save(Profile profile) {
        var entity = jpa.findById(profile.userId()).orElseGet(() -> new ProfileEntity(profile.userId()));
        entity.setDisplayName(profile.displayName());
        var zone = profile.zone();
        if (zone == null) {
            entity.setZone(null, null, null);
        } else {
            entity.setZone(zone.name(), BigDecimal.valueOf(zone.latitude()), BigDecimal.valueOf(zone.longitude()));
        }
        entity.getHobbies().clear();
        profile.hobbies().forEach(hobby -> entity.getHobbies().add(new HobbyEmbeddable(hobby.activity(), hobby.level())));
        return toDomain(jpa.save(entity));
    }

    private static Profile toDomain(ProfileEntity entity) {
        var zone = entity.getZoneName() == null ? null : new ApproximateZone(entity.getZoneName(),
                entity.getZoneLatitude().doubleValue(), entity.getZoneLongitude().doubleValue());
        var hobbies = entity.getHobbies().stream().map(h -> new Hobby(h.activity(), h.level())).toList();
        return new Profile(entity.getUserId(), entity.getDisplayName(), zone, hobbies);
    }
}
