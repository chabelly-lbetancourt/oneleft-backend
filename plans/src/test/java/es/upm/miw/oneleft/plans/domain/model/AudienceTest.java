package es.upm.miw.oneleft.plans.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.COURTS;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-070: for whom a plan is. */
class AudienceTest {

    private static AudienceMember person(PersonGender gender, int age) {
        return new AudienceMember(gender, age);
    }

    @Test
    void aPlanForEveryoneFitsEveryoneIncludingNonBinaryAndWhoPreferNotToSay() {
        for (var gender : PersonGender.values()) {
            assertThat(Audience.EVERYONE.fits(person(gender, 30))).as(gender.name()).isTrue();
        }
        assertThat(Audience.EVERYONE.isEveryone()).isTrue();
        assertThat(new Audience(null, null, null).isEveryone()).as("no gender means everyone").isTrue();
    }

    @Test
    void aPlanForOneGenderFitsOnlyThatGender() {
        var women = new Audience(AudienceGender.WOMAN, null, null);
        var men = new Audience(AudienceGender.MAN, null, null);

        assertThat(women.fits(person(PersonGender.WOMAN, 40))).isTrue();
        assertThat(women.fits(person(PersonGender.MAN, 40))).isFalse();
        assertThat(women.fits(person(PersonGender.NON_BINARY, 40))).isFalse();
        assertThat(women.fits(person(PersonGender.PREFER_NOT_TO_SAY, 40))).isFalse();
        assertThat(men.fits(person(PersonGender.MAN, 40))).isTrue();
        assertThat(men.fits(person(PersonGender.WOMAN, 40))).isFalse();
        assertThat(women.isEveryone()).isFalse();
    }

    @Test
    void theAgeRangeIsInclusiveAndEitherEndIsOptional() {
        var range = new Audience(AudienceGender.ALL, 25, 40);

        assertThat(range.fits(person(PersonGender.MAN, 24))).isFalse();
        assertThat(range.fits(person(PersonGender.MAN, 25))).isTrue();
        assertThat(range.fits(person(PersonGender.MAN, 40))).isTrue();
        assertThat(range.fits(person(PersonGender.MAN, 41))).isFalse();
        assertThat(new Audience(AudienceGender.ALL, 30, null).fits(person(PersonGender.MAN, 90))).isTrue();
        assertThat(new Audience(AudienceGender.ALL, null, 30).fits(person(PersonGender.MAN, 18))).isTrue();
        assertThat(new Audience(AudienceGender.WOMAN, 25, 40).fits(person(PersonGender.WOMAN, 33))).isTrue();
        assertThat(new Audience(AudienceGender.WOMAN, 25, 40).fits(person(PersonGender.MAN, 33))).isFalse();
    }

    @Test
    void agesMustBeOfAgeAndInOrder() {
        for (var ages : new Integer[][]{{17, null}, {null, 17}, {null, 121}, {41, 40}}) {
            assertThatThrownBy(() -> new Audience(AudienceGender.ALL, ages[0], ages[1]))
                    .isInstanceOf(ValidationException.class);
        }
        assertThat(new Audience(AudienceGender.ALL, 18, 120).minAge()).isEqualTo(18);
    }

    @Test
    void aPublishedPlanKeepsItsAudienceThroughEveryChange() {
        var audience = new Audience(AudienceGender.WOMAN, 25, 40);
        var plan = Plan.publish(ana(), Activity.PADEL, null, "Pádel", null, COURTS, NOW.plus(Duration.ofHours(2)), 2,
                null, null, null, false, audience, CLOCK);

        assertThat(plan.audience()).isEqualTo(audience);
        assertThat(plan.join(java.util.UUID.randomUUID(), "Lucía", CLOCK).audience()).isEqualTo(audience);
        assertThat(Plan.publish(ana(), Activity.PADEL, "Pádel", null, COURTS, NOW.plus(Duration.ofHours(2)), 2, null,
                CLOCK).audience()).isEqualTo(Audience.EVERYONE);
    }
}
