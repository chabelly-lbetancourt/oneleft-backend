package es.upm.miw.oneleft.plans.domain.port.out;

import es.upm.miw.oneleft.plans.domain.model.CheckIn;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Output port to the check-ins (HU-025): the attendance of each person to each plan. */
public interface CheckInRepository {

    Optional<CheckIn> find(UUID planId, UUID userId);

    CheckIn save(CheckIn checkIn);

    /** The check-ins of a plan, first to last. */
    List<CheckIn> findByPlan(UUID planId);
}
