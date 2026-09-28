package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.port.in.PublishPlanCommand;
import es.upm.miw.oneleft.plans.domain.port.in.PublishPlanUseCase;
import es.upm.miw.oneleft.plans.domain.port.in.QueryPlansUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class PlanService implements PublishPlanUseCase, QueryPlansUseCase {

    private final PlanRepository plans;
    private final PlanEventPublisher events;
    private final Clock clock;

    public PlanService(PlanRepository plans, PlanEventPublisher events, Clock clock) {
        this.plans = plans;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Plan publish(PublishPlanCommand command) {
        var plan = plans.save(Plan.publish(command.organizer(), command.activity(), command.title(),
                command.description(), command.meetingPoint(), command.startsAt(), command.spots(), command.level(),
                clock));
        events.publish(plan.publishedEvent());
        return plan;
    }

    @Override
    @Transactional(readOnly = true)
    public Plan plan(UUID planId) {
        return plans.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Plan> upcomingPlansOrganizedBy(UUID organizerId) {
        return plans.findByOrganizerStartingAfter(organizerId, clock.instant());
    }
}
