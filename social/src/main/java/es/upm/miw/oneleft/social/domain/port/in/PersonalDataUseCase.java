package es.upm.miw.oneleft.social.domain.port.in;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What this service keeps of a person (HU-045): the copy they can download (right of access and portability) and the
 * erasure of their account (right to erasure). The tables come by name, each one a list of rows.
 */
public interface PersonalDataUseCase {

    Map<String, List<Map<String, Object>>> export(UUID userId);

    /**
     * Removes what only concerns them and makes anonymous what is shared with other people. Repeating it changes
     * nothing.
     */
    void erase(UUID userId);
}
