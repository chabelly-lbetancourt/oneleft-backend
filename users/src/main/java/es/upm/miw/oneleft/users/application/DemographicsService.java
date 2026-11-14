package es.upm.miw.oneleft.users.application;

import es.upm.miw.oneleft.users.domain.model.Demographics;
import es.upm.miw.oneleft.users.domain.model.Gender;
import es.upm.miw.oneleft.users.domain.port.in.AnnounceAgesUseCase;
import es.upm.miw.oneleft.users.domain.port.in.ManageDemographicsUseCase;
import es.upm.miw.oneleft.users.domain.port.out.DemographicsEvents;
import es.upm.miw.oneleft.users.domain.port.out.DemographicsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class DemographicsService implements ManageDemographicsUseCase, AnnounceAgesUseCase {

    private final DemographicsRepository demographics;
    private final DemographicsEvents events;
    private final Clock clock;

    public DemographicsService(DemographicsRepository demographics, DemographicsEvents events, Clock clock) {
        this.demographics = demographics;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Demographics> mine(UUID userId) {
        return demographics.findByUser(userId);
    }

    @Override
    @Transactional
    public Demographics save(UUID userId, LocalDate birthDate, Gender gender) {
        var saved = demographics.findByUser(userId)
                .map(current -> current.update(birthDate, gender, clock))
                .orElseGet(() -> Demographics.first(userId, birthDate, gender, clock));
        var stored = demographics.save(saved);
        announce(stored);
        return stored;
    }

    @Override
    @Transactional(readOnly = true)
    public int announceBirthdays() {
        var today = demographics.bornOn(LocalDate.now(clock));
        today.forEach(this::announce);
        return today.size();
    }

    /** Only gender and age leave this service, never the date of birth. */
    private void announce(Demographics data) {
        events.changed(data.userId(), data.gender(), data.age(clock), clock.instant());
    }
}
