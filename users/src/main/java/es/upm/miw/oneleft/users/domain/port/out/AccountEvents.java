package es.upm.miw.oneleft.users.domain.port.out;

import java.time.Instant;
import java.util.UUID;

/** What the other services learn of an account: that it was deleted (HU-045), so that they erase their part. */
public interface AccountEvents {

    void deleted(UUID userId, Instant at);
}
