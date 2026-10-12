package es.upm.miw.oneleft.chat.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** The fields of the events of the plans service that the chat uses. */
final class PlanEvents {

    private PlanEvents() {
    }

    /** {@code plan.left}: someone is no longer in the plan. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Left(UUID planId, UUID participantId) {
    }

    /** {@code plan.cancelled}: the plan will not take place (HU-039). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Cancelled(UUID planId) {
    }
}
