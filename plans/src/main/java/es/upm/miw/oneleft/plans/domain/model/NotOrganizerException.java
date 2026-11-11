package es.upm.miw.oneleft.plans.domain.model;

/** Only the organizer of a plan can do this (HU-069). */
public class NotOrganizerException extends RuntimeException {

    public static final String CODE = "plan.notOrganizer";

    public NotOrganizerException() {
        super("Only the organizer of the plan can do this");
    }
}
