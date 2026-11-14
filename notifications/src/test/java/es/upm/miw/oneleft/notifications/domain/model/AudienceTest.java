package es.upm.miw.oneleft.notifications.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AudienceTest {

    private static final UUID USER = UUID.randomUUID();

    @Test
    void aPlanForEveryoneFitsEvenWhoIsUnknown() {
        assertThat(Audience.EVERYONE.fits(null)).isTrue();
        assertThat(Audience.EVERYONE.fits(new MemberAudience(USER, "PREFER_NOT_TO_SAY", 40))).isTrue();
    }

    @Test
    void aGenderOrAnAgeLimitNeedsKnownData() {
        assertThat(new Audience(AudienceGender.WOMAN, null, null).fits(null)).isFalse();
        assertThat(new Audience(AudienceGender.ALL, 30, null).fits(null)).isFalse();
    }

    @Test
    void theAgeLimitsAreInclusive() {
        var audience = new Audience(AudienceGender.ALL, 25, 35);

        assertThat(audience.fits(new MemberAudience(USER, "MAN", 25))).isTrue();
        assertThat(audience.fits(new MemberAudience(USER, "MAN", 35))).isTrue();
        assertThat(audience.fits(new MemberAudience(USER, "MAN", 24))).isFalse();
        assertThat(audience.fits(new MemberAudience(USER, "MAN", 36))).isFalse();
    }

    @Test
    void nonBinaryPeopleFitOnlyThePlansForEveryone() {
        var nonBinary = new MemberAudience(USER, "NON_BINARY", 30);

        assertThat(new Audience(AudienceGender.MAN, null, null).fits(nonBinary)).isFalse();
        assertThat(new Audience(AudienceGender.WOMAN, null, null).fits(nonBinary)).isFalse();
        assertThat(new Audience(AudienceGender.ALL, 18, 40).fits(nonBinary)).isTrue();
    }
}
