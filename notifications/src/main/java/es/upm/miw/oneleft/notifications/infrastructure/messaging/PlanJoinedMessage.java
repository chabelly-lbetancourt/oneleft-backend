package es.upm.miw.oneleft.notifications.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/** The {@code plan.joined} event as the plans service sends it; only the fields the notices need are read. */
@JsonIgnoreProperties(ignoreUnknown = true)
record PlanJoinedMessage(UUID planId, UUID participantId, boolean full, Instant occurredAt) {
}
