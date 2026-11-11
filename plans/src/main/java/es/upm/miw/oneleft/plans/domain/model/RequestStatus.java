package es.upm.miw.oneleft.plans.domain.model;

/** What became of a request for a spot (HU-069). {@link #EXPIRED} is never stored: a pending request is expired once the plan starts. */
public enum RequestStatus {
    PENDING, ACCEPTED, REJECTED, WITHDRAWN, EXPIRED
}
