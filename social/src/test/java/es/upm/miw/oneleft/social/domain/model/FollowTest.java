package es.upm.miw.oneleft.social.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-047: a public profile is followed at once, a private one receives a request. */
class FollowTest {

    private static final Instant NOW = Instant.parse("2026-10-13T16:00:00.123456789Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Person lucia = new Person(UUID.randomUUID(), "Lucía", Visibility.PRIVATE, FeedAudience.FOLLOWERS);
    private final Person ana = new Person(UUID.randomUUID(), "Ana", Visibility.PRIVATE, FeedAudience.FOLLOWERS);

    @Test
    void followingAPublicProfileIsAcceptedAtOnce() {
        var follow = Follow.request(lucia, ana.withVisibility(Visibility.PUBLIC), CLOCK);

        assertThat(follow.status()).isEqualTo(FollowStatus.ACCEPTED);
        assertThat(follow.isPending()).isFalse();
        assertThat(follow.since()).isEqualTo(Instant.parse("2026-10-13T16:00:00Z"));
    }

    @Test
    void followingAPrivateProfileIsARequestUntilItIsAccepted() {
        var request = Follow.request(lucia, ana, CLOCK);
        assertThat(request.isPending()).isTrue();
        assertThat(request.acceptedAt()).isNull();
        assertThat(request.since()).isEqualTo(request.requestedAt());

        var later = Clock.fixed(NOW.plusSeconds(60), ZoneOffset.UTC);
        var accepted = request.accept(later);

        assertThat(accepted.status()).isEqualTo(FollowStatus.ACCEPTED);
        assertThat(accepted.requestedAt()).isEqualTo(request.requestedAt());
        assertThat(accepted.since()).isEqualTo(Instant.parse("2026-10-13T16:01:00Z"));
        assertThat(accepted.accept(CLOCK)).isSameAs(accepted);
    }

    @Test
    void nobodyFollowsThemselves() {
        assertThatThrownBy(() -> Follow.request(lucia, lucia, CLOCK))
                .isInstanceOfSatisfying(ValidationException.class,
                        error -> assertThat(error.code()).isEqualTo("follow.self"));
    }

    @Test
    void onlyAnAcceptedFollowHasAnAcceptanceTime() {
        assertThatThrownBy(() -> new Follow(lucia.userId(), ana.userId(), FollowStatus.ACCEPTED, NOW, null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new Follow(lucia.userId(), ana.userId(), FollowStatus.PENDING, NOW, NOW))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new Follow(lucia.userId(), null, FollowStatus.PENDING, NOW, null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void pagesHaveOneToFiftyPeople() {
        assertThat(new PageQuery(0, PageQuery.MAX_SIZE).size()).isEqualTo(50);
        assertThatThrownBy(() -> new PageQuery(-1, 20)).isInstanceOfSatisfying(ValidationException.class,
                error -> assertThat(error.code()).isEqualTo("page.invalid"));
        assertThatThrownBy(() -> new PageQuery(0, 51)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new PageQuery(0, 0)).isInstanceOf(ValidationException.class);
    }
}
