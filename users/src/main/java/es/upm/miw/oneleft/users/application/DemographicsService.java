package es.upm.miw.oneleft.users.application;

import es.upm.miw.oneleft.users.domain.model.Demographics;
import es.upm.miw.oneleft.users.domain.model.Gender;
import es.upm.miw.oneleft.users.domain.port.in.ManageDemographicsUseCase;
import es.upm.miw.oneleft.users.domain.port.out.DemographicsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class DemographicsService implements ManageDemographicsUseCase {

    private final DemographicsRepository demographics;
    private final Clock clock;

    public DemographicsService(DemographicsRepository demographics, Clock clock) {
        this.demographics = demographics;
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
        return demographics.save(saved);
    }
}
