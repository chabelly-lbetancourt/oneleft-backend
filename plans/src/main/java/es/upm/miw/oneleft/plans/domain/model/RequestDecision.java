package es.upm.miw.oneleft.plans.domain.model;

import java.util.List;

/** The outcome of deciding on a request (HU-069): the new plan, the request decided and the ones rejected on their own because the plan filled up. */
public record RequestDecision(Plan plan, JoinRequest request, List<JoinRequest> autoRejected) {
}
