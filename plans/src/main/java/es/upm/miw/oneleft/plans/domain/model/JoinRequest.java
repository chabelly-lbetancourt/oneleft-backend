package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A person's request for a spot in a plan whose organizer chooses who comes (HU-069), with an optional short message.
 * Only what the organizer needs to decide: no contact data or location.
 */
public record JoinRequest(UUID userId, String name, String message, RequestStatus status, Instant requestedAt,
                          Instant decidedAt) {

    public static final int MAX_MESSAGE_LENGTH = 140;

    public JoinRequest {
        if (userId == null || name == null || status == null || requestedAt == null) {
            throw new ValidationException("request.missingData", "Required request data is missing");
        }
        message = message == null || message.isBlank() ? null : message.strip();
        if (message != null && (message.length() > MAX_MESSAGE_LENGTH || message.chars().anyMatch(Character::isISOControl))) {
            throw new ValidationException("request.message", "The message can have up to " + MAX_MESSAGE_LENGTH
                    + " characters");
        }
    }

    public static JoinRequest pending(UUID userId, String name, String message, Instant now) {
        return new JoinRequest(userId, name, message, RequestStatus.PENDING, now, null);
    }

    public JoinRequest decided(RequestStatus newStatus, Instant at) {
        return new JoinRequest(userId, name, message, newStatus, requestedAt, at);
    }

    public boolean isPending() {
        return status == RequestStatus.PENDING;
    }
}
