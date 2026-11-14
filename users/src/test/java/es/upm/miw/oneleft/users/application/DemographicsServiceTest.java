package es.upm.miw.oneleft.users.application;

import es.upm.miw.oneleft.users.domain.model.Demographics;
import es.upm.miw.oneleft.users.domain.model.Gender;
import es.upm.miw.oneleft.users.domain.model.ValidationException;
import es.upm.miw.oneleft.users.domain.port.out.DemographicsEvents;
import es.upm.miw.oneleft.users.domain.port.out.DemographicsRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemographicsServiceTest {

    private static final UUID USER = UUID.randomUUID();
    private final Map<UUID, Demographics> data = new HashMap<>();
    private final DemographicsRepository repository = new DemographicsRepository() {
        @Override
        public Optional<Demographics> findByUser(UUID userId) {
            return Optional.ofNullable(data.get(userId));
        }

        @Override
        public Demographics save(Demographics demographics) {
            data.put(demographics.userId(), demographics);
            return demographics;
        }

        @Override
        public List<Demographics> bornOn(LocalDate day) {
            return data.values().stream().filter(d -> d.birthDate().getMonth() == day.getMonth()
                    && d.birthDate().getDayOfMonth() == day.getDayOfMonth()).toList();
        }
    };
    private final List<String> announced = new ArrayList<>();
    private final DemographicsEvents events = (userId, gender, age, at) ->
            announced.add(userId + " " + gender + " " + age);
    private final DemographicsService service = new DemographicsService(repository, events,
            Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC));

    @Test
    void nothingIsSetUntilThePersonSetsIt() {
        assertThat(service.mine(USER)).isEmpty();

        service.save(USER, LocalDate.of(1995, 4, 12), Gender.WOMAN);

        assertThat(service.mine(USER)).hasValueSatisfying(found -> {
            assertThat(found.gender()).isEqualTo(Gender.WOMAN);
            assertThat(found.changes()).isZero();
        });
    }

    @Test
    void changingTheDateCountsAndIsLimitedWhileTheGenderIsFree() {
        service.save(USER, LocalDate.of(1995, 4, 12), Gender.WOMAN);
        service.save(USER, LocalDate.of(1995, 4, 12), Gender.NON_BINARY);
        assertThat(service.mine(USER).orElseThrow().changes()).isZero();

        service.save(USER, LocalDate.of(1994, 4, 12), Gender.NON_BINARY);
        service.save(USER, LocalDate.of(1993, 4, 12), Gender.NON_BINARY);

        assertThatThrownBy(() -> service.save(USER, LocalDate.of(1992, 4, 12), Gender.NON_BINARY))
                .isInstanceOf(ValidationException.class);
        assertThat(service.mine(USER).orElseThrow().birthDate()).isEqualTo(LocalDate.of(1993, 4, 12));
    }

    @Test
    void aMinorCannotSetItAndNothingIsSaved() {
        assertThatThrownBy(() -> service.save(USER, LocalDate.of(2015, 1, 1), Gender.MAN))
                .isInstanceOf(ValidationException.class);
        assertThat(service.mine(USER)).isEmpty();
    }

    @Test
    void savingAnnouncesTheGenderAndTheAgeButNeverTheDateOfBirth() {
        service.save(USER, LocalDate.of(1995, 11, 17), Gender.WOMAN);

        assertThat(announced).containsExactly(USER + " WOMAN 30");
    }

    @Test
    void theBirthdayAnnouncesTheNewAgeOfWhoHasItToday() {
        var other = UUID.randomUUID();
        service.save(USER, LocalDate.of(1990, 11, 16), Gender.MAN);
        service.save(other, LocalDate.of(1990, 11, 17), Gender.MAN);
        announced.clear();

        assertThat(service.announceBirthdays()).isEqualTo(1);

        assertThat(announced).containsExactly(USER + " MAN 36");
    }
}
