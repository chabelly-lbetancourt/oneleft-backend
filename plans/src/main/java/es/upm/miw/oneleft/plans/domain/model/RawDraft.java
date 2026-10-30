package es.upm.miw.oneleft.plans.domain.model;

/**
 * What the language model understood from the sentence, as it says it and before anything is checked (HU-010). Every
 * field may be missing or wrong: {@code PlanDraftService} validates it against the plan model.
 *
 * @param startsAt local date and time in the person's zone, {@code yyyy-MM-ddTHH:mm}
 * @param place    the meeting place as the person named it
 */
public record RawDraft(String activity, String title, String description, String place, String startsAt,
                       Integer spots, String level, Integer minParticipants) {
}
