package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.CheckIn;
import es.upm.miw.oneleft.plans.domain.model.DeviceLocation;

import java.util.List;
import java.util.UUID;

/** Check-in at the meeting point (HU-025) and who has arrived, for the group of the plan. */
public interface CheckInUseCase {

    /** Checks in whoever is at the meeting point; a second check-in keeps the first one. */
    CheckIn checkIn(UUID planId, UUID userId, DeviceLocation location);

    /** Who has arrived, first to last, for someone of the group. */
    List<CheckIn> checkIns(UUID planId, UUID userId);
}
