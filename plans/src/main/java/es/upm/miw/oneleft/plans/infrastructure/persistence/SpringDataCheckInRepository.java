package es.upm.miw.oneleft.plans.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataCheckInRepository extends JpaRepository<CheckInEntity, CheckInEntity.Key> {

    List<CheckInEntity> findByIdPlanIdOrderByCheckedInAt(UUID planId);
}
