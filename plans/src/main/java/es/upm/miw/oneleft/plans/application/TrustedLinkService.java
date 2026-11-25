package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.IssuedTrustedLink;
import es.upm.miw.oneleft.plans.domain.model.JoinRejectedException;
import es.upm.miw.oneleft.plans.domain.model.MemoryForbiddenException;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.PlanStatus;
import es.upm.miw.oneleft.plans.domain.model.TrustedLink;
import es.upm.miw.oneleft.plans.domain.model.TrustedLinkNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.TrustedLinkToken;
import es.upm.miw.oneleft.plans.domain.model.TrustedPlanView;
import es.upm.miw.oneleft.plans.domain.port.in.TrustedLinksUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import es.upm.miw.oneleft.plans.domain.port.out.TrustedLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Links for trusted contacts (HU-034). Only who goes to a plan creates them, and only for themselves; whoever opens
 * one learns nothing unless the link is valid right now.
 */
@Service
public class TrustedLinkService implements TrustedLinksUseCase {

    private final PlanRepository plans;
    private final TrustedLinkRepository links;
    private final Clock clock;

    public TrustedLinkService(PlanRepository plans, TrustedLinkRepository links, Clock clock) {
        this.plans = plans;
        this.links = links;
        this.clock = clock;
    }

    @Override
    @Transactional
    public IssuedTrustedLink create(UUID planId, UUID userId) {
        var now = clock.instant();
        var plan = plans.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));
        if (!plan.isMember(userId)) {
            throw new MemoryForbiddenException(MemoryForbiddenException.NOT_PARTICIPANT,
                    "Only the organizer and the participants of a plan can share it with a trusted contact");
        }
        // By the time of the plan and not by its status, which the lifecycle task updates a little later (HU-007)
        if (plan.status() == PlanStatus.CANCELLED || !plan.endsAt().isAfter(now)) {
            throw new JoinRejectedException("trustedLink.planOver", "The plan is over or was cancelled");
        }
        links.lockPlan(planId);
        if (links.findActive(planId, userId, now).size() >= TrustedLink.MAX_PER_PERSON) {
            throw new JoinRejectedException("trustedLink.limit", "Each person can have up to "
                    + TrustedLink.MAX_PER_PERSON + " links for a plan: revoke one first");
        }
        var token = TrustedLinkToken.random();
        return new IssuedTrustedLink(links.save(TrustedLink.of(plan, userId, token, now)), token);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrustedLink> mine(UUID planId, UUID userId) {
        return links.findActive(planId, userId, clock.instant());
    }

    @Override
    @Transactional
    public void revoke(UUID planId, UUID userId, UUID linkId) {
        links.delete(linkId, planId, userId);
    }

    /**
     * Every reason not to show the plan ends in the same exception. The creator must still be in the plan: if they
     * left, they are not going, and their contact has no reason to see it any more.
     */
    @Override
    @Transactional(readOnly = true)
    public TrustedPlanView open(String token) {
        if (!TrustedLinkToken.isWellFormed(token)) {
            throw new TrustedLinkNotFoundException();
        }
        var now = clock.instant();
        var link = links.findByTokenHash(TrustedLinkToken.hash(token)).filter(found -> found.isActive(now))
                .orElseThrow(TrustedLinkNotFoundException::new);
        return plans.findById(link.planId()).filter(plan -> plan.isMember(link.userId()))
                .map(plan -> TrustedPlanView.of(plan, link))
                .orElseThrow(TrustedLinkNotFoundException::new);
    }

    @Override
    @Transactional
    public int purgeExpired() {
        return links.deleteExpired(clock.instant());
    }
}
