package es.upm.miw.oneleft.users.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-070: date of birth (18+) and gender; others only ever get the age band. */
class DemographicsTest {

    /** 16/11/2026 */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC);
    private static final UUID USER = UUID.randomUUID();

    private static void invalid(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOf(ValidationException.class)
                .satisfies(error -> assertThat(((ValidationException) error).code()).isEqualTo(code));
    }

    @Test
    void theAgeIsWorkedOutFromTheDateAndTheDateOfAnAdultIsAccepted() {
        var born = Demographics.first(USER, LocalDate.of(2008, 11, 16), Gender.WOMAN, CLOCK);

        assertThat(born.age(CLOCK)).isEqualTo(18);
        assertThat(Demographics.first(USER, LocalDate.of(1990, 11, 17), Gender.MAN, CLOCK).age(CLOCK))
                .as("the day after the birthday of this year, one year less").isEqualTo(35);
    }

    @Test
    void anyoneUnder18OrBornInTheFutureOrTooLongAgoIsRejected() {
        invalid(() -> Demographics.first(USER, LocalDate.of(2008, 11, 17), Gender.MAN, CLOCK), "demographics.minor");
        invalid(() -> Demographics.first(USER, LocalDate.of(2026, 11, 17), Gender.MAN, CLOCK), "demographics.birthDate");
        invalid(() -> Demographics.first(USER, LocalDate.of(1890, 1, 1), Gender.MAN, CLOCK), "demographics.birthDate");
        invalid(() -> Demographics.first(USER, null, Gender.MAN, CLOCK), "demographics.missing");
        invalid(() -> new Demographics(USER, LocalDate.of(1990, 1, 1), null, 0), "demographics.missing");
    }

    @Test
    void theBandsHideTheExactAge() {
        assertThat(AgeBand.of(18).label()).isEqualTo("18–24");
        assertThat(AgeBand.of(24).label()).isEqualTo("18–24");
        assertThat(AgeBand.of(25).label()).isEqualTo("25–34");
        assertThat(AgeBand.of(44).label()).isEqualTo("35–44");
        assertThat(AgeBand.of(64).label()).isEqualTo("55–64");
        assertThat(AgeBand.of(65).label()).isEqualTo("65+");
        assertThat(AgeBand.of(99).to()).isNull();
        invalid(() -> AgeBand.of(17), "demographics.minor");
        assertThat(Demographics.first(USER, LocalDate.of(1995, 4, 12), Gender.WOMAN, CLOCK).ageBand(CLOCK).label())
                .isEqualTo("25–34");
    }

    @Test
    void theDateCanOnlyBeChangedAFewTimesButTheGenderAlways() {
        var first = Demographics.first(USER, LocalDate.of(1990, 1, 1), Gender.WOMAN, CLOCK);

        var gender = first.update(LocalDate.of(1990, 1, 1), Gender.NON_BINARY, CLOCK);
        assertThat(gender.changes()).isZero();
        assertThat(gender.gender()).isEqualTo(Gender.NON_BINARY);
        var one = first.update(LocalDate.of(1991, 1, 1), Gender.WOMAN, CLOCK);
        var two = one.update(LocalDate.of(1992, 1, 1), Gender.WOMAN, CLOCK);
        assertThat(two.changes()).isEqualTo(Demographics.MAX_BIRTH_DATE_CHANGES);
        invalid(() -> two.update(LocalDate.of(1993, 1, 1), Gender.WOMAN, CLOCK), "demographics.birthDateLocked");
        assertThat(two.update(LocalDate.of(1992, 1, 1), Gender.MAN, CLOCK).gender()).isEqualTo(Gender.MAN);
    }

    @Test
    void aChangeCannotMakeThePersonAMinor() {
        var adult = Demographics.first(USER, LocalDate.of(1990, 1, 1), Gender.MAN, CLOCK);

        invalid(() -> adult.update(LocalDate.of(2015, 1, 1), Gender.MAN, CLOCK), "demographics.minor");
        invalid(() -> adult.update(null, Gender.MAN, CLOCK), "demographics.missing");
    }
}
