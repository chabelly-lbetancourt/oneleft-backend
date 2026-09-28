package es.upm.miw.oneleft.plans.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface SpringDataPlanRepository extends JpaRepository<PlanEntity, UUID> {

    List<PlanEntity> findByOrganizerIdAndStartsAtAfterOrderByStartsAt(UUID organizerId, Instant from);
}
