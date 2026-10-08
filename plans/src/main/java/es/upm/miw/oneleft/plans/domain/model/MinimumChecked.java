package es.upm.miw.oneleft.plans.domain.model;

import java.util.Optional;

/**
 * Result of checking the minimum of a plan at its deadline (HU-039): the confirmed or cancelled plan and, when
 * cancelled, the event to publish.
 */
public record MinimumChecked(Plan plan, Optional<PlanCancelled> cancellation) {
}
