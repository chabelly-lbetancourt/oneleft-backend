package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.DraftRequest;
import es.upm.miw.oneleft.plans.domain.model.PlanDraft;

/** Turns a sentence into a draft of a plan to review and confirm (HU-010). Nothing is published. */
public interface DraftPlanUseCase {

    PlanDraft draft(DraftRequest request);
}
