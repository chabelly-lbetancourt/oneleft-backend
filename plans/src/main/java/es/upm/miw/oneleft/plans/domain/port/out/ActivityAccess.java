package es.upm.miw.oneleft.plans.domain.port.out;

import java.util.UUID;

/**
 * Whether whoever is asking may see the plans of someone (HU-048). The social service decides it: it knows who may
 * follow whom.
 */
public interface ActivityAccess {

    /**
     * @throws es.upm.miw.oneleft.plans.domain.model.PersonActivityException {@code NOT_FOUND} if nobody has that id,
     *                                                                        {@code UNAVAILABLE} if it cannot be asked
     */
    boolean visible(UUID userId);
}
