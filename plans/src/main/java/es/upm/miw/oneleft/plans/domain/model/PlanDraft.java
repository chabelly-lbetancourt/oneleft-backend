package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * A plan understood from a sentence, checked against the plan model but not published (HU-010): the person reviews it
 * in the form and confirms. What could not be understood, or does not fit the rules of a plan, is left empty and
 * listed in {@code missing}; {@code warnings} explain why when it is not just absent.
 *
 * @param place        the meeting place as the person named it, even if no place was found for it
 * @param meetingPoint the closest place found for it, or {@code null}
 */
public record PlanDraft(Activity activity, String title, String description, String place, MeetingPoint meetingPoint,
                        Instant startsAt, Integer spots, Level level, Integer minParticipants, List<String> missing,
                        List<String> warnings) {
}
