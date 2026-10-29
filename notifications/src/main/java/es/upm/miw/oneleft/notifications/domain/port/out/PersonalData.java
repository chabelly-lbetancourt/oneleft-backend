package es.upm.miw.oneleft.notifications.domain.port.out;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The personal data of this service, in its storage (HU-045). */
public interface PersonalData {

    Map<String, List<Map<String, Object>>> of(UUID userId);

    void erase(UUID userId);
}
