package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;

/**
 * What the trusted contact of someone sees of their plan (HU-034): where, when and with whom. Of the other people
 * only the name of the organizer, which is what the person wants their contact to know; no identifiers, nobody else
 * of the group and nothing else about them.
 *
 * @param sharedBy  the name, in the plan, of whoever shared the link
 * @param expiresAt when the link stops working: when the plan is over
 */
public record TrustedPlanView(String title, Activity activity, String activityOther, MeetingPoint meetingPoint,
                              Instant startsAt, Instant endsAt, PlanStatus status, String organizerName,
                              String sharedBy, Instant expiresAt) {

    public static TrustedPlanView of(Plan plan, TrustedLink link) {
        return new TrustedPlanView(plan.title(), plan.activity(), plan.activityOther(), plan.meetingPoint(),
                plan.startsAt(), plan.endsAt(), plan.status(), plan.organizer().name(),
                plan.memberName(link.userId()), link.expiresAt());
    }
}
