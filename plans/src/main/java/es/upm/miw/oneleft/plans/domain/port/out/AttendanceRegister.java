package es.upm.miw.oneleft.plans.domain.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Which plans still have to close their attendance, and that a plan has closed it (once per plan). */
public interface AttendanceRegister {

    /** Plans not cancelled that started before {@code startedBefore} and whose attendance is still open. */
    List<UUID> dueToClose(Instant startedBefore, int limit);

    void markClosed(UUID planId, Instant at);
}
