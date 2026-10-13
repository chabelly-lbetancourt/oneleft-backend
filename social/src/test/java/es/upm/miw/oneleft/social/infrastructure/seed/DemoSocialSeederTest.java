package es.upm.miw.oneleft.social.infrastructure.seed;

import es.upm.miw.oneleft.social.InMemorySocial;
import es.upm.miw.oneleft.social.domain.model.FollowStatus;
import es.upm.miw.oneleft.social.domain.model.Visibility;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class DemoSocialSeederTest {

    private final InMemorySocial social = new InMemorySocial();
    private final DemoSocialSeeder seeder = new DemoSocialSeeder(social, social, Clock.systemUTC());

    @Test
    void anaIsPrivateWithAFollowerAndARequestAndTheOthersArePublic() {
        seeder.run(null);

        assertThat(social.find(DemoSocialSeeder.ANA.userId()).orElseThrow().visibility())
                .isEqualTo(Visibility.PRIVATE);
        assertThat(social.find(DemoSocialSeeder.JAVIER.userId()).orElseThrow().visibility())
                .isEqualTo(Visibility.PUBLIC);
        var ana = DemoSocialSeeder.ANA.userId();
        assertThat(social.find(DemoSocialSeeder.DIEGO.userId(), ana).orElseThrow().status())
                .isEqualTo(FollowStatus.ACCEPTED);
        assertThat(social.find(DemoSocialSeeder.MARTA.userId(), ana).orElseThrow().status())
                .isEqualTo(FollowStatus.PENDING);
    }

    @Test
    void whatAlreadyExistsIsLeftUntouched() {
        seeder.run(null);
        var ana = social.save(social.find(DemoSocialSeeder.ANA.userId()).orElseThrow()
                .withVisibility(Visibility.PUBLIC));
        social.delete(DemoSocialSeeder.MARTA.userId(), ana.userId());
        social.save(social.find(DemoSocialSeeder.DIEGO.userId(), ana.userId()).orElseThrow());

        seeder.run(null);

        assertThat(social.find(ana.userId()).orElseThrow().visibility()).isEqualTo(Visibility.PUBLIC);
        assertThat(social.people).hasSize(4);
        assertThat(social.follows).hasSize(2);
    }
}
