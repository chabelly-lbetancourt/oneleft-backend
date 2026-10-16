package es.upm.miw.oneleft.social.infrastructure.seed;

import es.upm.miw.oneleft.social.domain.model.FeedAudience;
import es.upm.miw.oneleft.social.domain.model.Follow;
import es.upm.miw.oneleft.social.domain.model.FollowStatus;
import es.upm.miw.oneleft.social.domain.model.Person;
import es.upm.miw.oneleft.social.domain.model.Visibility;
import es.upm.miw.oneleft.social.domain.port.out.Follows;
import es.upm.miw.oneleft.social.domain.port.out.People;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Demo people for the dev and pre environments, with the fixed ids of the demo plans: Diego, Marta and Javier have
 * public profiles and can be followed from their plans. Diego already follows Ana and Marta is waiting for her answer.
 * Lucía is left out, so that following and the requests can be tried with her. Existing rows are left untouched.
 */
@Component
@Profile(DemoSocialSeeder.PROFILE)
public class DemoSocialSeeder implements ApplicationRunner {

    /** Explicit opt-in, and never together with production. */
    public static final String PROFILE = "seed & !pro";

    static final Person ANA = new Person(UUID.fromString("a624d063-bd1e-442d-b52d-8de2df356c13"), "Ana Pruebas",
            Visibility.PRIVATE, FeedAudience.FOLLOWERS);
    static final Person DIEGO = new Person(UUID.fromString("5d1c7c2e-2f0b-4d6e-9a43-0c1e7a9b0002"), "Diego",
            Visibility.PUBLIC, FeedAudience.FOLLOWERS);
    static final Person MARTA = new Person(UUID.fromString("5d1c7c2e-2f0b-4d6e-9a43-0c1e7a9b0003"), "Marta",
            Visibility.PUBLIC, FeedAudience.FOLLOWERS);
    static final Person JAVIER = new Person(UUID.fromString("5d1c7c2e-2f0b-4d6e-9a43-0c1e7a9b0004"), "Javier",
            Visibility.PUBLIC, FeedAudience.FOLLOWERS);

    private static final Logger log = LoggerFactory.getLogger(DemoSocialSeeder.class);

    private final People people;
    private final Follows follows;
    private final Clock clock;

    public DemoSocialSeeder(People people, Follows follows, Clock clock) {
        this.people = people;
        this.follows = follows;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var created = List.of(ANA, DIEGO, MARTA, JAVIER).stream()
                .filter(person -> people.find(person.userId()).isEmpty())
                .map(people::save).count();
        var yesterday = clock.instant().minus(Duration.ofDays(1));
        seed(new Follow(DIEGO.userId(), ANA.userId(), FollowStatus.ACCEPTED, yesterday, yesterday));
        seed(new Follow(MARTA.userId(), ANA.userId(), FollowStatus.PENDING, yesterday, null));
        log.info("Seed: {} demo people created, {} already existed", created, 4 - created);
    }

    private void seed(Follow follow) {
        if (follows.find(follow.followerId(), follow.followeeId()).isEmpty()) {
            follows.save(follow);
        }
    }
}
