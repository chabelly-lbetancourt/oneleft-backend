package es.upm.miw.oneleft.notifications.domain.port.out;

import java.util.UUID;

/** Who blocked whom (HU-033), as far as the notices are concerned. */
@FunctionalInterface
public interface Blocks {

    /** Whether either of them blocked the other one. */
    boolean between(UUID userId, UUID otherId);
}
