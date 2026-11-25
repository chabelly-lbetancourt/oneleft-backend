package es.upm.miw.oneleft.plans.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-046: what makes the place of a plan accessible, and the search that asks for it. */
class AccessibilityFeatureTest {

    private static final double LAT = 40.3912;
    private static final double LON = -3.6287;

    private static PlanPublished published(Set<AccessibilityFeature> accessibility) {
        return new PlanPublished(UUID.randomUUID(), UUID.randomUUID(), Activity.CINEMA, "Plan", "Place", LAT, LON,
                NOW.plusSeconds(3600), 1, null, NOW, Audience.EVERYONE, accessibility);
    }

    @Test
    void theFeaturesComeByNameWithoutRepeatsAndInTheOrderOfTheList() {
        assertThat(AccessibilityFeature.of(List.of("ACCESSIBLE_TOILET", "WHEELCHAIR_ACCESSIBLE", "ACCESSIBLE_TOILET")))
                .containsExactly(AccessibilityFeature.WHEELCHAIR_ACCESSIBLE, AccessibilityFeature.ACCESSIBLE_TOILET);
        assertThat(AccessibilityFeature.of(null)).isEmpty();
        assertThat(AccessibilityFeature.of(List.of())).isEmpty();
    }

    @Test
    void aNameThatIsNotInTheListIsRejected() {
        for (var unknown : Arrays.asList("BLIND_FRIENDLY", "wheelchair_accessible", "", null)) {
            assertThatThrownBy(() -> AccessibilityFeature.of(Arrays.asList("STEP_FREE_ACCESS", unknown)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("WHEELCHAIR_ACCESSIBLE")
                    .extracting(thrown -> ((ValidationException) thrown).code()).isEqualTo("plan.accessibility");
        }
    }

    @Test
    void aPlaceKeepsItsFeaturesInTheOrderOfTheListAndHasNoneByDefault() {
        var given = new java.util.HashSet<>(Set.of(AccessibilityFeature.ACCESSIBLE_PARKING,
                AccessibilityFeature.WHEELCHAIR_ACCESSIBLE));
        var place = new MeetingPoint("Cinema", LAT, LON, given);
        given.clear();

        assertThat(place.accessibility()).containsExactly(AccessibilityFeature.WHEELCHAIR_ACCESSIBLE,
                AccessibilityFeature.ACCESSIBLE_PARKING);
        assertThat(new MeetingPoint("Cinema", LAT, LON).accessibility()).isEmpty();
        assertThat(new MeetingPoint("Cinema", LAT, LON, null).accessibility()).isEmpty();
    }

    @Test
    void thePublishedEventCarriesTheFeaturesOfThePlace() {
        var place = new MeetingPoint("Cinema", LAT, LON, Set.of(AccessibilityFeature.STEP_FREE_ACCESS));
        var plan = Plan.publish(new Organizer(UUID.randomUUID(), "Ana"), Activity.CINEMA, "Double session", null,
                place, NOW.plusSeconds(3600), 2, null, CLOCK);

        assertThat(plan.meetingPoint().accessibility()).containsExactly(AccessibilityFeature.STEP_FREE_ACCESS);
        assertThat(plan.publishedEvent().accessibility()).containsExactly(AccessibilityFeature.STEP_FREE_ACCESS);
        // An event from before HU-046 has none
        assertThat(published(null).accessibility()).isEmpty();
    }

    @Test
    void theRealTimeSearchOnlyMatchesThePlacesWithEveryFeatureAskedFor() {
        var anyPlace = new NearbySearch(LAT, LON, 2_000, null, null, UUID.randomUUID());
        var accessible = anyPlace.withAccessibility(Set.of(AccessibilityFeature.WHEELCHAIR_ACCESSIBLE,
                AccessibilityFeature.ACCESSIBLE_TOILET));
        var both = published(Set.of(AccessibilityFeature.WHEELCHAIR_ACCESSIBLE,
                AccessibilityFeature.ACCESSIBLE_TOILET, AccessibilityFeature.ACCESSIBLE_PARKING));
        var onlyOne = published(Set.of(AccessibilityFeature.WHEELCHAIR_ACCESSIBLE));
        var none = published(Set.of());

        assertThat(anyPlace.accessibility()).isEmpty();
        assertThat(anyPlace.matches(both, NOW)).isTrue();
        assertThat(anyPlace.matches(none, NOW)).isTrue();
        assertThat(accessible.matches(both, NOW)).isTrue();
        assertThat(accessible.matches(onlyOne, NOW)).isFalse();
        assertThat(accessible.matches(none, NOW)).isFalse();
    }

    @Test
    void theSearchKeepsWhatItAsksForWhenItLearnsWhoSearches() {
        var search = new NearbySearch(LAT, LON, 2_000, null, null, UUID.randomUUID())
                .withAccessibility(Set.of(AccessibilityFeature.WHEELCHAIR_ACCESSIBLE))
                .forViewer(new AudienceMember(PersonGender.WOMAN, 33));

        assertThat(search.accessibility()).containsExactly(AccessibilityFeature.WHEELCHAIR_ACCESSIBLE);
        assertThat(search.viewerAge()).isEqualTo(33);
        assertThat(search.withAccessibility(null).accessibility()).isEmpty();
    }
}
