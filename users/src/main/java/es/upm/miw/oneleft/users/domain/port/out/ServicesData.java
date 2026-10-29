package es.upm.miw.oneleft.users.domain.port.out;

import java.util.Map;
import java.util.UUID;

/** The data that the other services keep of a person (HU-045), by service. */
public interface ServicesData {

    /** @throws es.upm.miw.oneleft.users.domain.model.AccountException if any of them cannot answer */
    Map<String, Object> of(UUID userId);
}
