package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.MemoryKey;
import es.upm.miw.oneleft.plans.domain.model.MemoryStorageUnavailableException;
import es.upm.miw.oneleft.plans.domain.port.in.PersonalDataUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.MemoryStorage;
import es.upm.miw.oneleft.plans.domain.port.out.PersonalData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The personal data of this service (HU-045): what a person can download and what their erasure removes. */
@Service
@Transactional
public class PersonalDataService implements PersonalDataUseCase {

    private static final Logger log = LoggerFactory.getLogger(PersonalDataService.class);

    private final PersonalData data;
    private final MemoryStorage storage;

    public PersonalDataService(PersonalData data, MemoryStorage storage) {
        this.data = data;
        this.storage = storage;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<Map<String, Object>>> export(UUID userId) {
        return data.of(userId);
    }

    @Override
    public void erase(UUID userId) {
        data.erase(userId);
        erasePhotos(userId);
    }

    /**
     * The photos they added to the albums (HU-052). The erasure arrives as an event that is retried until it works:
     * if the storage does not answer, the rest of their data is erased all the same and the photos, which nobody can
     * reach any more, are left to be deleted by hand, with this error as the notice.
     */
    private void erasePhotos(UUID userId) {
        var folder = MemoryKey.prefixOf(userId);
        try {
            storage.deleteUnder(folder);
        } catch (MemoryStorageUnavailableException exception) {
            log.error("The photos of a deleted account are still in the storage, under {}: delete them by hand",
                    folder, exception);
        }
    }
}
