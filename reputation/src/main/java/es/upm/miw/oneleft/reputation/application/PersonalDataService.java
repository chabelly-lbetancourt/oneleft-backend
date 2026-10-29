package es.upm.miw.oneleft.reputation.application;

import es.upm.miw.oneleft.reputation.domain.port.in.PersonalDataUseCase;
import es.upm.miw.oneleft.reputation.domain.port.out.PersonalData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The personal data of this service (HU-045): what a person can download and what their erasure removes. */
@Service
@Transactional
public class PersonalDataService implements PersonalDataUseCase {

    private final PersonalData data;

    public PersonalDataService(PersonalData data) {
        this.data = data;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<Map<String, Object>>> export(UUID userId) {
        return data.of(userId);
    }

    @Override
    public void erase(UUID userId) {
        data.erase(userId);
    }
}
