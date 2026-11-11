package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A person's request for a spot in a plan whose organizer chooses who comes (HU-069), with an optional short message.
 * Only what the organizer needs to decide: no contact data or location, and the age band of the person (HU-070) at
 * the time of asking, if they set it, never their date of birth.
 */
public record JoinRequest(UUID userId, String name, String message, RequestStatus status, Instant requestedAt,
                          Instant decidedAt, String ageBand) {

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

    public JoinRequest(UUID userId, String name, String message, RequestStatus status, Instant requestedAt,
                       Instant decidedAt) {
        this(userId, name, message, status, requestedAt, decidedAt, null);
    }

    public static JoinRequest pending(UUID userId, String name, String message, Instant now) {
        return pending(userId, name, message, null, now);
    }

    public static JoinRequest pending(UUID userId, String name, String message, String ageBand, Instant now) {
        return new JoinRequest(userId, name, message, RequestStatus.PENDING, now, null, ageBand);
    }

    public JoinRequest decided(RequestStatus newStatus, Instant at) {
        return new JoinRequest(userId, name, message, newStatus, requestedAt, at, ageBand);
    }

    public boolean isPending() {
        return status == RequestStatus.PENDING;
    }
}
