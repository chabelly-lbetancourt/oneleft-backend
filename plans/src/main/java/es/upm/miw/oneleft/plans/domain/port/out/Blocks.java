package es.upm.miw.oneleft.plans.domain.port.out;

import java.util.UUID;

/** Who blocked whom (HU-033), as far as the plans are concerned. */
@FunctionalInterface
public interface Blocks {

    /** Whether either of them blocked the other one. */
    boolean between(UUID userId, UUID otherId);
}
